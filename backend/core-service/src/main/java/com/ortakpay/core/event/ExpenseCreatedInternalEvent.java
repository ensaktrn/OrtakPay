package com.ortakpay.core.event;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Deliberately holds only primitives/IDs/Strings pulled off the entities at
 * publish time (getEmail(), getDisplayName(), ...) - never an entity reference.
 * Entities are lazy, transactionally-scoped, and specific to core-service's own
 * persistence context; this event may outlive the transaction (see
 * EventPublisherListener's AFTER_COMMIT timing) and must not carry anything that
 * could trigger a LazyInitializationException or leak JPA state.
 */
public record ExpenseCreatedInternalEvent(
        UUID expenseId,
        UUID groupId,
        String groupName,
        UUID paidByUserId,
        String paidByDisplayName,
        BigDecimal amount,
        String description,
        List<ParticipantShare> shares) {}
