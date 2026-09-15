package com.ortakpay.core.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ortakpay.core.domain.Balance;
import com.ortakpay.core.domain.Group;
import com.ortakpay.core.domain.User;
import com.ortakpay.core.event.BalanceReminderInternalEvent;
import com.ortakpay.core.repository.BalanceRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class BalanceReminderServiceTest {

    private static final int REMINDER_INTERVAL_DAYS = 3;

    // A fixed Clock (not the real Instant.now()) makes every boundary in this
    // test class an exact, deterministic comparison instead of "probably far
    // enough apart in real time" - most valuable for the exact-boundary test
    // below, which needs its fixture's timestamp to be bit-for-bit identical
    // to the threshold the service computes.
    private static final Instant FIXED_NOW = Instant.parse("2026-01-10T09:00:00Z");
    private static final Clock FIXED_CLOCK = Clock.fixed(FIXED_NOW, ZoneOffset.UTC);

    @Mock
    private BalanceRepository balanceRepository;

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    private BalanceReminderService balanceReminderService;

    @BeforeEach
    void setUp() {
        balanceReminderService = new BalanceReminderService(
                balanceRepository, applicationEventPublisher, new BalanceReminderProperties(REMINDER_INTERVAL_DAYS), FIXED_CLOCK);
    }

    @Test
    void findDueReminders_includesBalanceNeverReminded() {
        Balance neverReminded = balanceWithLastReminder(null);
        when(balanceRepository.findByNetAmountLessThan(BigDecimal.ZERO)).thenReturn(List.of(neverReminded));

        assertThat(balanceReminderService.findDueReminders()).containsExactly(neverReminded);
    }

    @Test
    void findDueReminders_excludesBalanceRemindedOneDayLessThanTheInterval() {
        Balance recentlyReminded =
                balanceWithLastReminder(FIXED_NOW.minus(REMINDER_INTERVAL_DAYS - 1, ChronoUnit.DAYS));
        when(balanceRepository.findByNetAmountLessThan(BigDecimal.ZERO)).thenReturn(List.of(recentlyReminded));

        assertThat(balanceReminderService.findDueReminders()).isEmpty();
    }

    @Test
    void findDueReminders_includesBalanceRemindedMoreThanTheIntervalAgo() {
        Balance longAgoReminded =
                balanceWithLastReminder(FIXED_NOW.minus(REMINDER_INTERVAL_DAYS + 1, ChronoUnit.DAYS));
        when(balanceRepository.findByNetAmountLessThan(BigDecimal.ZERO)).thenReturn(List.of(longAgoReminded));

        assertThat(balanceReminderService.findDueReminders()).containsExactly(longAgoReminded);
    }

    @Test
    void findDueReminders_excludesBalanceRemindedJustInsideTheInterval() {
        Balance justInsideInterval =
                balanceWithLastReminder(FIXED_NOW.minus(REMINDER_INTERVAL_DAYS, ChronoUnit.DAYS).plusSeconds(30));
        when(balanceRepository.findByNetAmountLessThan(BigDecimal.ZERO)).thenReturn(List.of(justInsideInterval));

        assertThat(balanceReminderService.findDueReminders()).isEmpty();
    }

    @Test
    void findDueReminders_excludesBalanceRemindedExactlyAtTheIntervalBoundary() {
        // lastReminderSentAt set to bit-for-bit the same instant
        // findDueReminders() computes as its own threshold (both derive from
        // FIXED_CLOCK), so this is a true equality check, not an
        // approximation. isBefore() is strict: an exact match must NOT be
        // selected - "exactly N days ago" is not yet "more than N days ago".
        // If this behavior ever regresses (e.g. isBefore swapped for
        // !isAfter), this test goes red.
        Instant exactlyAtThreshold = FIXED_NOW.minus(REMINDER_INTERVAL_DAYS, ChronoUnit.DAYS);
        Balance remindedExactlyAtBoundary = balanceWithLastReminder(exactlyAtThreshold);
        when(balanceRepository.findByNetAmountLessThan(BigDecimal.ZERO)).thenReturn(List.of(remindedExactlyAtBoundary));

        assertThat(balanceReminderService.findDueReminders()).isEmpty();
    }

    @Test
    void findDueReminders_excludesPositiveBalance_evenIfRepositoryIncorrectlyReturnedOne() {
        // The repository query (findByNetAmountLessThan) is supposed to already
        // guarantee this, but the service re-checks netAmount itself rather than
        // blindly trusting the query name - see BalanceReminderService's comment.
        Balance positiveBalance = Balance.builder()
                .group(Group.builder().id(UUID.randomUUID()).name("G").build())
                .user(User.builder().id(UUID.randomUUID()).email("u@example.com").displayName("U").build())
                .netAmount(new BigDecimal("10.00"))
                .build();
        when(balanceRepository.findByNetAmountLessThan(BigDecimal.ZERO)).thenReturn(List.of(positiveBalance));

        assertThat(balanceReminderService.findDueReminders()).isEmpty();
    }

    @Test
    void sendDueReminders_marksReminderSentAndPublishesEvent_forEachDueBalance() {
        Group group = Group.builder().id(UUID.randomUUID()).name("Trip").build();
        User user = User.builder()
                .id(UUID.randomUUID())
                .email("owes@example.com")
                .displayName("Ower")
                .build();
        Balance due = Balance.builder()
                .group(group)
                .user(user)
                .netAmount(new BigDecimal("-15.00"))
                .build();
        when(balanceRepository.findByNetAmountLessThan(BigDecimal.ZERO)).thenReturn(List.of(due));
        when(balanceRepository.save(any(Balance.class))).thenAnswer(invocation -> invocation.getArgument(0));

        balanceReminderService.sendDueReminders();

        assertThat(due.getLastReminderSentAt()).isEqualTo(FIXED_NOW);
        verify(balanceRepository).save(due);

        ArgumentCaptor<BalanceReminderInternalEvent> eventCaptor =
                ArgumentCaptor.forClass(BalanceReminderInternalEvent.class);
        verify(applicationEventPublisher).publishEvent(eventCaptor.capture());
        BalanceReminderInternalEvent event = eventCaptor.getValue();
        assertThat(event.groupId()).isEqualTo(group.getId());
        assertThat(event.userId()).isEqualTo(user.getId());
        assertThat(event.email()).isEqualTo("owes@example.com");
        assertThat(event.owedAmount()).isEqualByComparingTo("-15.00");
    }

    @Test
    void sendDueReminders_publishesNothing_whenNoBalancesAreDue() {
        when(balanceRepository.findByNetAmountLessThan(BigDecimal.ZERO)).thenReturn(List.of());

        balanceReminderService.sendDueReminders();

        verify(applicationEventPublisher, never()).publishEvent(any());
        verify(balanceRepository, never()).save(any());
    }

    private Balance balanceWithLastReminder(Instant lastReminderSentAt) {
        return Balance.builder()
                .group(Group.builder().id(UUID.randomUUID()).name("G").build())
                .user(User.builder().id(UUID.randomUUID()).email("u@example.com").displayName("U").build())
                .netAmount(new BigDecimal("-10.00"))
                .lastReminderSentAt(lastReminderSentAt)
                .build();
    }
}
