/**
 * 
 */
package com.spearhead.ufc.model;

import java.time.OffsetDateTime;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author manikandan.m permission status table for special permission
 */

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "permission_status", schema = "compliance")
public class PermissionStatus {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "permission_status_id")
	private int permissionStatusId;

	@ManyToOne
	@JoinColumn(name = "org_id", nullable = false)
	private Org org;
	
	
	@Column(name = "is_active")
	private Boolean isActive = true;

	@Column(name = "created_by")
	private int createdBy;

	@Column(name = "created_dt")
	private OffsetDateTime createdDt;

	@Column(name = "updated_by")
	private int updatedBy;

	@Column(name = "updated_dt")
	private OffsetDateTime updatedDt;
}
