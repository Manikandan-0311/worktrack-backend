package com.spearhead.ufc.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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
@Table(
        name = "employee_location_access",
        schema = "base",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"employee_id", "location_id"})
        }
)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class EmployeeLocationAccess {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "emp_location_access_id")
    private Integer empLocationAccessId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "org_id", nullable = false)
    private Org org;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id", nullable = false)
    private Location location;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "created_by")
    private Integer createdBy;

    @Column(name = "created_dt", updatable = false)
    private OffsetDateTime createdDt;

    @Column(name = "updated_by")
    private Integer updatedBy;

    @Column(name = "updated_dt")
    private OffsetDateTime updatedDt;
}
