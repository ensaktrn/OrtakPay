package com.ortakpay.core.dto;

import java.util.UUID;

/**
 * Slimmer than {@link GroupResponse} - a "my groups" list screen only needs
 * enough to render a row (name + how many people are in it), not the full
 * member list of every group at once.
 */
public record GroupSummaryResponse(UUID id, String name, int memberCount) {}
