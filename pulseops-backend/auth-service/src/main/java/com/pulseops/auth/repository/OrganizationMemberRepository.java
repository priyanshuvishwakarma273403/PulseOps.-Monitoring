package com.pulseops.auth.repository;

import com.pulseops.auth.entity.OrganizationMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrganizationMemberRepository extends JpaRepository<OrganizationMember, Long> {
    List<OrganizationMember> findByUserId(Long userId);
    Optional<OrganizationMember> findByOrganizationIdAndUserId(Long organizationId, Long userId);
}
