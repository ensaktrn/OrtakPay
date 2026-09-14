package com.ortakpay.core.controller;

import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ortakpay.core.AbstractIntegrationTest;
import com.ortakpay.core.dto.LoginRequest;
import com.ortakpay.core.dto.RegisterRequest;
import com.ortakpay.core.security.JwtProperties;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Uses a plain {@code new ObjectMapper()} rather than an injected bean: Spring Boot
 * 4 no longer auto-configures a classic Jackson 2 {@code ObjectMapper}, and nothing
 * serialized here (request DTOs, a raw response tree read) needs any special module.
 */
class AuthFlowIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtProperties jwtProperties;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void register_thenDuplicateRegister_conflicts() throws Exception {
        RegisterRequest request = new RegisterRequest(uniqueEmail(), "password123", "Alice");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value(request.email()));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void registerThenLogin_returnsToken() throws Exception {
        String email = uniqueEmail();
        registerUser(email, "password123", "Bob");

        LoginRequest loginRequest = new LoginRequest(email, "password123");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.expiresAt").isNotEmpty());
    }

    @Test
    void login_withWrongPassword_returnsUnauthorized() throws Exception {
        String email = uniqueEmail();
        registerUser(email, "password123", "Carol");

        LoginRequest loginRequest = new LoginRequest(email, "wrong-password");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void me_withoutToken_returnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(unauthenticated());
    }

    @Test
    void me_withValidToken_returnsCurrentUser() throws Exception {
        String email = uniqueEmail();
        registerUser(email, "password123", "Dave");
        String token = login(email, "password123");

        mockMvc.perform(get("/api/users/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.displayName").value("Dave"));
    }

    @Test
    void me_withMalformedToken_returnsUnauthorizedNotServerError() throws Exception {
        mockMvc.perform(get("/api/users/me").header(HttpHeaders.AUTHORIZATION, "Bearer not.a.valid.jwt"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(unauthenticated());
    }

    @Test
    void me_withExpiredToken_returnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/users/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + expiredToken()))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(unauthenticated());
    }

    private String expiredToken() {
        SecretKey key = Keys.hmacShaKeyFor(jwtProperties.secret().getBytes(StandardCharsets.UTF_8));
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(UUID.randomUUID().toString())
                .claim("email", "expired@example.com")
                .issuedAt(Date.from(now.minusSeconds(120)))
                .expiration(Date.from(now.minusSeconds(60)))
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    private void registerUser(String email, String password, String displayName) throws Exception {
        RegisterRequest request = new RegisterRequest(email, password, displayName);
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    private String login(String email, String password) throws Exception {
        LoginRequest loginRequest = new LoginRequest(email, password);
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

    private String uniqueEmail() {
        return "user-" + UUID.randomUUID() + "@example.com";
    }
}
