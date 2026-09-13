package com.ortakpay.core.controller;

import com.ortakpay.core.domain.User;
import com.ortakpay.core.dto.BalanceResponse;
import com.ortakpay.core.service.BalanceService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/groups/{groupId}/balances")
@RequiredArgsConstructor
public class BalanceController {

    private final BalanceService balanceService;

    @GetMapping
    public List<BalanceResponse> getBalances(@PathVariable UUID groupId, @AuthenticationPrincipal User currentUser) {
        return balanceService.getGroupBalances(groupId, currentUser.getId());
    }
}
