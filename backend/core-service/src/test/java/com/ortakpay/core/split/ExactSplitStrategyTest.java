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
class ExactSplitStrategyTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ExactSplitStrategy strategy;

    @Test
    void supportsReturnsExact() {
        assertThat(strategy.supports()).isEqualTo(SplitType.EXACT);
    }

    @Test
    void usesGivenValuesDirectly_whenSumMatchesAmount() {
        when(userRepository.getReferenceById(any(UUID.class)))
                .thenAnswer(invocation -> User.builder().id(invocation.getArgument(0)).build());

        Expense expense = Expense.builder().amount(new BigDecimal("10.00")).build();
        List<ParticipantInput> participants = List.of(
                new ParticipantInput(UUID.randomUUID(), new BigDecimal("6.00")),
                new ParticipantInput(UUID.randomUUID(), new BigDecimal("4.00")));

        List<ExpenseShare> shares = strategy.calculateShares(expense, participants);

        assertThat(shares).hasSize(2);
        assertThat(shares.get(0).getOwedAmount()).isEqualByComparingTo("6.00");
        assertThat(shares.get(1).getOwedAmount()).isEqualByComparingTo("4.00");
    }

    @Test
    void throwsInvalidSplitException_whenSumDoesNotMatchAmount() {
        Expense expense = Expense.builder().amount(new BigDecimal("10.00")).build();
        List<ParticipantInput> participants = List.of(
                new ParticipantInput(UUID.randomUUID(), new BigDecimal("6.00")),
                new ParticipantInput(UUID.randomUUID(), new BigDecimal("3.00")));

        assertThatThrownBy(() -> strategy.calculateShares(expense, participants))
                .isInstanceOf(InvalidSplitException.class)
                .hasMessageContaining("1.00");
    }
}
