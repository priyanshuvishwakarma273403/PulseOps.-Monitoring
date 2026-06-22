package com.pulseops.auth.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "organization_members")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OrganizationMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;

    @Column(name = "invited_at")
    private LocalDateTime invitedAt;

    @Column(name = "joined_at")
    private LocalDateTime joinedAt;

    @Column(nullable = false, length = 50)
    private String status; // INVITED, ACTIVE, SUSPENDED

    @PrePersist
    protected void onCreate() {
        invitedAt = LocalDateTime.now();
        joinedAt = LocalDateTime.now();
        status = "ACTIVE";
    }
}
