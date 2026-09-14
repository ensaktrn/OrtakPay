package com.ortakpay.core.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.ortakpay.core.domain.Expense;
import com.ortakpay.core.domain.Group;
import com.ortakpay.core.domain.SplitType;
import com.ortakpay.core.domain.User;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

class ExpenseRepositoryTest extends AbstractRepositoryIT {

    @Autowired
    private ExpenseRepository expenseRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void savesAndReadsExpenseById() {
        User payer = entityManager.persistAndFlush(User.builder()
                .email("payer@example.com")
                .passwordHash("hashed-pw")
                .displayName("Payer")
                .build());
        Group group = entityManager.persistAndFlush(
                Group.builder().name("Trip").createdBy(payer).build());

        Expense expense = Expense.builder()
                .group(group)
                .paidBy(payer)
                .amount(new BigDecimal("123.45"))
                .description("Dinner")
                .splitType(SplitType.EQUAL)
                .build();

        Expense saved = expenseRepository.saveAndFlush(expense);
        entityManager.clear();

        Optional<Expense> found = expenseRepository.findById(saved.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getAmount()).isEqualByComparingTo("123.45");
        assertThat(found.get().getDescription()).isEqualTo("Dinner");
        assertThat(found.get().getSplitType()).isEqualTo(SplitType.EQUAL);
        assertThat(found.get().getGroup().getId()).isEqualTo(group.getId());
        assertThat(found.get().getPaidBy().getId()).isEqualTo(payer.getId());
        assertThat(found.get().getCreatedAt()).isNotNull();
        assertThat(found.get().getUpdatedAt()).isNotNull();
    }
}
