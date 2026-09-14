package com.ortakpay.notification.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record SettlementRecordedMessage(
        UUID groupId,
        String groupName,
        UUID fromUserId,
        String fromDisplayName,
        UUID toUserId,
        String toEmail,
        String toDisplayName,
        BigDecimal amount) {}
