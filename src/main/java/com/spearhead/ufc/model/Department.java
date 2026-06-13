package com.spearhead.ufc.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "department", schema = "base", uniqueConstraints = {
		@UniqueConstraint(columnNames = { "org_id", "department_code" }),
		@UniqueConstraint(columnNames = { "org_id", "department_name" }) })
public class Department {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "department_id")
	private Integer departmentId;

	@ManyToOne
	@JoinColumn(name = "org_id", nullable = false)
	private Org org;

	@Column(name = "department_code", nullable = false, length = 20)
	private String departmentCode;

	@Column(name = "department_name", nullable = false, length = 100)
	private String departmentName;

	@Column(name = "description")
	private String description;

	@Column(name = "is_active")
	private Boolean isActive = true;

	@Column(name = "created_by")
	private Integer createdBy;

	@Column(name = "created_dt")
	private OffsetDateTime createdDt;

	@Column(name = "updated_by")
	private Integer updatedBy;

	@Column(name = "updated_dt")
	private OffsetDateTime updatedDt;

}
