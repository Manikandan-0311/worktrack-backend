/**
 * Author: Somaskandhan
 */
package com.spearhead.ufc.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;
import java.time.OffsetDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "branch", schema = "base", uniqueConstraints = {
		@UniqueConstraint(columnNames = { "org_id", "branch_code" }),
		@UniqueConstraint(columnNames = { "org_id", "branch_name" }) })
@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
public class Location {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "location_id")
	private Integer locationId;

	@ManyToOne
	@JoinColumn(name = "org_id", nullable = false)
	private Org org;

	@Column(name = "branch_code", nullable = false, length = 20)
	private String locationCode;

	@Column(name = "branch_name", nullable = false, length = 100)
	private String locationName;

	@Column(name = "address")
	private String address;

	@Column(name = "address1")
	private String address1;

	@Column(name = "city")
	private String city;

	@Column(name = "state")
	private String state;

	@Column(name = "zip_code", length = 10)
	private String zipCode;

	@Column(name = "phone_number", length = 20)
	private String phoneNumber;

	@Column(name = "email_id", length = 50)
	private String emailId;

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

	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm[:ss]")
	@Column(name = "maintenance_threshold_start_time")
	private LocalTime maintenanceThresholdStartTime;

	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm[:ss]")
	@Column(name = "maintenance_threshold_end_time")
	private LocalTime maintenanceThresholdEndTime;

	@Column(name ="country", length = 50)
	private String country;

	@Transient
	private String updatedEmployeeName;

	@Transient
	private String createdEmployeeName;
}
