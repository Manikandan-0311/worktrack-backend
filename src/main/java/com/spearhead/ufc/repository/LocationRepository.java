package com.spearhead.ufc.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.spearhead.ufc.model.Location;

import java.util.List;

@Repository
public interface LocationRepository extends JpaRepository<Location, Integer> {
	List<Location> findByIsActiveTrue();
	List<Location> findByOrgOrgIdAndIsActiveTrue(Integer orgId);
	List<Location> findByOrgOrgId(Integer orgId);
	
	/**
	 * Find location by organization ID and location code
	 * Used to check for duplicate location codes within an organization
	 * @param orgId the organization ID
	 * @param locationCode the location/branch code
	 * @return Location if found, null otherwise
	 */
	Location findByOrgOrgIdAndLocationCode(Integer orgId, String locationCode);
	
	/**
	 * Find location by organization ID and location name
	 * Used to check for duplicate location names within an organization
	 * @param orgId the organization ID
	 * @param locationName the location/branch name
	 * @return Location if found, null otherwise
	 */
	Location findByOrgOrgIdAndLocationName(Integer orgId, String locationName);
}
