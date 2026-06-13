package com.spearhead.ufc.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "employee", schema = "base", uniqueConstraints = {
		@UniqueConstraint(columnNames = { "org_id", "location_id", "employee_code" }),
		@UniqueConstraint(columnNames = { "email_id" }) })
@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
public class Employee {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "employee_id")
	private Integer employeeId;

	@ManyToOne
	@JoinColumn(name = "org_id", nullable = true)
	private Org org;

	@ManyToOne
	@JoinColumn(name = "location_id", nullable = true)
	private Location location;

	@ManyToOne
	@JoinColumn(name = "department_id")
	private Department department;

	@ManyToOne
	@JoinColumn(name = "role_id", nullable = true)
	private Role roleId;

	@Column(name = "password_hash", nullable = false, columnDefinition = "TEXT")
	private String passwordHash;

	@Column(name = "employee_code", nullable = false, length = 50)
	private String employeeCode;

	@Column(name = "username", nullable = false, length = 50, unique = true)
	private String username;

	@Column(name = "first_name", nullable = false, length = 100)
	private String firstName;

	@Column(name = "last_name", length = 100)
	private String lastName;

	@Column(name = "email_id", nullable = false, length = 50)
	private String emailId;

	@Column(name = "phone_number", length = 20)
	private String phoneNumber;

	@Column(name = "is_active")
	private Boolean isActive = true;

	@Column(name = "join_date")
	private LocalDate joinDate;

	@Column(name = "relieve_date")
	private LocalDate relieveDate;

	@Column(name = "created_by")
	private Integer createdBy;

	@Column(name = "created_dt", updatable = false)
	private OffsetDateTime createdDt;

	@Column(name = "updated_by")
	private Integer updatedBy;

	@Column(name = "updated_dt")
	private OffsetDateTime updatedDt;

	@Column(name = "remarks")
	private String remarks;

	@Column(name = "profile_image_file_path", length = 255)
	private String profileImageFilePath;

	@Column(name = "reporting_to")
	private Integer reportingTo;

	@Column(name = "salary", columnDefinition = "NUMERIC(10,2)")
	private java.math.BigDecimal salary;

	@Column(name = "incentive", columnDefinition = "NUMERIC(10,2)")
	private java.math.BigDecimal incentive;

	@Column(name = "special_permission_flag")
	private Boolean specialPermissionFlag = false;

	@Transient
	private String reportingToName;

	@Transient
	private String createdEmployeeName;

	@Transient
	private String updatedEmployeeName;

	@Transient
	private List<EmployeeLocationAccess> employeeLocationAccess;

	 @Column(name = "otp", length = 6)
    private String otp;

    @Column(name = "otp_expiry")
    private LocalDateTime otpExpiry;

    @Column(name = "otp_attempts")
    private Integer otpAttempts = 0;

    @Column(name = "otp_send")
    private Boolean otpSend = false;

}
