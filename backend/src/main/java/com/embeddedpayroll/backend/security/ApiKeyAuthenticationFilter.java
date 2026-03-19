package com.embeddedpayroll.backend.security;

import com.embeddedpayroll.backend.repository.PartnerApiClientRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@RequiredArgsConstructor
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

    private static final String API_KEY_HEADER = "X-API-Key";
    private static final int PREFIX_LENGTH = 12;

    private final PartnerApiClientRepository partnerApiClientRepository;
    private final ApiKeyHashService apiKeyHashService;

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            String rawApiKey = request.getHeader(API_KEY_HEADER);
            if (StringUtils.hasText(rawApiKey)) {
                String keyPrefix = rawApiKey.substring(0, Math.min(PREFIX_LENGTH, rawApiKey.length()));
                partnerApiClientRepository.findByKeyPrefix(keyPrefix)
                    .filter(partnerApiClient -> partnerApiClient.isActive())
                    .filter(partnerApiClient -> partnerApiClient.getApiKeyHash().equals(apiKeyHashService.hash(rawApiKey)))
                    .ifPresent(partnerApiClient -> {
                        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                            partnerApiClient.getClientCode(),
                            rawApiKey,
                            List.of(
                                new SimpleGrantedAuthority("ROLE_DEVELOPER_PLATFORM_INTEGRATOR"),
                                new SimpleGrantedAuthority("ROLE_PARTNER_API")
                            )
                        );
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                    });
            }
        }

        filterChain.doFilter(request, response);
    }
}
