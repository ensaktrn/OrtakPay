package com.ortakpay.core.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ortakpay.core.domain.User;
import com.ortakpay.core.dto.AuthResponse;
import com.ortakpay.core.dto.LoginRequest;
import com.ortakpay.core.dto.RegisterRequest;
import com.ortakpay.core.exception.DuplicateEmailException;
import com.ortakpay.core.repository.UserRepository;
import com.ortakpay.core.security.JwtService;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    @Test
    void register_savesUserWithEncodedPassword_whenEmailNotTaken() {
        RegisterRequest request = new RegisterRequest("alice@example.com", "password123", "Alice");
        when(userRepository.findByEmail(request.email())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(request.password())).thenReturn("encoded-hash");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User result = authService.register(request);

        assertThat(result.getEmail()).isEqualTo("alice@example.com");
        assertThat(result.getPasswordHash()).isEqualTo("encoded-hash");
        assertThat(result.getDisplayName()).isEqualTo("Alice");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void register_throwsDuplicateEmailException_whenEmailAlreadyExists() {
        RegisterRequest request = new RegisterRequest("alice@example.com", "password123", "Alice");
        when(userRepository.findByEmail(request.email()))
                .thenReturn(Optional.of(User.builder().build()));

        assertThatThrownBy(() -> authService.register(request)).isInstanceOf(DuplicateEmailException.class);

        verify(userRepository, never()).save(any());
    }

    @Test
    void login_returnsAuthResponseWithToken_whenCredentialsValid() {
        User user = User.builder()
                .id(UUID.randomUUID())
                .email("alice@example.com")
                .passwordHash("hashed-pw")
                .displayName("Alice")
                .build();
        LoginRequest request = new LoginRequest("alice@example.com", "password123");
        Instant expiresAt = Instant.now().plusSeconds(3600);
        when(userRepository.findByEmail(request.email())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(request.password(), user.getPasswordHash())).thenReturn(true);
        when(jwtService.generateToken(user)).thenReturn(new JwtService.IssuedToken("jwt-token", expiresAt));

        AuthResponse response = authService.login(request);

        assertThat(response.token()).isEqualTo("jwt-token");
        assertThat(response.expiresAt()).isEqualTo(expiresAt);
    }

    @Test
    void login_throwsBadCredentialsException_whenUserNotFound() {
        LoginRequest request = new LoginRequest("missing@example.com", "password123");
        when(userRepository.findByEmail(request.email())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(request)).isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void login_throwsBadCredentialsException_whenPasswordWrong() {
        User user = User.builder()
                .id(UUID.randomUUID())
                .email("alice@example.com")
                .passwordHash("hashed-pw")
                .displayName("Alice")
                .build();
        LoginRequest request = new LoginRequest("alice@example.com", "wrong-password");
        when(userRepository.findByEmail(request.email())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(request.password(), user.getPasswordHash())).thenReturn(false);

        assertThatThrownBy(() -> authService.login(request)).isInstanceOf(BadCredentialsException.class);
    }
}
