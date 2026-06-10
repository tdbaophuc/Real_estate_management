package com.javaweb.auth.repository;

import com.javaweb.auth.entity.Role;
import com.javaweb.auth.enums.RoleCode;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByCode(RoleCode code);

    @EntityGraph(attributePaths = "permissions")
    Optional<Role> findWithPermissionsByCode(RoleCode code);
}
