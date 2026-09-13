package com.ortakpay.notification.service;

import com.ortakpay.notification.config.RabbitConfig;
import com.ortakpay.notification.domain.NotificationEventType;
import com.ortakpay.notification.domain.NotificationLog;
import com.ortakpay.notification.domain.NotificationStatus;
import com.ortakpay.notification.dto.ExpenseCreatedMessage;
import com.ortakpay.notification.dto.GroupMemberAddedMessage;
import com.ortakpay.notification.dto.SettlementRecordedMessage;
import com.ortakpay.notification.repository.NotificationLogRepository;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/**
 * Mock notification sending: no real email/push provider in this phase, just a
 * log line standing in for "the notification was sent" plus a persisted audit
 * row. A failure here (e.g. a DB error) propagates back to the listener
 * container, which rejects the message per RabbitConfig's
 * defaultRequeueRejected(false) - it lands in the queue's DLQ instead of
 * retrying forever.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationListener {

    private final NotificationLogRepository notificationLogRepository;
    private final JsonMapper jsonMapper;

    @RabbitListener(queues = RabbitConfig.EXPENSE_CREATED_QUEUE)
    public void onExpenseCreated(ExpenseCreatedMessage message) {
        for (ExpenseCreatedMessage.ParticipantShare share : message.shares()) {
            log.info(
                    "Sending notification to {}: You were added to expense '{}' (you owe {})",
                    share.email(),
                    message.description(),
                    share.owedAmount());
            notificationLogRepository.save(NotificationLog.builder()
                    .eventType(NotificationEventType.EXPENSE_CREATED)
                    .recipientUserId(share.userId())
                    .recipientEmail(share.email())
                    .payload(jsonMapper.writeValueAsString(message))
                    .status(NotificationStatus.SENT)
                    .createdAt(Instant.now())
                    .build());
        }
    }

    @RabbitListener(queues = RabbitConfig.GROUP_MEMBER_ADDED_QUEUE)
    public void onGroupMemberAdded(GroupMemberAddedMessage message) {
        log.info(
                "Sending notification to {}: You were added to group '{}' by {}",
                message.newMemberEmail(),
                message.groupName(),
                message.addedByDisplayName());
        notificationLogRepository.save(NotificationLog.builder()
                .eventType(NotificationEventType.GROUP_MEMBER_ADDED)
                .recipientUserId(message.newMemberId())
                .recipientEmail(message.newMemberEmail())
                .payload(jsonMapper.writeValueAsString(message))
                .status(NotificationStatus.SENT)
                .createdAt(Instant.now())
                .build());
    }

    @RabbitListener(queues = RabbitConfig.SETTLEMENT_RECORDED_QUEUE)
    public void onSettlementRecorded(SettlementRecordedMessage message) {
        log.info(
                "Sending notification to {}: {} recorded a payment of {} to you in group '{}'",
                message.toEmail(),
                message.fromDisplayName(),
                message.amount(),
                message.groupName());
        notificationLogRepository.save(NotificationLog.builder()
                .eventType(NotificationEventType.SETTLEMENT_RECORDED)
                .recipientUserId(message.toUserId())
                .recipientEmail(message.toEmail())
                .payload(jsonMapper.writeValueAsString(message))
                .status(NotificationStatus.SENT)
                .createdAt(Instant.now())
                .build());
    }
}
