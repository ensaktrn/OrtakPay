package com.ortakpay.core.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ortakpay.core.domain.Balance;
import com.ortakpay.core.domain.Expense;
import com.ortakpay.core.domain.ExpenseShare;
import com.ortakpay.core.domain.Group;
import com.ortakpay.core.domain.User;
import com.ortakpay.core.exception.GroupAccessDeniedException;
import com.ortakpay.core.repository.BalanceRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BalanceServiceTest {

    @Mock
    private BalanceRepository balanceRepository;

    @Mock
    private GroupAccessGuard groupAccessGuard;

    @InjectMocks
    private BalanceService balanceService;

    @Test
    void getOrCreate_returnsExisting_whenPresent() {
        Group group = Group.builder().id(UUID.randomUUID()).build();
        User user = User.builder().id(UUID.randomUUID()).build();
        Balance existing = Balance.builder().group(group).user(user).build();
        when(balanceRepository.findByGroupAndUser(group, user)).thenReturn(Optional.of(existing));

        Balance result = balanceService.getOrCreate(group, user);

        assertThat(result).isSameAs(existing);
        verify(balanceRepository, never()).save(any());
    }

    @Test
    void getOrCreate_createsNewWithZeroBalance_whenAbsent() {
        Group group = Group.builder().id(UUID.randomUUID()).build();
        User user = User.builder().id(UUID.randomUUID()).build();
        when(balanceRepository.findByGroupAndUser(group, user)).thenReturn(Optional.empty());
        when(balanceRepository.save(any(Balance.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Balance result = balanceService.getOrCreate(group, user);

        assertThat(result.getNetAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        verify(balanceRepository).save(any(Balance.class));
    }

    @Test
    void applyExpense_appliesPositiveDeltaToPayer_andNegativeDeltaToEachOwer() {
        Group group = Group.builder().id(UUID.randomUUID()).build();
        User payer = User.builder().id(UUID.randomUUID()).build();
        User ower1 = User.builder().id(UUID.randomUUID()).build();
        User ower2 = User.builder().id(UUID.randomUUID()).build();

        Expense expense = Expense.builder()
                .group(group)
                .paidBy(payer)
                .amount(new BigDecimal("10.00"))
                .build();
        ExpenseShare share1 =
                ExpenseShare.builder().user(ower1).owedAmount(new BigDecimal("5.00")).build();
        ExpenseShare share2 =
                ExpenseShare.builder().user(ower2).owedAmount(new BigDecimal("5.00")).build();

        Balance payerBalance = Balance.builder().group(group).user(payer).build();
        Balance ower1Balance = Balance.builder().group(group).user(ower1).build();
        Balance ower2Balance = Balance.builder().group(group).user(ower2).build();

        when(balanceRepository.findByGroupAndUser(group, payer)).thenReturn(Optional.of(payerBalance));
        when(balanceRepository.findByGroupAndUser(group, ower1)).thenReturn(Optional.of(ower1Balance));
        when(balanceRepository.findByGroupAndUser(group, ower2)).thenReturn(Optional.of(ower2Balance));
        when(balanceRepository.save(any(Balance.class))).thenAnswer(invocation -> invocation.getArgument(0));

        balanceService.applyExpense(expense, List.of(share1, share2));

        assertThat(payerBalance.getNetAmount()).isEqualByComparingTo("10.00");
        assertThat(ower1Balance.getNetAmount()).isEqualByComparingTo("-5.00");
        assertThat(ower2Balance.getNetAmount()).isEqualByComparingTo("-5.00");
        verify(balanceRepository, times(3)).save(any(Balance.class));
    }

    @Test
    void getGroupBalances_throwsGroupAccessDenied_whenRequesterNotAMember() {
        UUID groupId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();
        when(groupAccessGuard.requireMembership(groupId, requesterId))
                .thenThrow(new GroupAccessDeniedException("User is not a member of this group"));

        assertThatThrownBy(() -> balanceService.getGroupBalances(groupId, requesterId))
                .isInstanceOf(GroupAccessDeniedException.class);

        verify(balanceRepository, never()).findByGroup_Id(any());
    }
}
