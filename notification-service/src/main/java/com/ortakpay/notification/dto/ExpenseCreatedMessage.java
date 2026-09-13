package com.ortakpay.notification.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Mirrors core-service's event of the same name field-for-field, but is defined
 * independently here - no shared JAR/module between services (see
 * docs/adr/0001-monorepo-with-independent-services.md).
 */
public record ExpenseCreatedMessage(
        UUID expenseId,
        UUID groupId,
        String groupName,
        UUID paidByUserId,
        String paidByDisplayName,
        BigDecimal amount,
        String description,
        List<ParticipantShare> shares) {

    public record ParticipantShare(UUID userId, String email, String displayName, BigDecimal owedAmount) {}
}
