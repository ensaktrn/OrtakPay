package com.ortakpay.core.split;

import com.ortakpay.core.domain.Expense;
import com.ortakpay.core.domain.ExpenseShare;
import com.ortakpay.core.domain.SplitType;
import com.ortakpay.core.dto.ParticipantInput;
import com.ortakpay.core.exception.InvalidSplitException;
import com.ortakpay.core.repository.UserRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PercentageSplitStrategy implements SplitStrategy {

    private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);

    private final UserRepository userRepository;

    @Override
    public SplitType supports() {
        return SplitType.PERCENTAGE;
    }

    @Override
    public List<ExpenseShare> calculateShares(Expense expense, List<ParticipantInput> participants) {
        List<BigDecimal> percentages =
                participants.stream().map(ParticipantInput::value).toList();
        BigDecimal percentageSum = percentages.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        if (percentageSum.compareTo(ONE_HUNDRED) != 0) {
            throw new InvalidSplitException("Percentages must sum to 100; got " + percentageSum);
        }

        int n = participants.size();
        List<Long> centsPerParticipant =
                Money.distributeProportionally(Money.toCents(expense.getAmount()), percentages);

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
