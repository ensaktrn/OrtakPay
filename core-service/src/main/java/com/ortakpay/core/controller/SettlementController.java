package com.ortakpay.core.controller;

import com.ortakpay.core.domain.User;
import com.ortakpay.core.dto.SettlementRequest;
import com.ortakpay.core.dto.SettlementResponse;
import com.ortakpay.core.service.SettlementService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/groups/{groupId}/settlements")
@RequiredArgsConstructor
public class SettlementController {

    private final SettlementService settlementService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SettlementResponse recordSettlement(
            @PathVariable UUID groupId,
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody SettlementRequest request) {
        return settlementService.recordSettlement(groupId, currentUser.getId(), request.toUserId(), request.amount());
    }
}
