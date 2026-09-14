package com.ortakpay.core.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.ortakpay.core.domain.Expense;
import com.ortakpay.core.domain.ExpenseShare;
import com.ortakpay.core.domain.Group;
import com.ortakpay.core.domain.SplitType;
import com.ortakpay.core.domain.User;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

class ExpenseShareRepositoryTest extends AbstractRepositoryIT {

    @Autowired
    private ExpenseShareRepository expenseShareRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void savesAndReadsExpenseShareById() {
        User payer = entityManager.persistAndFlush(User.builder()
                .email("payer@example.com")
                .passwordHash("hashed-pw")
                .displayName("Payer")
                .build());
        User owingUser = entityManager.persistAndFlush(User.builder()
                .email("owes@example.com")
                .passwordHash("hashed-pw")
                .displayName("Owes")
                .build());
        Group group = entityManager.persistAndFlush(
                Group.builder().name("Trip").createdBy(payer).build());
        Expense expense = entityManager.persistAndFlush(Expense.builder()
                .group(group)
                .paidBy(payer)
                .amount(new BigDecimal("100.00"))
                .description("Hotel")
                .splitType(SplitType.EQUAL)
                .build());

        ExpenseShare share = ExpenseShare.builder()
                .expense(expense)
                .user(owingUser)
                .owedAmount(new BigDecimal("50.00"))
                .build();

        ExpenseShare saved = expenseShareRepository.saveAndFlush(share);
        entityManager.clear();

        Optional<ExpenseShare> found = expenseShareRepository.findById(saved.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getOwedAmount()).isEqualByComparingTo("50.00");
        assertThat(found.get().getExpense().getId()).isEqualTo(expense.getId());
        assertThat(found.get().getUser().getId()).isEqualTo(owingUser.getId());
    }
}
