package com.ortakpay.core.controller;

import com.ortakpay.core.domain.User;
import com.ortakpay.core.dto.CreateExpenseRequest;
import com.ortakpay.core.dto.ExpenseResponse;
import com.ortakpay.core.service.ExpenseService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/groups/{groupId}/expenses")
@RequiredArgsConstructor
public class ExpenseController {

    private final ExpenseService expenseService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ExpenseResponse createExpense(
            @PathVariable UUID groupId,
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody CreateExpenseRequest request) {
        return expenseService.createExpense(groupId, currentUser.getId(), request);
    }

    @GetMapping
    public Page<ExpenseResponse> getExpenses(
            @PathVariable UUID groupId, @AuthenticationPrincipal User currentUser, Pageable pageable) {
        return expenseService.getExpenses(groupId, currentUser.getId(), pageable);
    }
}
