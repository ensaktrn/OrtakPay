package com.ortakpay.core.service;

import com.ortakpay.core.domain.User;
import com.ortakpay.core.dto.AuthResponse;
import com.ortakpay.core.dto.LoginRequest;
import com.ortakpay.core.dto.RegisterRequest;
import com.ortakpay.core.exception.DuplicateEmailException;
import com.ortakpay.core.repository.UserRepository;
import com.ortakpay.core.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String INVALID_CREDENTIALS_MESSAGE = "Invalid email or password";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Transactional
    public User register(RegisterRequest request) {
        userRepository.findByEmail(request.email()).ifPresent(existing -> {
            throw new DuplicateEmailException("Email already registered: " + request.email());
        });

        User user = User.builder()
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .displayName(request.displayName())
                .build();
        return userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        User user = userRepository
                .findByEmail(request.email())
                .orElseThrow(() -> new BadCredentialsException(INVALID_CREDENTIALS_MESSAGE));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BadCredentialsException(INVALID_CREDENTIALS_MESSAGE);
        }

        JwtService.IssuedToken issued = jwtService.generateToken(user);
        return new AuthResponse(issued.token(), issued.expiresAt());
    }
}
