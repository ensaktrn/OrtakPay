package com.ortakpay.core.split;

import com.ortakpay.core.domain.Expense;
import com.ortakpay.core.domain.ExpenseShare;
import com.ortakpay.core.domain.SplitType;
import com.ortakpay.core.dto.ParticipantInput;
import com.ortakpay.core.repository.UserRepository;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EqualSplitStrategy implements SplitStrategy {

    private final UserRepository userRepository;

    @Override
    public SplitType supports() {
        return SplitType.EQUAL;
    }

    @Override
    public List<ExpenseShare> calculateShares(Expense expense, List<ParticipantInput> participants) {
        int n = participants.size();
        List<Long> centsPerParticipant = Money.distributeEqually(Money.toCents(expense.getAmount()), n);

        List<ExpenseShare> shares = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            shares.add(ExpenseShare.builder()
                    .expense(expense)
                    .user(userRepository.getReferenceById(participants.get(i).userId()))
                    .owedAmount(Money.fromCents(centsPerParticipant.get(i)))
                    .build());
        }
        return shares;
    }
}
