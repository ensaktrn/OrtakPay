package com.ortakpay.core.split;

import com.ortakpay.core.domain.Expense;
import com.ortakpay.core.domain.ExpenseShare;
import com.ortakpay.core.domain.SplitType;
import com.ortakpay.core.dto.ParticipantInput;
import com.ortakpay.core.exception.InvalidSplitException;
import com.ortakpay.core.repository.UserRepository;
import java.math.BigDecimal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ExactSplitStrategy implements SplitStrategy {

    private final UserRepository userRepository;

    @Override
    public SplitType supports() {
        return SplitType.EXACT;
    }

    @Override
    public List<ExpenseShare> calculateShares(Expense expense, List<ParticipantInput> participants) {
        BigDecimal sum = participants.stream().map(ParticipantInput::value).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal difference = expense.getAmount().subtract(sum);
        if (difference.compareTo(BigDecimal.ZERO) != 0) {
            throw new InvalidSplitException("Exact shares (%s) must sum to the expense amount (%s); difference is %s"
                    .formatted(sum, expense.getAmount(), difference.abs()));
        }

        return participants.stream()
                .map(participant -> ExpenseShare.builder()
                        .expense(expense)
                        .user(userRepository.getReferenceById(participant.userId()))
                        .owedAmount(participant.value())
                        .build())
                .toList();
    }
}
