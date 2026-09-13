package com.ortakpay.core.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ortakpay.core.AbstractIntegrationTest;
import com.ortakpay.core.domain.User;
import com.ortakpay.core.dto.AddMemberRequest;
import com.ortakpay.core.dto.CreateGroupRequest;
import com.ortakpay.core.dto.LoginRequest;
import com.ortakpay.core.dto.RegisterRequest;
import com.ortakpay.core.repository.GroupMemberRepository;
import com.ortakpay.core.repository.UserRepository;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

class GroupIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GroupMemberRepository groupMemberRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void createGroup_addsCreatorAsMember() throws Exception {
        AuthedUser creator = registerAndLogin();

        UUID groupId = createGroup(creator.token(), "Trip");

        assertThat(groupMemberRepository.existsByGroup_IdAndUser_Id(groupId, creator.user().getId()))
                .isTrue();
    }

    @Test
    void addMember_byMember_succeeds_butByNonMember_isForbidden() throws Exception {
        AuthedUser creator = registerAndLogin();
        AuthedUser outsider = registerAndLogin();
        AuthedUser newMember = registerAndLogin();
        UUID groupId = createGroup(creator.token(), "Trip");

        mockMvc.perform(post("/api/groups/{groupId}/members", groupId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + outsider.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new AddMemberRequest(newMember.user().getEmail()))))
                .andExpect(status().isForbidden())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
        assertThat(groupMemberRepository.existsByGroup_IdAndUser_Id(groupId, newMember.user().getId()))
                .isFalse();

        mockMvc.perform(post("/api/groups/{groupId}/members", groupId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + creator.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new AddMemberRequest(newMember.user().getEmail()))))
                .andExpect(status().isCreated());

        assertThat(groupMemberRepository.existsByGroup_IdAndUser_Id(groupId, newMember.user().getId()))
                .isTrue();
    }

    @Test
    void requestToNonexistentGroup_returns404() throws Exception {
        AuthedUser caller = registerAndLogin();

        mockMvc.perform(get("/api/groups/{groupId}", UUID.randomUUID())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + caller.token()))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void nonMember_cannotAccessGroupDetailsExpensesOrBalances() throws Exception {
        AuthedUser creator = registerAndLogin();
        AuthedUser outsider = registerAndLogin();
        UUID groupId = createGroup(creator.token(), "Trip");

        mockMvc.perform(get("/api/groups/{groupId}", groupId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + outsider.token()))
                .andExpect(status().isForbidden())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));

        mockMvc.perform(get("/api/groups/{groupId}/expenses", groupId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + outsider.token()))
                .andExpect(status().isForbidden())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));

        mockMvc.perform(get("/api/groups/{groupId}/balances", groupId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + outsider.token()))
                .andExpect(status().isForbidden())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
    }

    private UUID createGroup(String token, String name) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/groups")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateGroupRequest(name))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value(name))
                .andExpect(jsonPath("$.members.length()").value(1))
                .andReturn();
        return UUID.fromString(objectMapper
                .readTree(result.getResponse().getContentAsString())
                .get("id")
                .asText());
    }

    private AuthedUser registerAndLogin() throws Exception {
        String email = "user-" + UUID.randomUUID() + "@example.com";
        RegisterRequest registerRequest = new RegisterRequest(email, "password123", "Group Tester");
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
}
