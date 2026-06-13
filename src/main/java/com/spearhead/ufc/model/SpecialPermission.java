/**
 * 
 */
package com.spearhead.ufc.model;

/**
 * 
 */

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.OffsetDateTime;
import java.util.Date;

@Entity
@Table(name = "special_permissions", schema = "compliance", uniqueConstraints = {
		@UniqueConstraint(columnNames = { "employee_id", "permission_date", "permission_type" }) })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SpecialPermission {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "permission_id")
	private Integer permissionId;

	@ManyToOne
	@JoinColumn(name = "employee_id", nullable = false)
	private Employee employee;

	@Column(name = "permission_type", length = 50)
	private String permissionType;

	@Column(name = "permission_date", nullable = false)
	private Date permissionDate;

	@ManyToOne
	@JoinColumn(name = "permission_status_id", nullable = false)
	private PermissionStatus permissionStatus;

	@Column(name = "remarks")
	private String remarks;

	@ManyToOne
	@JoinColumn(name = "granted_by")
	private Employee grantedBy;

	@Column(name = "reason")
	private String reason;

	@Column(name = "created_by")
	private Integer createdBy;

	@Column(name = "created_dt", updatable = false, insertable = false)
	private OffsetDateTime createdDt;

	@Column(name = "updated_by")
	private Integer updatedBy;

	@Column(name = "updated_dt")
	private OffsetDateTime updatedDt;
}
