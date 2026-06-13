package com.spearhead.ufc.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "user", schema = "base", uniqueConstraints = { @UniqueConstraint(columnNames = { "username" }),
		@UniqueConstraint(columnNames = { "email_id" }) })
@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
public class User {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "user_id")
	private Integer userId;

	@ManyToOne
	@JoinColumn(name = "org_id", nullable = false)
	private Org org;

	@ManyToOne
	@JoinColumn(name = "employee_id", nullable = false)
	private Employee employee;

	@Column(name = "password_hash", nullable = false, columnDefinition = "TEXT")
	private String passwordHash;

	@Column(name = "is_employee")
	private Boolean isEmployee = true;

	@Column(name = "username", nullable = false, length = 50, unique = true)
	private String username;

	@Column(name = "is_active")
	private Boolean isActive = true;

	@ManyToOne
	@JoinColumn(name = "role_id", nullable = false)
	private Role roleId;

	@Column(name = "firebase_access_token", columnDefinition = "TEXT")
	private String firebaseAccessToken;

	@Column(name = "last_login_dt")
	private OffsetDateTime lastLoginDt;

	@Column(name = "last_password_reset")
	private OffsetDateTime lastPasswordReset;

	@Column(name = "created_by")
	private Integer createdBy;

	@Column(name = "created_dt")
	private OffsetDateTime createdDt;

	@Column(name = "updated_by")
	private Integer updatedBy;

	@Column(name = "updated_dt")
	private OffsetDateTime updatedDt;

	@Transient
	private String password;

}
