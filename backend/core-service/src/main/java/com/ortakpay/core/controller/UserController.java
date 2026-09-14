package com.ortakpay.core.controller;

import com.ortakpay.core.domain.User;
import com.ortakpay.core.dto.UserResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @GetMapping("/me")
    public UserResponse me(@AuthenticationPrincipal User currentUser) {
        return new UserResponse(currentUser.getId(), currentUser.getEmail(), currentUser.getDisplayName());
    }
}
