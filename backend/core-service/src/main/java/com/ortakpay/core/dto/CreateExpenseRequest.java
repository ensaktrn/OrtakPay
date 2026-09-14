package com.ortakpay.core.dto;

import com.ortakpay.core.domain.SplitType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record CreateExpenseRequest(
        @NotNull UUID paidBy,
        @NotNull @Positive BigDecimal amount,
        @NotBlank String description,
        @NotNull SplitType splitType,
        @NotEmpty List<@Valid ParticipantInput> participants) {}
