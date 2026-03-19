package com.embeddedpayroll.backend.controller;

import com.embeddedpayroll.backend.dto.SecurityDtos;
import com.embeddedpayroll.backend.repository.UserAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/security")
@RequiredArgsConstructor
public class SecurityController {

    private final UserAccountRepository userAccountRepository;

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public SecurityDtos.CurrentUserResponse currentUser(Authentication authentication) {
        return userAccountRepository.findByUsername(authentication.getName())
            .map(SecurityDtos::fromEntity)
            .orElseThrow(() -> new IllegalArgumentException("Authenticated user not found"));
    }
}
