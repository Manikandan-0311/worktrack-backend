/**
 * 
 */
package com.spearhead.ufc.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.spearhead.ufc.model.PermissionStatus;

/**
 * @author manikandan.m repository for the permission status like
 *         pending,approval,rejected
 */

@Repository
public interface PermissionStatusRepository extends JpaRepository<PermissionStatus, Integer> {

}
