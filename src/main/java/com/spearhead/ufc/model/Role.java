package com.spearhead.ufc.model;

import com.fasterxml.jackson.annotation.JsonFormat;
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
@Table(name = "role", schema = "base",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = { "org_id", "role_name" })
})
@com.fasterxml.jackson.annotation.JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "role_id")
    private Integer roleId;


    @Column(name = "role_name", nullable = false, length = 30) 
    private String roleName;

    // roleCode column removed from DB; keep model clean

    @ManyToOne
    @JoinColumn(name = "org_id", nullable = false)
    private Org org;

    @Column(name = "description", length = 30) 
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

    @ManyToOne
    @JoinColumn(name = "location_id", nullable = true)
    private Location location;

    @Transient
    private String updatedEmployeeName;

    @Transient
    private String createdEmployeeName;

}
