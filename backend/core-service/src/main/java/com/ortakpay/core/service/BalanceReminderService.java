package com.ortakpay.core.service;

import com.ortakpay.core.domain.Balance;
import com.ortakpay.core.event.BalanceReminderInternalEvent;
import com.ortakpay.core.repository.BalanceRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BalanceReminderService {

    private final BalanceRepository balanceRepository;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final BalanceReminderProperties properties;

    /**
     * The reminder-timing filter (never reminded, or reminded long enough ago)
     * is applied here in Java rather than folded into the repository query:
     * this table will never be large enough in this project for that to be a
     * real performance concern, and keeping it in Java makes the boundary
     * logic (exactly N days vs N-1 days) directly unit-testable with Mockito
     * instead of only verifiable against a real database. The netAmount < 0
     * check is deliberately re-asserted here too, even though
     * findByNetAmountLessThan already guarantees it at the query level - a
     * reminder job is exactly the kind of place where trusting a query name
     * alone, instead of also checking the invariant it implies, isn't worth
     * the risk.
     */
    @Transactional(readOnly = true)
    public List<Balance> findDueReminders() {
        Instant threshold = Instant.now().minus(properties.reminderIntervalDays(), ChronoUnit.DAYS);
        return balanceRepository.findByNetAmountLessThan(BigDecimal.ZERO).stream()
                .filter(balance -> balance.getNetAmount().compareTo(BigDecimal.ZERO) < 0)
                .filter(balance ->
                        balance.getLastReminderSentAt() == null || balance.getLastReminderSentAt().isBefore(threshold))
                .toList();
    }

    @Transactional
    public void sendDueReminders() {
        for (Balance balance : findDueReminders()) {
            balance.markReminderSent(Instant.now());
            balanceRepository.save(balance);

            applicationEventPublisher.publishEvent(new BalanceReminderInternalEvent(
                    balance.getGroup().getId(),
                    balance.getGroup().getName(),
                    balance.getUser().getId(),
                    balance.getUser().getEmail(),
                    balance.getUser().getDisplayName(),
                    balance.getNetAmount()));
        }
    }
}
