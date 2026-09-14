package com.ortakpay.core.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateGroupRequest(@NotBlank String name) {}
