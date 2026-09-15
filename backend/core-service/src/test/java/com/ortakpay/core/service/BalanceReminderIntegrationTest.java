package com.ortakpay.core.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.ortakpay.core.AbstractIntegrationTest;
import com.ortakpay.core.domain.Balance;
import com.ortakpay.core.domain.Group;
import com.ortakpay.core.domain.User;
import com.ortakpay.core.event.BalanceReminderInternalEvent;
import com.ortakpay.core.repository.BalanceRepository;
import com.ortakpay.core.repository.GroupRepository;
import com.ortakpay.core.repository.UserRepository;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Same pattern as ExpenseEventPublishingIntegrationTest: TransactionTemplate
 * with PROPAGATION_REQUIRES_NEW forces a genuine commit so the AFTER_COMMIT
 * listener actually fires, and ApplicationEvents records what got published
 * through the real ApplicationEventPublisher.
 */
@RecordApplicationEvents
class BalanceReminderIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private BalanceReminderService balanceReminderService;

    @Autowired
    private BalanceRepository balanceRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GroupRepository groupRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private ApplicationEvents applicationEvents;

    private record Fixture(User user, Group group, UUID balanceId) {}

    @Test
    void sendDueReminders_stampsTimestampAndPublishesEvent_thenSkipsTheSameBalanceOnTheNextRun() {
        TransactionTemplate requiresNew = new TransactionTemplate(transactionManager);
        requiresNew.setPropagationBehavior(TransactionTemplate.PROPAGATION_REQUIRES_NEW);

        Fixture fixture = requiresNew.execute(status -> {
            User user = userRepository.save(User.builder()
                    .email("reminder-" + UUID.randomUUID() + "@example.com")
                    .passwordHash("hash")
                    .displayName("Reminder Target")
                    .build());
            Group group = groupRepository.save(
                    Group.builder().name("Reminder Group").createdBy(user).build());
            Balance balance = balanceRepository.save(Balance.builder()
                    .group(group)
                    .user(user)
                    .netAmount(new BigDecimal("-15.00"))
                    .build());
            return new Fixture(user, group, balance.getId());
        });

        requiresNew.executeWithoutResult(status -> balanceReminderService.sendDueReminders());

        assertThat(applicationEvents.stream(BalanceReminderInternalEvent.class)).anySatisfy(event -> {
            assertThat(event.groupId()).isEqualTo(fixture.group().getId());
            assertThat(event.userId()).isEqualTo(fixture.user().getId());
            assertThat(event.owedAmount()).isEqualByComparingTo("-15.00");
        });

        Balance reloaded = balanceRepository.findById(fixture.balanceId()).orElseThrow();
        assertThat(reloaded.getLastReminderSentAt()).isNotNull();

        // Reminder-interval-days defaults to 3 in this test's config, so
        // calling again immediately must not re-select the balance we just
        // reminded - only one event should exist for OUR user, not two.
        // (The shared Testcontainers Postgres carries leftover negative
        // balances from other test classes' fixtures across the whole suite
        // run, so sendDueReminders() legitimately also fires for those - the
        // assertion below filters down to our own fixture's user instead of
        // counting the whole stream.)
        requiresNew.executeWithoutResult(status -> balanceReminderService.sendDueReminders());

        assertThat(applicationEvents
                        .stream(BalanceReminderInternalEvent.class)
                        .filter(event -> event.userId().equals(fixture.user().getId())))
                .hasSize(1);
    }
}
