package com.ortakpay.core.split;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.ortakpay.core.domain.Expense;
import com.ortakpay.core.domain.ExpenseShare;
import com.ortakpay.core.domain.SplitType;
import com.ortakpay.core.domain.User;
import com.ortakpay.core.dto.ParticipantInput;
import com.ortakpay.core.exception.InvalidSplitException;
import com.ortakpay.core.repository.UserRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PercentageSplitStrategyTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private PercentageSplitStrategy strategy;

    @Test
    void supportsReturnsPercentage() {
        assertThat(strategy.supports()).isEqualTo(SplitType.PERCENTAGE);
    }

    @Test
    void distributesProportionally_withRemainderCentsToFirstParticipants() {
        when(userRepository.getReferenceById(any(UUID.class)))
                .thenAnswer(invocation -> User.builder().id(invocation.getArgument(0)).build());

        Expense expense = Expense.builder().amount(new BigDecimal("10.00")).build();
        List<ParticipantInput> participants = List.of(
                new ParticipantInput(UUID.randomUUID(), new BigDecimal("33.34")),
                new ParticipantInput(UUID.randomUUID(), new BigDecimal("33.33")),
                new ParticipantInput(UUID.randomUUID(), new BigDecimal("33.33")));

        List<ExpenseShare> shares = strategy.calculateShares(expense, participants);

        assertThat(shares.get(0).getOwedAmount()).isEqualByComparingTo("3.34");
        assertThat(shares.get(1).getOwedAmount()).isEqualByComparingTo("3.33");
        assertThat(shares.get(2).getOwedAmount()).isEqualByComparingTo("3.33");

        BigDecimal sum =
                shares.stream().map(ExpenseShare::getOwedAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(sum).isEqualByComparingTo(expense.getAmount());
    }

    @Test
    void throwsInvalidSplitException_whenPercentagesDoNotSumTo100() {
        Expense expense = Expense.builder().amount(new BigDecimal("10.00")).build();
        List<ParticipantInput> participants = List.of(
                new ParticipantInput(UUID.randomUUID(), new BigDecimal("60")),
                new ParticipantInput(UUID.randomUUID(), new BigDecimal("30")));

        assertThatThrownBy(() -> strategy.calculateShares(expense, participants))
                .isInstanceOf(InvalidSplitException.class)
                .hasMessageContaining("90");
    }
}
