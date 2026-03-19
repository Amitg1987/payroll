package com.embeddedpayroll.backend.security;

import java.util.Set;

public record ActorContext(
    Long organizationId,
    String actorKey,
    String displayName,
    Set<String> authorities,
    AuthenticationMode authenticationMode,
    String partnerClientCode
) {

    public boolean hasAuthority(String authority) {
        return authorities.contains(authority);
    }

    public enum AuthenticationMode {
        BASIC_USER,
        API_KEY
    }
}
