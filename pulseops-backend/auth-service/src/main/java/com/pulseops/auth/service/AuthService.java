package com.pulseops.auth.service;

import com.pulseops.auth.dto.*;
import com.pulseops.auth.entity.*;
import com.pulseops.auth.repository.*;
import com.pulseops.shared.exception.BadRequestException;
import com.pulseops.shared.exception.ResourceNotFoundException;
import com.pulseops.shared.security.JwtUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final OrganizationRepository organizationRepository;
    private final OrganizationMemberRepository organizationMemberRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;

    @Transactional
    public void signup(SignupRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email is already registered!");
        }

        String orgSlug = request.getOrganizationName().toLowerCase().replaceAll("[^a-z0-9-]", "-");
        if (organizationRepository.existsBySlug(orgSlug)) {
            orgSlug = orgSlug + "-" + System.currentTimeMillis() % 1000;
        }

        // 1. Get or Create Organization Role
        Role adminRole = roleRepository.findByName("ORG_ADMIN")
                .orElseGet(() -> roleRepository.save(Role.builder().name("ORG_ADMIN").build()));

        // Ensure default user roles are in DB
        roleRepository.findByName("SUPER_ADMIN")
                .orElseGet(() -> roleRepository.save(Role.builder().name("SUPER_ADMIN").build()));
        roleRepository.findByName("DEVELOPER")
                .orElseGet(() -> roleRepository.save(Role.builder().name("DEVELOPER").build()));
        roleRepository.findByName("VIEWER")
                .orElseGet(() -> roleRepository.save(Role.builder().name("VIEWER").build()));

        // 2. Create User
        User user = User.builder()
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .isEnabled(true)
                .roles(new HashSet<>(Collections.singletonList(adminRole)))
                .build();
        User savedUser = userRepository.save(user);

        // 3. Create Organization
        Organization organization = Organization.builder()
                .name(request.getOrganizationName())
                .slug(orgSlug)
                .build();
        Organization savedOrg = organizationRepository.save(organization);

        // 4. Map User to Organization as Admin Member
        OrganizationMember member = OrganizationMember.builder()
                .organization(savedOrg)
                .user(savedUser)
                .role(adminRole)
                .status("ACTIVE")
                .build();
        organizationMemberRepository.save(member);
    }

    @Transactional
    public TokenResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadRequestException("Invalid email or password!"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BadRequestException("Invalid email or password!");
        }

        List<String> roles = user.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toList());

        String accessToken = jwtUtils.generateToken(user.getEmail(), user.getId(), roles);
        String refreshTokenStr = UUID.randomUUID().toString();

        // Expire refresh token after 7 days
        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token(refreshTokenStr)
                .expiryDate(Instant.now().plusMillis(7 * 24 * 60 * 60 * 1000))
                .isRevoked(false)
                .build();
        refreshTokenRepository.save(refreshToken);

        List<OrganizationDto> orgs = organizationMemberRepository.findByUserId(user.getId()).stream()
                .map(m -> OrganizationDto.builder()
                        .id(m.getOrganization().getId())
                        .name(m.getOrganization().getName())
                        .slug(m.getOrganization().getSlug())
                        .build())
                .collect(Collectors.toList());

        return TokenResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshTokenStr)
                .tokenType("Bearer")
                .userId(user.getId())
                .email(user.getEmail())
                .roles(roles)
                .organizations(orgs)
                .build();
    }

    @Transactional
    public TokenResponse refresh(RefreshTokenRequest request) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(request.getRefreshToken())
                .orElseThrow(() -> new BadRequestException("Invalid refresh token!"));

        if (refreshToken.isRevoked()) {
            throw new BadRequestException("Refresh token has been revoked!");
        }

        if (refreshToken.getExpiryDate().isBefore(Instant.now())) {
            refreshTokenRepository.delete(refreshToken);
            throw new BadRequestException("Refresh token has expired!");
        }

        User user = refreshToken.getUser();
        List<String> roles = user.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toList());

        String newAccessToken = jwtUtils.generateToken(user.getEmail(), user.getId(), roles);

        List<OrganizationDto> orgs = organizationMemberRepository.findByUserId(user.getId()).stream()
                .map(m -> OrganizationDto.builder()
                        .id(m.getOrganization().getId())
                        .name(m.getOrganization().getName())
                        .slug(m.getOrganization().getSlug())
                        .build())
                .collect(Collectors.toList());

        return TokenResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(refreshToken.getToken())
                .tokenType("Bearer")
                .userId(user.getId())
                .email(user.getEmail())
                .roles(roles)
                .organizations(orgs)
                .build();
    }

    @Transactional
    public void logout(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        refreshTokenRepository.deleteByUserId(user.getId());
    }
}
