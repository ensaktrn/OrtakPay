package com.ortakpay.core.event;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * The AMQP wire payload, kept as a type distinct from {@link ExpenseCreatedInternalEvent}
 * even though the fields currently match 1:1 - this is the external contract other
 * services depend on, so it should be free to diverge from the internal event's
 * shape later without that being a breaking change for consumers, or vice versa.
 */
public record ExpenseCreatedMessage(
        UUID expenseId,
        UUID groupId,
        String groupName,
        UUID paidByUserId,
        String paidByDisplayName,
        BigDecimal amount,
        String description,
        List<ParticipantShare> shares) {}
