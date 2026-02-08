package org.fintech.wallet.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fintech.wallet.service.impl.TokenBlacklistService;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtTokenProvider tokenProvider;
    private final UserDetailsService userDetailsService;
    private final TokenBlacklistService tokenBlacklistService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        try {
            String jwt = getJwtFromRequest(request);

            if (StringUtils.hasText(jwt)) {

                // Check blacklist (single-device logout)
                if (tokenBlacklistService.isTokenBlacklisted(jwt)) {
                    log.warn("Attempted to use blacklisted token");
                    filterChain.doFilter(request, response);
                    return;
                }

                //Validate JWT signature + expiry
                if (tokenProvider.validateToken(jwt)) {

                    //Extract userId
                    UUID userId = tokenProvider.getUserId(jwt);

                    //Check logout-all invalidation
                    Long lastLogoutAt =
                            tokenBlacklistService.getLastLogoutAt(userId);

                    if (tokenProvider.isTokenIssuedBeforeLogout(jwt, lastLogoutAt)) {
                        log.warn("Token invalidated due to logout-all for user: {}", userId);
                        filterChain.doFilter(request, response);
                        return;
                    }

                    // Authenticate user
                    String username = tokenProvider.getUsernameFromToken(jwt);
                    UserDetails userDetails =
                            userDetailsService.loadUserByUsername(username);

                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails,
                                    null,
                                    userDetails.getAuthorities()
                            );

                    authentication.setDetails(
                            new WebAuthenticationDetailsSource().buildDetails(request)
                    );

                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    log.debug("Authentication set for user: {}", username);
                }
            }
        } catch (Exception ex) {
            log.error("Could not set user authentication", ex);
        }

        filterChain.doFilter(request, response);
    }

    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
