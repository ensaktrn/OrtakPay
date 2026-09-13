package com.ortakpay.core.service;

import com.ortakpay.core.domain.Balance;
import com.ortakpay.core.domain.Expense;
import com.ortakpay.core.domain.ExpenseShare;
import com.ortakpay.core.domain.Group;
import com.ortakpay.core.domain.User;
import com.ortakpay.core.dto.BalanceResponse;
import com.ortakpay.core.repository.BalanceRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BalanceService {

    private final BalanceRepository balanceRepository;
    private final GroupAccessGuard groupAccessGuard;

    @Transactional
    public Balance getOrCreate(Group group, User user) {
        return balanceRepository
                .findByGroupAndUser(group, user)
                .orElseGet(() ->
                        balanceRepository.save(Balance.builder().group(group).user(user).build()));
    }

    /**
     * Loads (or creates) the balance for group+user, applies the delta, and saves
     * it - one call per row so each hits its own optimistic-lock check on flush.
     * Shared by {@link #applyExpense} and {@code SettlementService}, which both
     * need exactly this "adjust one balance row transactionally" primitive.
     */
    @Transactional
    public Balance applyDeltaAndSave(Group group, User user, BigDecimal delta) {
        Balance balance = getOrCreate(group, user);
        balance.applyDelta(delta);
        return balanceRepository.save(balance);
    }

    @Transactional
    public void applyExpense(Expense expense, List<ExpenseShare> shares) {
        applyDeltaAndSave(expense.getGroup(), expense.getPaidBy(), expense.getAmount());
        for (ExpenseShare share : shares) {
            applyDeltaAndSave(expense.getGroup(), share.getUser(), share.getOwedAmount().negate());
        }
    }

    @Transactional(readOnly = true)
    public List<BalanceResponse> getGroupBalances(UUID groupId, UUID requesterId) {
        groupAccessGuard.requireMembership(groupId, requesterId);

        return balanceRepository.findByGroup_Id(groupId).stream()
                .map(balance -> new BalanceResponse(
                        balance.getUser().getId(),
                        balance.getUser().getEmail(),
                        balance.getUser().getDisplayName(),
                        balance.getNetAmount()))
                .toList();
    }
}
