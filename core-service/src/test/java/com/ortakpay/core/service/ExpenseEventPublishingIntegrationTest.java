package com.ortakpay.core.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.ortakpay.core.AbstractIntegrationTest;
import com.ortakpay.core.domain.Group;
import com.ortakpay.core.domain.GroupMember;
import com.ortakpay.core.domain.SplitType;
import com.ortakpay.core.domain.User;
import com.ortakpay.core.dto.CreateExpenseRequest;
import com.ortakpay.core.dto.ParticipantInput;
import com.ortakpay.core.event.ExpenseCreatedInternalEvent;
import com.ortakpay.core.repository.GroupMemberRepository;
import com.ortakpay.core.repository.GroupRepository;
import com.ortakpay.core.repository.UserRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Proves the AFTER_COMMIT design from docs/adr/0010-transactional-outbox-lite.md
 * actually depends on a real commit having happened: uses TransactionTemplate with
 * PROPAGATION_REQUIRES_NEW (same pattern as BalanceOptimisticLockingIntegrationTest)
 * so the transaction genuinely commits, then checks ApplicationEvents - which
 * records every event published through the real ApplicationEventPublisher during
 * the test - for the event createExpense is expected to publish as its last step.
 */
@RecordApplicationEvents
class ExpenseEventPublishingIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private ExpenseService expenseService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GroupRepository groupRepository;

    @Autowired
    private GroupMemberRepository groupMemberRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private ApplicationEvents applicationEvents;

    private record Fixture(User payer, User ower, Group group) {}

    @Test
    void createExpense_afterRealCommit_publishesExpenseCreatedInternalEvent() {
        TransactionTemplate requiresNew = new TransactionTemplate(transactionManager);
        requiresNew.setPropagationBehavior(TransactionTemplate.PROPAGATION_REQUIRES_NEW);

        Fixture fixture = requiresNew.execute(status -> {
            User payer = userRepository.save(User.builder()
                    .email("payer-" + UUID.randomUUID() + "@example.com")
                    .passwordHash("hash")
                    .displayName("Payer")
                    .build());
            User ower = userRepository.save(User.builder()
                    .email("ower-" + UUID.randomUUID() + "@example.com")
                    .passwordHash("hash")
                    .displayName("Ower")
                    .build());
            Group group = groupRepository.save(
                    Group.builder().name("Event Test Group").createdBy(payer).build());
            groupMemberRepository.save(
                    GroupMember.builder().group(group).user(payer).build());
            groupMemberRepository.save(
                    GroupMember.builder().group(group).user(ower).build());
            return new Fixture(payer, ower, group);
        });

        CreateExpenseRequest request = new CreateExpenseRequest(
                fixture.payer().getId(),
                new BigDecimal("10.00"),
                "Event Test Expense",
                SplitType.EQUAL,
                List.of(
                        new ParticipantInput(fixture.payer().getId(), null),
                        new ParticipantInput(fixture.ower().getId(), null)));

        requiresNew.execute(
                status -> expenseService.createExpense(fixture.group().getId(), fixture.payer().getId(), request));

        assertThat(applicationEvents.stream(ExpenseCreatedInternalEvent.class)).anySatisfy(event -> {
            assertThat(event.groupId()).isEqualTo(fixture.group().getId());
            assertThat(event.paidByUserId()).isEqualTo(fixture.payer().getId());
            assertThat(event.amount()).isEqualByComparingTo("10.00");
            assertThat(event.description()).isEqualTo("Event Test Expense");
            assertThat(event.shares()).hasSize(2);
        });
    }
}
