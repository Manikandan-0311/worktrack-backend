package com.spearhead.ufc.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.time.LocalTime;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonFormat;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "org", schema = "base", uniqueConstraints = { @UniqueConstraint(columnNames = "org_code"),
		@UniqueConstraint(columnNames = "org_name") })
@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
public class Org {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "org_id")
	private Integer orgId;

	@Column(name = "org_code", nullable = false, length = 20)
	private String orgCode;

	@Column(name = "org_name", nullable = false, length = 100)
	private String orgName;

	@Column(name = "phone_number", length = 20)
	private String phoneNumber;

	@Column(name = "email_id", length = 50)
	private String emailId;

	@Column(name = "address")
	private String address;

	@Column(name = "address1")
	private String address1;

	@Column(name = "city", length = 50)
	private String city;

	@Column(name = "state", length = 50)
	private String state;

	@Column(name = "country", length = 50)
	private String country;

	@Column(name = "pincode", length = 10)
	private String pincode;

	// @Column(name = "firebase_access_token")
	// private String firebaseAccessToken;

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

	@Column(name ="common_mail")
	private String commonMail;

	@Transient
	private String updatedEmployeeName;

	@Transient
	private String createdEmployeeName;

	@Column(name = "maintenance_threshold_special_permission_dt")
	private Integer maintenanceThresholdSpecialPermissionDt;

	@Column(name="logo")
	private String logo;

}
