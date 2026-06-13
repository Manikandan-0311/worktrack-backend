package com.spearhead.ufc.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.spearhead.ufc.model.Role;

@Repository
public interface RoleRepository extends JpaRepository<Role, Integer> {
	List<Role> findByIsActiveTrue();
	List<Role> findByOrgOrgIdAndIsActiveTrue(Integer orgId);
	List<Role> findByOrgOrgId(Integer orgId);

	boolean existsByOrgOrgIdAndRoleNameIgnoreCase(Integer orgId, String roleName);

	boolean existsByOrgOrgIdAndRoleNameIgnoreCaseAndRoleIdNot(Integer orgId, String roleName, Integer roleId);
	boolean existsByRoleNameIgnoreCase(String roleName);
	Role findByRoleNameIgnoreCase(String roleName);
}
