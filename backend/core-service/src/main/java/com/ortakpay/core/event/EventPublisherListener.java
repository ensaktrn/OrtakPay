package com.ortakpay.core.event;

import com.ortakpay.core.config.RabbitConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Maps each internal event to its wire-format message and publishes it - see
 * docs/adr/0010-transactional-outbox-lite.md for why this only happens
 * AFTER_COMMIT rather than inline in the service method.
 */
@Component
@RequiredArgsConstructor
public class EventPublisherListener {

    public static final String EXPENSE_CREATED_ROUTING_KEY = "expense.created";
    public static final String GROUP_MEMBER_ADDED_ROUTING_KEY = "group.member.added";
    public static final String SETTLEMENT_RECORDED_ROUTING_KEY = "settlement.recorded";
    public static final String BALANCE_REMINDER_ROUTING_KEY = "settlement.reminder";

    private final RabbitTemplate rabbitTemplate;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onExpenseCreated(ExpenseCreatedInternalEvent event) {
        ExpenseCreatedMessage message = new ExpenseCreatedMessage(
                event.expenseId(),
                event.groupId(),
                event.groupName(),
                event.paidByUserId(),
                event.paidByDisplayName(),
                event.amount(),
                event.description(),
                event.shares());
        rabbitTemplate.convertAndSend(RabbitConfig.EVENTS_EXCHANGE, EXPENSE_CREATED_ROUTING_KEY, message);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onGroupMemberAdded(GroupMemberAddedInternalEvent event) {
        GroupMemberAddedMessage message = new GroupMemberAddedMessage(
                event.groupId(),
                event.groupName(),
                event.newMemberId(),
                event.newMemberEmail(),
                event.newMemberDisplayName(),
                event.addedByDisplayName());
        rabbitTemplate.convertAndSend(RabbitConfig.EVENTS_EXCHANGE, GROUP_MEMBER_ADDED_ROUTING_KEY, message);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onSettlementRecorded(SettlementRecordedInternalEvent event) {
        SettlementRecordedMessage message = new SettlementRecordedMessage(
                event.groupId(),
                event.groupName(),
                event.fromUserId(),
                event.fromDisplayName(),
                event.toUserId(),
                event.toEmail(),
                event.toDisplayName(),
                event.amount());
        rabbitTemplate.convertAndSend(RabbitConfig.EVENTS_EXCHANGE, SETTLEMENT_RECORDED_ROUTING_KEY, message);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onBalanceReminder(BalanceReminderInternalEvent event) {
        BalanceReminderMessage message = new BalanceReminderMessage(
                event.groupId(), event.groupName(), event.userId(), event.email(), event.displayName(), event.owedAmount());
        rabbitTemplate.convertAndSend(RabbitConfig.EVENTS_EXCHANGE, BALANCE_REMINDER_ROUTING_KEY, message);
    }
}
