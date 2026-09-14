package com.ortakpay.core.dto;

import com.ortakpay.core.domain.SplitType;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record ExpenseResponse(
        UUID id, BigDecimal amount, String description, SplitType splitType, List<ShareResponse> shares) {

    public record ShareResponse(UUID userId, BigDecimal owedAmount) {}
}
