package com.ortakpay.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.ortakpay.notification.AbstractIntegrationTest;
import com.ortakpay.notification.config.RabbitConfig;
import com.ortakpay.notification.domain.NotificationEventType;
import com.ortakpay.notification.domain.NotificationLog;
import com.ortakpay.notification.domain.NotificationStatus;
import com.ortakpay.notification.dto.ExpenseCreatedMessage;
import com.ortakpay.notification.dto.GroupMemberAddedMessage;
import com.ortakpay.notification.dto.SettlementRecordedMessage;
import com.ortakpay.notification.repository.NotificationLogRepository;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Publishes directly to the shared exchange (not via core-service, which isn't
 * running here) and waits for NotificationListener to consume and persist it -
 * message handling is asynchronous, so a direct post-publish assertion would be
 * flaky; Awaitility polls until the expected row shows up or the timeout expires.
 */
class NotificationListenerIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private NotificationLogRepository notificationLogRepository;

    @Test
    void expenseCreatedMessage_isConsumedAndPersistedForEachParticipant() {
        String participantEmail = "participant-" + UUID.randomUUID() + "@example.com";
        UUID participantId = UUID.randomUUID();

        ExpenseCreatedMessage message = new ExpenseCreatedMessage(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Trip",
                UUID.randomUUID(),
                "Payer",
                new BigDecimal("10.00"),
                "Dinner",
                List.of(new ExpenseCreatedMessage.ParticipantShare(
                        participantId, participantEmail, "Participant", new BigDecimal("5.00"))));

        rabbitTemplate.convertAndSend(RabbitConfig.EVENTS_EXCHANGE, "expense.created", message);

        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            List<NotificationLog> logs = notificationLogRepository.findByRecipientEmail(participantEmail);
            assertThat(logs).hasSize(1);
            NotificationLog log = logs.get(0);
            assertThat(log.getEventType()).isEqualTo(NotificationEventType.EXPENSE_CREATED);
            assertThat(log.getRecipientUserId()).isEqualTo(participantId);
            assertThat(log.getStatus()).isEqualTo(NotificationStatus.SENT);
            assertThat(log.getPayload()).contains("Dinner");
        });
    }

    @Test
    void groupMemberAddedMessage_isConsumedAndPersisted() {
        String newMemberEmail = "newmember-" + UUID.randomUUID() + "@example.com";
        UUID newMemberId = UUID.randomUUID();

        GroupMemberAddedMessage message = new GroupMemberAddedMessage(
                UUID.randomUUID(), "Trip", newMemberId, newMemberEmail, "New Member", "Creator");

        rabbitTemplate.convertAndSend(RabbitConfig.EVENTS_EXCHANGE, "group.member.added", message);

        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            List<NotificationLog> logs = notificationLogRepository.findByRecipientEmail(newMemberEmail);
            assertThat(logs).hasSize(1);
            NotificationLog log = logs.get(0);
            assertThat(log.getEventType()).isEqualTo(NotificationEventType.GROUP_MEMBER_ADDED);
            assertThat(log.getRecipientUserId()).isEqualTo(newMemberId);
            assertThat(log.getStatus()).isEqualTo(NotificationStatus.SENT);
        });
    }

    @Test
    void settlementRecordedMessage_isConsumedAndPersisted() {
        String toEmail = "to-" + UUID.randomUUID() + "@example.com";
        UUID toUserId = UUID.randomUUID();

        SettlementRecordedMessage message = new SettlementRecordedMessage(
                UUID.randomUUID(),
                "Trip",
                UUID.randomUUID(),
                "From User",
                toUserId,
                toEmail,
                "To User",
                new BigDecimal("20.00"));

        rabbitTemplate.convertAndSend(RabbitConfig.EVENTS_EXCHANGE, "settlement.recorded", message);

        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            List<NotificationLog> logs = notificationLogRepository.findByRecipientEmail(toEmail);
            assertThat(logs).hasSize(1);
            NotificationLog log = logs.get(0);
            assertThat(log.getEventType()).isEqualTo(NotificationEventType.SETTLEMENT_RECORDED);
            assertThat(log.getRecipientUserId()).isEqualTo(toUserId);
            assertThat(log.getStatus()).isEqualTo(NotificationStatus.SENT);
        });
    }
}
