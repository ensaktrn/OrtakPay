package com.ortakpay.core.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
 * The authenticated caller must be a group member (Faz 4's GroupAccessGuard), so
 * every test below has the registered/logged-in user also be the payer - who is
 * always added as a group member anyway for the split/balance assertions to make
 * sense. A separate caller-is-not-a-member 403 case belongs to GroupIntegrationTest,
 * not here.
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
        AuthedUser caller = registerAndLogin();
        String token = caller.token();
        User payer = caller.user();
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
        AuthedUser caller = registerAndLogin();
        String token = caller.token();
        User payer = caller.user();
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
        AuthedUser caller = registerAndLogin();
        String token = caller.token();
        User payer = caller.user();
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
        AuthedUser caller = registerAndLogin();
        String token = caller.token();
        User payer = caller.user();
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

    @Test
    void getExpenses_returnsPagedResultsForGroupMember() throws Exception {
        AuthedUser caller = registerAndLogin();
        String token = caller.token();
        User payer = caller.user();
        User ower = persistUser();
        Group group = persistGroupWithMembers(payer, payer, ower);

        CreateExpenseRequest request = new CreateExpenseRequest(
                payer.getId(),
                new BigDecimal("10.00"),
                "Coffee",
                SplitType.EQUAL,
                List.of(new ParticipantInput(payer.getId(), null), new ParticipantInput(ower.getId(), null)));
        mockMvc.perform(post("/api/groups/{groupId}/expenses", group.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // Verifies Page<ExpenseResponse> actually serializes under Spring Boot 4's
        // Jackson 3 default stack (see docs/adr/0007-jackson-2-3-coexistence.md) as
        // Spring Data's stable PagedModel DTO (content + a nested "page" object),
        // not the raw, JSON-shape-unstable PageImpl - see application.yml's
        // spring.data.web.pageable.serialization-mode: via-dto.
        mockMvc.perform(get("/api/groups/{groupId}/expenses", group.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].description").value("Coffee"))
                .andExpect(jsonPath("$.content[0].shares.length()").value(2))
                .andExpect(jsonPath("$.page.totalElements").value(1));
    }

    @Test
    void getExpenses_defaultsToNewestFirst() throws Exception {
        AuthedUser caller = registerAndLogin();
        String token = caller.token();
        User payer = caller.user();
        Group group = persistGroupWithMembers(payer, payer);

        // Sleeps guarantee distinct createdAt instants: three requests fired back
        // to back could otherwise land on the same millisecond, which would make
        // the ordering assertion below flaky (id is a random UUID, not a
        // chronological tiebreaker - see ExpenseService.DEFAULT_EXPENSE_SORT).
        createSingleParticipantExpense(token, group.getId(), payer.getId(), "First");
        Thread.sleep(10);
        createSingleParticipantExpense(token, group.getId(), payer.getId(), "Second");
        Thread.sleep(10);
        createSingleParticipantExpense(token, group.getId(), payer.getId(), "Third");

        mockMvc.perform(get("/api/groups/{groupId}/expenses", group.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(3))
                .andExpect(jsonPath("$.content[0].description").value("Third"))
                .andExpect(jsonPath("$.content[1].description").value("Second"))
                .andExpect(jsonPath("$.content[2].description").value("First"));
    }

    private void createSingleParticipantExpense(String token, UUID groupId, UUID payerId, String description)
            throws Exception {
        CreateExpenseRequest request = new CreateExpenseRequest(
                payerId,
                new BigDecimal("5.00"),
                description,
                SplitType.EQUAL,
                List.of(new ParticipantInput(payerId, null)));
        mockMvc.perform(post("/api/groups/{groupId}/expenses", groupId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    private AuthedUser registerAndLogin() throws Exception {
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
        String token = objectMapper
                .readTree(result.getResponse().getContentAsString())
                .get("token")
                .asText();
        User user = userRepository.findByEmail(email).orElseThrow();
        return new AuthedUser(token, user);
    }

    private record AuthedUser(String token, User user) {}

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
