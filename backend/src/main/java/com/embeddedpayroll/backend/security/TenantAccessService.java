package com.embeddedpayroll.backend.security;

import com.embeddedpayroll.backend.model.PartnerApiClient;
import com.embeddedpayroll.backend.model.UserAccount;
import com.embeddedpayroll.backend.repository.PartnerApiClientRepository;
import com.embeddedpayroll.backend.repository.UserAccountRepository;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TenantAccessService {

    private final UserAccountRepository userAccountRepository;
    private final PartnerApiClientRepository partnerApiClientRepository;

    public ActorContext resolveActor(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalArgumentException("Authentication is required");
        }

        String principalName = authentication.getName();
        return userAccountRepository.findByUsername(principalName)
            .map(this::fromUser)
            .orElseGet(() -> partnerApiClientRepository.findByClientCode(principalName)
                .map(client -> fromApiClient(client, authentication))
                .orElseThrow(() -> new IllegalArgumentException("Authenticated actor not found: " + principalName)));
    }

    public void assertOrganizationAccess(Authentication authentication, Long organizationId) {
        ActorContext actorContext = resolveActor(authentication);
        if (!actorContext.organizationId().equals(organizationId)) {
            throw new IllegalArgumentException(
                "Authenticated actor does not have access to organization " + organizationId
            );
        }
    }

    private ActorContext fromUser(UserAccount userAccount) {
        Set<String> authorities = userAccount.getRoles().stream()
            .map(role -> "ROLE_" + role.name())
            .collect(java.util.stream.Collectors.toUnmodifiableSet());

        return new ActorContext(
            userAccount.getOrganization().getId(),
            userAccount.getUsername(),
            userAccount.getFullName(),
            authorities,
            ActorContext.AuthenticationMode.BASIC_USER,
            null
        );
    }

    private ActorContext fromApiClient(PartnerApiClient partnerApiClient, Authentication authentication) {
        Set<String> authorities = authentication.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .collect(java.util.stream.Collectors.toUnmodifiableSet());

        return new ActorContext(
            partnerApiClient.getOrganization().getId(),
            partnerApiClient.getClientCode(),
            partnerApiClient.getDisplayName(),
            authorities,
            ActorContext.AuthenticationMode.API_KEY,
            partnerApiClient.getClientCode()
        );
    }
}
