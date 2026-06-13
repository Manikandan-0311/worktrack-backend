package com.spearhead.ufc.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.spearhead.ufc.model.SubmissionStatus;

@Repository
public interface SubmissionStatusRepository extends JpaRepository<SubmissionStatus, Integer> {
    Optional<SubmissionStatus> findFirstByOrg_OrgIdAndIsActiveTrue(Integer orgId);
}
