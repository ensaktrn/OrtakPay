package com.ortakpay.core.service;

import com.ortakpay.core.domain.Expense;
import com.ortakpay.core.domain.ExpenseShare;
import com.ortakpay.core.domain.Group;
import com.ortakpay.core.domain.User;
import com.ortakpay.core.dto.CreateExpenseRequest;
import com.ortakpay.core.dto.ExpenseResponse;
import com.ortakpay.core.dto.ParticipantInput;
import com.ortakpay.core.exception.GroupMembershipException;
import com.ortakpay.core.exception.InvalidSplitException;
import com.ortakpay.core.repository.ExpenseRepository;
import com.ortakpay.core.repository.ExpenseShareRepository;
import com.ortakpay.core.repository.GroupMemberRepository;
import com.ortakpay.core.repository.GroupRepository;
import com.ortakpay.core.repository.UserRepository;
import com.ortakpay.core.split.SplitStrategy;
import com.ortakpay.core.split.SplitStrategyResolver;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ExpenseService {

    private final GroupRepository groupRepository;
    private final UserRepository userRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final ExpenseRepository expenseRepository;
    private final ExpenseShareRepository expenseShareRepository;
    private final SplitStrategyResolver splitStrategyResolver;
    private final BalanceService balanceService;

    @Transactional
    public ExpenseResponse createExpense(UUID groupId, CreateExpenseRequest request) {
        if (request.participants().stream().map(ParticipantInput::userId).distinct().count()
                != request.participants().size()) {
            throw new InvalidSplitException("Participant list contains duplicate user IDs");
        }

        Set<UUID> requiredUserIds = new LinkedHashSet<>();
        requiredUserIds.add(request.paidBy());
        request.participants().forEach(participant -> requiredUserIds.add(participant.userId()));

        // A nonexistent groupId is not special-cased: no GroupMember rows can exist
        // for it either, so every required user id simply comes back "missing" below.
        assertAllAreGroupMembers(groupId, requiredUserIds);

        Group group = groupRepository.getReferenceById(groupId);
        User payer = userRepository.getReferenceById(request.paidBy());

        Expense expense = expenseRepository.save(Expense.builder()
                .group(group)
                .paidBy(payer)
                .amount(request.amount())
                .description(request.description())
                .splitType(request.splitType())
                .build());

        SplitStrategy strategy = splitStrategyResolver.resolve(request.splitType());
        List<ExpenseShare> shares =
                expenseShareRepository.saveAll(strategy.calculateShares(expense, request.participants()));

        balanceService.applyExpense(expense, shares);

        List<ExpenseResponse.ShareResponse> shareResponses = shares.stream()
                .map(share -> new ExpenseResponse.ShareResponse(share.getUser().getId(), share.getOwedAmount()))
                .toList();

        return new ExpenseResponse(
                expense.getId(),
                expense.getAmount(),
                expense.getDescription(),
                expense.getSplitType(),
                shareResponses);
    }

    private void assertAllAreGroupMembers(UUID groupId, Set<UUID> requiredUserIds) {
        Set<UUID> memberUserIds = groupMemberRepository.findByGroup_IdAndUser_IdIn(groupId, requiredUserIds).stream()
                .map(member -> member.getUser().getId())
                .collect(Collectors.toSet());

        if (!memberUserIds.containsAll(requiredUserIds)) {
            Set<UUID> missing = new LinkedHashSet<>(requiredUserIds);
            missing.removeAll(memberUserIds);
            throw new GroupMembershipException(
                    "The following users are not members of group " + groupId + ": " + missing);
        }
    }
}
