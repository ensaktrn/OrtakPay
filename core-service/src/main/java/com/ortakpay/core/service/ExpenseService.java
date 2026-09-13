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
import com.ortakpay.core.repository.UserRepository;
import com.ortakpay.core.split.SplitStrategy;
import com.ortakpay.core.split.SplitStrategyResolver;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ExpenseService {

    /**
     * id is a random UUID (see docs/adr/0003-uuid-primary-keys.md), not a
     * chronological one - this tiebreaker only guarantees deterministic ordering
     * when two expenses land on the exact same createdAt instant, it is not
     * itself a secondary time signal.
     */
    private static final Sort DEFAULT_EXPENSE_SORT =
            Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by(Sort.Direction.DESC, "id"));

    private final UserRepository userRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final ExpenseRepository expenseRepository;
    private final ExpenseShareRepository expenseShareRepository;
    private final SplitStrategyResolver splitStrategyResolver;
    private final BalanceService balanceService;
    private final GroupAccessGuard groupAccessGuard;

    @Transactional
    public ExpenseResponse createExpense(UUID groupId, UUID currentUserId, CreateExpenseRequest request) {
        Group group = groupAccessGuard.requireMembership(groupId, currentUserId);

        if (request.participants().stream().map(ParticipantInput::userId).distinct().count()
                != request.participants().size()) {
            throw new InvalidSplitException("Participant list contains duplicate user IDs");
        }

        Set<UUID> requiredUserIds = new LinkedHashSet<>();
        requiredUserIds.add(request.paidBy());
        request.participants().forEach(participant -> requiredUserIds.add(participant.userId()));

        // Different rule layer from the guard above: the guard checks the CALLER
        // is a group member, this checks paidBy/participants are too.
        assertAllAreGroupMembers(groupId, requiredUserIds);

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

        return toExpenseResponse(expense, shares);
    }

    @Transactional(readOnly = true)
    public Page<ExpenseResponse> getExpenses(UUID groupId, UUID requesterId, Pageable pageable) {
        groupAccessGuard.requireMembership(groupId, requesterId);

        // Only fall back to the default sort when the caller didn't ask for one -
        // an explicit ?sort= query parameter always wins.
        Pageable effectivePageable = pageable.getSort().isSorted()
                ? pageable
                : PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), DEFAULT_EXPENSE_SORT);

        Page<Expense> expenses = expenseRepository.findByGroup_Id(groupId, effectivePageable);
        List<UUID> expenseIds = expenses.getContent().stream().map(Expense::getId).toList();

        Map<UUID, List<ExpenseShare>> sharesByExpenseId = expenseShareRepository.findByExpense_IdIn(expenseIds).stream()
                .collect(Collectors.groupingBy(share -> share.getExpense().getId()));

        return expenses.map(
                expense -> toExpenseResponse(expense, sharesByExpenseId.getOrDefault(expense.getId(), List.of())));
    }

    private ExpenseResponse toExpenseResponse(Expense expense, List<ExpenseShare> shares) {
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
