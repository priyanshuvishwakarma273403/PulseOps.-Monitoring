package com.pulseops.shared.security;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class GatewayHeaderAuthFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String userIdStr = request.getHeader("X-User-Id");
        String email = request.getHeader("X-User-Email");
        String rolesStr = request.getHeader("X-User-Roles");

        if (userIdStr != null && email != null) {
            try {
                Long userId = Long.parseLong(userIdStr);
                List<SimpleGrantedAuthority> authorities = Collections.emptyList();
                if (rolesStr != null && !rolesStr.isEmpty()) {
                    authorities = Arrays.stream(rolesStr.split(","))
                            .map(role -> new SimpleGrantedAuthority("ROLE_" + role.trim().toUpperCase()))
                            .collect(Collectors.toList());
                }

                UserPrincipal principal = new UserPrincipal(userId, email, authorities);

                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        principal, null, authorities);
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (NumberFormatException e) {
                // Invalid user ID header format, skip authentication
            }
        }

        filterChain.doFilter(request, response);
    }
}
