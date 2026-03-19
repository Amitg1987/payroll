package com.embeddedpayroll.backend.dto;

import com.embeddedpayroll.backend.model.UserAccount;
import com.embeddedpayroll.backend.security.ActorContext;
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
            userAccount.getRoles().stream().map(Enum::name).collect(java.util.stream.Collectors.toUnmodifiableSet()),
            ActorContext.AuthenticationMode.BASIC_USER.name(),
            null
        );
    }

    public static CurrentUserResponse fromActorContext(ActorContext actorContext) {
        return new CurrentUserResponse(
            null,
            actorContext.organizationId(),
            actorContext.actorKey(),
            actorContext.displayName(),
            null,
            actorContext.authorities().stream()
                .map(authority -> authority.replaceFirst("^ROLE_", ""))
                .collect(java.util.stream.Collectors.toUnmodifiableSet()),
            actorContext.authenticationMode().name(),
            actorContext.partnerClientCode()
        );
    }

    public record CurrentUserResponse(
        Long id,
        Long organizationId,
        String username,
        String fullName,
        String email,
        Set<String> roles,
        String authenticationMode,
        String partnerClientCode
    ) {
    }
}
