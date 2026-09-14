package com.ortakpay.core.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import com.ortakpay.core.config.RabbitConfig;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

@ExtendWith(MockitoExtension.class)
class EventPublisherListenerTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private EventPublisherListener listener;

    @Test
    void onExpenseCreated_publishesToEventsExchangeWithExpenseCreatedRoutingKey() {
        ParticipantShare share = new ParticipantShare(UUID.randomUUID(), "alice@example.com", "Alice", new BigDecimal("5.00"));
        ExpenseCreatedInternalEvent event = new ExpenseCreatedInternalEvent(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Trip",
                UUID.randomUUID(),
                "Payer",
                new BigDecimal("10.00"),
                "Dinner",
                List.of(share));

        listener.onExpenseCreated(event);

        ArgumentCaptor<Object> payloadCaptor = ArgumentCaptor.forClass(Object.class);
        verify(rabbitTemplate)
                .convertAndSend(eq(RabbitConfig.EVENTS_EXCHANGE), eq("expense.created"), payloadCaptor.capture());

        assertThat(payloadCaptor.getValue()).isInstanceOf(ExpenseCreatedMessage.class);
        ExpenseCreatedMessage message = (ExpenseCreatedMessage) payloadCaptor.getValue();
        assertThat(message.expenseId()).isEqualTo(event.expenseId());
        assertThat(message.groupId()).isEqualTo(event.groupId());
        assertThat(message.amount()).isEqualByComparingTo("10.00");
        assertThat(message.shares()).containsExactly(share);
    }

    @Test
    void onGroupMemberAdded_publishesToEventsExchangeWithGroupMemberAddedRoutingKey() {
        UUID newMemberId = UUID.randomUUID();
        GroupMemberAddedInternalEvent event = new GroupMemberAddedInternalEvent(
                UUID.randomUUID(), "Trip", newMemberId, "new@example.com", "New Member", "Creator");

        listener.onGroupMemberAdded(event);

        ArgumentCaptor<Object> payloadCaptor = ArgumentCaptor.forClass(Object.class);
        verify(rabbitTemplate)
                .convertAndSend(eq(RabbitConfig.EVENTS_EXCHANGE), eq("group.member.added"), payloadCaptor.capture());

        assertThat(payloadCaptor.getValue()).isInstanceOf(GroupMemberAddedMessage.class);
        GroupMemberAddedMessage message = (GroupMemberAddedMessage) payloadCaptor.getValue();
        assertThat(message.groupId()).isEqualTo(event.groupId());
        assertThat(message.newMemberId()).isEqualTo(newMemberId);
        assertThat(message.newMemberEmail()).isEqualTo("new@example.com");
        assertThat(message.addedByDisplayName()).isEqualTo("Creator");
    }

    @Test
    void onSettlementRecorded_publishesToEventsExchangeWithSettlementRecordedRoutingKey() {
        SettlementRecordedInternalEvent event = new SettlementRecordedInternalEvent(
                UUID.randomUUID(),
                "Trip",
                UUID.randomUUID(),
                "From",
                UUID.randomUUID(),
                "to@example.com",
                "To",
                new BigDecimal("20.00"));

        listener.onSettlementRecorded(event);

        ArgumentCaptor<Object> payloadCaptor = ArgumentCaptor.forClass(Object.class);
        verify(rabbitTemplate)
                .convertAndSend(eq(RabbitConfig.EVENTS_EXCHANGE), eq("settlement.recorded"), payloadCaptor.capture());

        assertThat(payloadCaptor.getValue()).isInstanceOf(SettlementRecordedMessage.class);
        SettlementRecordedMessage message = (SettlementRecordedMessage) payloadCaptor.getValue();
        assertThat(message.fromUserId()).isEqualTo(event.fromUserId());
        assertThat(message.toEmail()).isEqualTo("to@example.com");
        assertThat(message.amount()).isEqualByComparingTo("20.00");
    }
}
