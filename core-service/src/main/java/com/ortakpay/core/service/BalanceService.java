package com.ortakpay.core.service;

import com.ortakpay.core.domain.Balance;
import com.ortakpay.core.domain.Expense;
import com.ortakpay.core.domain.ExpenseShare;
import com.ortakpay.core.domain.Group;
import com.ortakpay.core.domain.User;
import com.ortakpay.core.repository.BalanceRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BalanceService {

    private final BalanceRepository balanceRepository;

    @Transactional
    public Balance getOrCreate(Group group, User user) {
        return balanceRepository
                .findByGroupAndUser(group, user)
                .orElseGet(() ->
                        balanceRepository.save(Balance.builder().group(group).user(user).build()));
    }

    /**
     * Applies one expense's effect to every balance it touches, one {@code save()}
     * per row so each hits its own optimistic-lock check on flush - batching these
     * into a single save at the end would still work, but calling save() per delta
     * keeps the intent (and the {@code @Version} check) explicit at each step.
     */
    @Transactional
    public void applyExpense(Expense expense, List<ExpenseShare> shares) {
        Balance payerBalance = getOrCreate(expense.getGroup(), expense.getPaidBy());
        payerBalance.applyDelta(expense.getAmount());
        balanceRepository.save(payerBalance);

        for (ExpenseShare share : shares) {
            Balance owerBalance = getOrCreate(expense.getGroup(), share.getUser());
            owerBalance.applyDelta(share.getOwedAmount().negate());
            balanceRepository.save(owerBalance);
        }
    }
}
