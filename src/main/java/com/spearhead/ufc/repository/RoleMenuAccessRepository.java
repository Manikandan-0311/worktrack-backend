package com.spearhead.ufc.repository;

import com.spearhead.ufc.model.RoleMenuAccess;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

@Repository
public interface RoleMenuAccessRepository extends JpaRepository<RoleMenuAccess, Integer> {

    @Query("SELECT rma FROM RoleMenuAccess rma JOIN rma.role r JOIN rma.menu m WHERE r.org.orgId = :orgId AND rma.isActive = true AND m.isActive = true")
    List<RoleMenuAccess> findAllByOrgId(@Param("orgId") Integer orgId);

    @Query("SELECT rma FROM RoleMenuAccess rma WHERE rma.role.roleId = :roleId AND rma.isActive = true AND rma.menu.isActive = true")
    List<RoleMenuAccess> findActiveByRoleIdAndMenuActive(@Param("roleId") Integer roleId);

    @Query("SELECT rma FROM RoleMenuAccess rma WHERE rma.role.roleId = :roleId AND rma.menu.isActive = true")
    List<RoleMenuAccess> findByRoleIdAndMenuActive(@Param("roleId") Integer roleId);

    @Query("SELECT rma FROM RoleMenuAccess rma JOIN rma.role r JOIN rma.menu m WHERE rma.isActive = true AND m.isActive = true")
    List<RoleMenuAccess> findAllActiveWithExistingRole();

    Optional<RoleMenuAccess> findFirstByRoleRoleIdAndMenuMenuIdOrderByRoleMenuAccessIdDesc(Integer roleId, Integer menuId);
}
