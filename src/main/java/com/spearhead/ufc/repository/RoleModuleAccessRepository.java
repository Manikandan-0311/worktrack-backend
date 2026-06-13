package com.spearhead.ufc.repository;

import com.spearhead.ufc.model.RoleModuleAccess;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.spearhead.ufc.model.Role;
import java.util.List;

public interface RoleModuleAccessRepository extends JpaRepository<RoleModuleAccess, Integer> {
    @Query("SELECT rma FROM RoleModuleAccess rma JOIN rma.role r JOIN rma.module m WHERE r.org.orgId = :orgId AND rma.isActive = true AND m.isActive = true")
    List<RoleModuleAccess> findAllByOrgId(@Param("orgId") Integer orgId);

    @Query("SELECT rma FROM RoleModuleAccess rma WHERE rma.role.roleId = :roleId AND rma.isActive = true AND rma.module.isActive = true")
    List<RoleModuleAccess> findActiveByRoleIdAndModuleActive(@Param("roleId") Integer roleId);

    @Query("SELECT rma FROM RoleModuleAccess rma WHERE rma.role.roleId = :roleId AND rma.module.isActive = true")
    List<RoleModuleAccess> findByRoleIdAndModuleActive(@Param("roleId") Integer roleId);

    @Query("SELECT rma FROM RoleModuleAccess rma JOIN rma.role r JOIN rma.module m WHERE rma.isActive = true AND m.isActive = true")
    List<RoleModuleAccess> findAllActiveWithExistingRole();

    @Query("SELECT rma FROM RoleModuleAccess rma WHERE rma.role = :role AND rma.isActive = true")
    List<RoleModuleAccess> findAllByRole(@Param("role") Role role);
}
