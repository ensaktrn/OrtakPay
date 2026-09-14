package com.ortakpay.core.service;

import com.ortakpay.core.domain.Group;
import com.ortakpay.core.domain.Settlement;
import com.ortakpay.core.domain.User;
import com.ortakpay.core.dto.SettlementResponse;
import com.ortakpay.core.event.SettlementRecordedInternalEvent;
import com.ortakpay.core.exception.GroupMembershipException;
import com.ortakpay.core.exception.InvalidSettlementException;
import com.ortakpay.core.repository.GroupMemberRepository;
import com.ortakpay.core.repository.SettlementRepository;
import com.ortakpay.core.repository.UserRepository;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SettlementService {

    private final GroupAccessGuard groupAccessGuard;
    private final GroupMemberRepository groupMemberRepository;
    private final UserRepository userRepository;
    private final BalanceService balanceService;
    private final SettlementRepository settlementRepository;
    private final ApplicationEventPublisher applicationEventPublisher;

    /**
     * fromUser is always currentUserId - there is no fromUserId in the request
     * contract (see SettlementRequest) and none is accepted here either, so a
     * caller can only ever record a settlement paid by themselves.
     */
    @Transactional
    public SettlementResponse recordSettlement(UUID groupId, UUID currentUserId, UUID toUserId, BigDecimal amount) {
        Group group = groupAccessGuard.requireMembership(groupId, currentUserId);

        if (currentUserId.equals(toUserId)) {
            throw new InvalidSettlementException("Cannot record a settlement to yourself");
        }
        if (amount == null || amount.signum() <= 0) {
            throw new InvalidSettlementException("Settlement amount must be positive");
        }
        if (!groupMemberRepository.existsByGroup_IdAndUser_Id(groupId, toUserId)) {
            throw new GroupMembershipException("User " + toUserId + " is not a member of group " + groupId);
        }

        User fromUser = userRepository.getReferenceById(currentUserId);
        User toUser = userRepository.getReferenceById(toUserId);

        balanceService.applyDeltaAndSave(group, fromUser, amount);
        balanceService.applyDeltaAndSave(group, toUser, amount.negate());

        Settlement settlement = settlementRepository.save(Settlement.builder()
                .group(group)
                .fromUser(fromUser)
                .toUser(toUser)
                .amount(amount)
                .build());

        applicationEventPublisher.publishEvent(new SettlementRecordedInternalEvent(
                groupId,
                group.getName(),
                currentUserId,
                fromUser.getDisplayName(),
                toUserId,
                toUser.getEmail(),
                toUser.getDisplayName(),
                amount));

        return new SettlementResponse(settlement.getId(), currentUserId, toUserId, settlement.getAmount());
    }
}
