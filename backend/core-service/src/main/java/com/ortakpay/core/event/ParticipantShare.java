package com.ortakpay.core.event;

import java.math.BigDecimal;
import java.util.UUID;

public record ParticipantShare(UUID userId, String email, String displayName, BigDecimal owedAmount) {}
