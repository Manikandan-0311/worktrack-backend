package com.spearhead.ufc.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.spearhead.ufc.model.Org;

import java.util.List;

@Repository
public interface OrgRepository extends JpaRepository<Org, Integer> {
	List<Org> findByIsActiveTrue();
	
	/**
	 * Find organization by org code
	 * @param orgCode the organization code
	 * @return Org if found, null otherwise
	 */
	Org findByOrgCode(String orgCode);
	
	/**
	 * Check if organization with given code exists
	 * @param orgCode the organization code
	 * @return true if exists, false otherwise
	 */
	boolean existsByOrgCode(String orgCode);
	
	/**
	 * Find organization by org name
	 * @param orgName the organization name
	 * @return Org if found, null otherwise
	 */
	Org findByOrgName(String orgName);
}