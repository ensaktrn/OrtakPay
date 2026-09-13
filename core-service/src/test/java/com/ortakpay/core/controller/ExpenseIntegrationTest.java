package com.ortakpay.core.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ortakpay.core.AbstractIntegrationTest;
import com.ortakpay.core.domain.Balance;
import com.ortakpay.core.domain.Group;
import com.ortakpay.core.domain.GroupMember;
import com.ortakpay.core.domain.SplitType;
import com.ortakpay.core.domain.User;
import com.ortakpay.core.dto.CreateExpenseRequest;
import com.ortakpay.core.dto.LoginRequest;
import com.ortakpay.core.dto.ParticipantInput;
import com.ortakpay.core.dto.RegisterRequest;
import com.ortakpay.core.repository.BalanceRepository;
import com.ortakpay.core.repository.GroupMemberRepository;
import com.ortakpay.core.repository.GroupRepository;
import com.ortakpay.core.repository.UserRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Note: the calling/authenticated user here is unrelated to the group under test -
 * resource-based authorization ("is the caller allowed to touch this group at all")
 * is explicitly out of scope until Faz 4, so any authenticated user's token is
 * sufficient to pass Spring Security's gate; only paidBy/participants membership
 * in the group is checked at this phase.
 */
class ExpenseIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GroupRepository groupRepository;

    @Autowired
    private GroupMemberRepository groupMemberRepository;

    @Autowired
    private BalanceRepository balanceRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void equalSplit_createsExpense_andUpdatesBalancesCorrectly() throws Exception {
        String token = registerAndLogin();
        User payer = persistUser();
        User ower1 = persistUser();
        User ower2 = persistUser();
        Group group = persistGroupWithMembers(payer, payer, ower1, ower2);

        CreateExpenseRequest request = new CreateExpenseRequest(
                payer.getId(),
                new BigDecimal("10.00"),
                "Dinner",
                SplitType.EQUAL,
                List.of(
                        new ParticipantInput(payer.getId(), null),
                        new ParticipantInput(ower1.getId(), null),
                        new ParticipantInput(ower2.getId(), null)));

        mockMvc.perform(post("/api/groups/{groupId}/expenses", group.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.amount").value(10.00))
                .andExpect(jsonPath("$.shares.length()").value(3));

        Balance payerBalance = balanceRepository.findByGroupAndUser(group, payer).orElseThrow();
        Balance ower1Balance = balanceRepository.findByGroupAndUser(group, ower1).orElseThrow();
        Balance ower2Balance = balanceRepository.findByGroupAndUser(group, ower2).orElseThrow();

        // payer paid 10.00 total but also owes their own EQUAL share (3.34, the
        // first participant in request order) back into the pool
        assertThat(payerBalance.getNetAmount()).isEqualByComparingTo("6.66");
        assertThat(ower1Balance.getNetAmount()).isEqualByComparingTo("-3.33");
        assertThat(ower2Balance.getNetAmount()).isEqualByComparingTo("-3.33");

        BigDecimal sumOfAllBalances = payerBalance
                .getNetAmount()
                .add(ower1Balance.getNetAmount())
                .add(ower2Balance.getNetAmount());
        assertThat(sumOfAllBalances).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void exactSplit_whenTotalsDoNotMatch_returns400() throws Exception {
        String token = registerAndLogin();
        User payer = persistUser();
        User ower = persistUser();
        Group group = persistGroupWithMembers(payer, payer, ower);

        CreateExpenseRequest request = new CreateExpenseRequest(
                payer.getId(),
                new BigDecimal("10.00"),
                "Groceries",
                SplitType.EXACT,
                List.of(
                        new ParticipantInput(payer.getId(), new BigDecimal("6.00")),
                        new ParticipantInput(ower.getId(), new BigDecimal("3.00"))));

        mockMvc.perform(post("/api/groups/{groupId}/expenses", group.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void nonGroupMemberParticipant_returns400() throws Exception {
        String token = registerAndLogin();
        User payer = persistUser();
        User outsider = persistUser(); // never added as a member of the group below
        Group group = persistGroupWithMembers(payer, payer);

        CreateExpenseRequest request = new CreateExpenseRequest(
                payer.getId(),
                new BigDecimal("10.00"),
                "Movie tickets",
                SplitType.EQUAL,
                List.of(new ParticipantInput(payer.getId(), null), new ParticipantInput(outsider.getId(), null)));

        mockMvc.perform(post("/api/groups/{groupId}/expenses", group.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void duplicateParticipantUserId_returns400NotServerError() throws Exception {
        String token = registerAndLogin();
        User payer = persistUser();
        User ower = persistUser();
        Group group = persistGroupWithMembers(payer, payer, ower);

        CreateExpenseRequest request = new CreateExpenseRequest(
                payer.getId(),
                new BigDecimal("10.00"),
                "Duplicate participant",
                SplitType.EQUAL,
                List.of(
                        new ParticipantInput(payer.getId(), null),
                        new ParticipantInput(ower.getId(), null),
                        new ParticipantInput(ower.getId(), null)));

        mockMvc.perform(post("/api/groups/{groupId}/expenses", group.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("Participant list contains duplicate user IDs"));
    }

    private String registerAndLogin() throws Exception {
        String email = "requester-" + UUID.randomUUID() + "@example.com";
        RegisterRequest registerRequest = new RegisterRequest(email, "password123", "Requester");
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated());

        LoginRequest loginRequest = new LoginRequest(email, "password123");
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper
                .readTree(result.getResponse().getContentAsString())
                .get("token")
                .asText();
    }

    private User persistUser() {
        return userRepository.save(User.builder()
                .email("member-" + UUID.randomUUID() + "@example.com")
                .passwordHash("hash")
                .displayName("Member")
                .build());
    }

    private Group persistGroupWithMembers(User creator, User... members) {
        Group group = groupRepository.save(
                Group.builder().name("Test Group").createdBy(creator).build());
        for (User member : members) {
            groupMemberRepository.save(
                    GroupMember.builder().group(group).user(member).build());
        }
        return group;
    }
}
