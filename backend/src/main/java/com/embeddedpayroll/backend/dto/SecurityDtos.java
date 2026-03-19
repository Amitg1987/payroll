package com.embeddedpayroll.backend.dto;

import com.embeddedpayroll.backend.model.UserAccount;
import java.util.Set;

public final class SecurityDtos {

    private SecurityDtos() {
    }

    public static CurrentUserResponse fromEntity(UserAccount userAccount) {
        return new CurrentUserResponse(
            userAccount.getId(),
            userAccount.getOrganization().getId(),
            userAccount.getUsername(),
            userAccount.getFullName(),
            userAccount.getEmail(),
            userAccount.getRoles()
        );
    }

    public record CurrentUserResponse(
        Long id,
        Long organizationId,
        String username,
        String fullName,
        String email,
        Set<UserAccount.RoleName> roles
    ) {
    }
}
