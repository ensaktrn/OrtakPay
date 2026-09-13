package com.ortakpay.core.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ortakpay.core.AbstractIntegrationTest;
import com.ortakpay.core.domain.Group;
import com.ortakpay.core.domain.GroupMember;
import com.ortakpay.core.domain.User;
import com.ortakpay.core.dto.LoginRequest;
import com.ortakpay.core.dto.RegisterRequest;
import com.ortakpay.core.dto.SettlementRequest;
import com.ortakpay.core.repository.BalanceRepository;
import com.ortakpay.core.repository.GroupMemberRepository;
import com.ortakpay.core.repository.GroupRepository;
import com.ortakpay.core.repository.UserRepository;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

class SettlementIntegrationTest extends AbstractIntegrationTest {

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
    void settlement_updatesBothBalances_inOppositeDirections() throws Exception {
        AuthedUser a = registerAndLogin();
        AuthedUser b = registerAndLogin();
        Group group = persistGroupWithMembers(a.user(), a.user(), b.user());

        mockMvc.perform(post("/api/groups/{groupId}/settlements", group.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + a.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new SettlementRequest(b.user().getId(), new BigDecimal("20.00")))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.fromUserId").value(a.user().getId().toString()))
                .andExpect(jsonPath("$.toUserId").value(b.user().getId().toString()));

        var aBalance = balanceRepository.findByGroupAndUser(group, a.user()).orElseThrow();
        var bBalance = balanceRepository.findByGroupAndUser(group, b.user()).orElseThrow();

        assertThat(aBalance.getNetAmount()).isEqualByComparingTo("20.00");
        assertThat(bBalance.getNetAmount()).isEqualByComparingTo("-20.00");
    }

    @Test
    void selfSettlement_returns400() throws Exception {
        AuthedUser a = registerAndLogin();
        Group group = persistGroupWithMembers(a.user(), a.user());

        mockMvc.perform(post("/api/groups/{groupId}/settlements", group.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + a.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new SettlementRequest(a.user().getId(), new BigDecimal("5.00")))))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
    }

    /**
     * Security test: the request body's shape has no fromUserId field at all
     * (see SettlementRequest), but a stale/malicious client could still stuff an
     * extra "fromUserId" property into the raw JSON hoping it gets bound. Jackson
     * silently ignores unknown properties by default, and the service only ever
     * reads currentUserId from the authenticated principal - so this proves that,
     * sending the exact same spoofed body, each of two different tokens only ever
     * debits its OWN holder, never the identity named in the bogus field.
     */
    @Test
    void fromUser_isAlwaysTheAuthenticatedCaller_neverASpoofableRequestField() throws Exception {
        AuthedUser a = registerAndLogin();
        AuthedUser b = registerAndLogin();
        AuthedUser bogus = registerAndLogin();

        Group group1 = persistGroupWithMembers(a.user(), a.user(), b.user(), bogus.user());
        String spoofedBodyClaimingBogus1 = String.format(
                "{\"fromUserId\":\"%s\",\"toUserId\":\"%s\",\"amount\":15.00}",
                bogus.user().getId(), b.user().getId());

        mockMvc.perform(post("/api/groups/{groupId}/settlements", group1.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + a.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(spoofedBodyClaimingBogus1))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.fromUserId").value(a.user().getId().toString()));

        assertThat(balanceRepository
                        .findByGroupAndUser(group1, a.user())
                        .orElseThrow()
                        .getNetAmount())
                .isEqualByComparingTo("15.00");
        assertThat(balanceRepository.findByGroupAndUser(group1, bogus.user())).isEmpty();

        Group group2 = persistGroupWithMembers(b.user(), b.user(), a.user(), bogus.user());
        String spoofedBodyClaimingBogus2 = String.format(
                "{\"fromUserId\":\"%s\",\"toUserId\":\"%s\",\"amount\":15.00}",
                bogus.user().getId(), a.user().getId());

        mockMvc.perform(post("/api/groups/{groupId}/settlements", group2.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + b.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(spoofedBodyClaimingBogus2))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.fromUserId").value(b.user().getId().toString()));

        assertThat(balanceRepository
                        .findByGroupAndUser(group2, b.user())
                        .orElseThrow()
                        .getNetAmount())
                .isEqualByComparingTo("15.00");
        assertThat(balanceRepository.findByGroupAndUser(group2, bogus.user())).isEmpty();
    }

    private AuthedUser registerAndLogin() throws Exception {
        String email = "user-" + UUID.randomUUID() + "@example.com";
        RegisterRequest registerRequest = new RegisterRequest(email, "password123", "Settlement Tester");
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

    private Group persistGroupWithMembers(User creator, User... members) {
        Group group = groupRepository.save(
                Group.builder().name("Settlement Group").createdBy(creator).build());
        for (User member : members) {
            groupMemberRepository.save(
                    GroupMember.builder().group(group).user(member).build());
        }
        return group;
    }
}
