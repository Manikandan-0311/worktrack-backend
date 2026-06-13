package com.spearhead.ufc.controller;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.spearhead.ufc.dto.SubmissionAnswerDTO;
import com.spearhead.ufc.model.JsonResponse;
import com.spearhead.ufc.model.QuestionBank;
import com.spearhead.ufc.model.Submission;
import com.spearhead.ufc.model.Employee;
import com.spearhead.ufc.repository.SubmissionRepository;
import com.spearhead.ufc.repository.SubmissionStatusRepository;
import com.spearhead.ufc.utils.AuthUtil;

@RestController
@RequestMapping("/compliance")
public class ComplianceController {

    private static final Logger log = LoggerFactory.getLogger(ComplianceController.class);

    @Autowired
    private AuthUtil authUtil;
    @Autowired
    private SubmissionRepository submissionRepository;
    
    @Autowired
    private ObjectMapper objectMapper;

    @PostMapping("/submit-answer")
    public JsonResponse submitComplianceAnswers(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam(value = "roleId", required = false) Integer roleId,
            @RequestBody Object body) {
        log.info("Entered submitComplianceAnswers roleId={}", roleId);
        try {
            Employee employee = authUtil.getEmployeeFromToken(authHeader);
            if (employee == null) {
                return new JsonResponse(false, null, "Invalid or expired token");
            }
            Integer orgId = employee.getOrg() != null ? employee.getOrg().getOrgId() : null;
            if (orgId == null) {
                return new JsonResponse(false, null, "User does not have an organization assigned");
            }
            // Normalize payload: accept both single object and array of answers
            List<SubmissionAnswerDTO> answers = new ArrayList<>();
            if (body instanceof List<?>) {
                answers = objectMapper.convertValue(body, new TypeReference<List<SubmissionAnswerDTO>>() {
                });
            } else if (body != null) {
                SubmissionAnswerDTO single = objectMapper.convertValue(body, SubmissionAnswerDTO.class);
                if (single != null)
                    answers.add(single);
            }
            if (answers.isEmpty()) {
                return new JsonResponse(false, null, "answers payload is required (object or array)");
            }

            // status column removed; submissions will no longer reference SubmissionStatus

            OffsetDateTime now = OffsetDateTime.now();
            List<Submission> saved = new ArrayList<>();
            for (SubmissionAnswerDTO dto : answers) {
                if (dto.getQuestionId() == null)
                    continue;
                QuestionBank qref = new QuestionBank();
                qref.setQuestionId(dto.getQuestionId());
                Submission s = Submission.builder()
                        .employee(employee)
                        .questionBank(qref)
                        .questionDate(LocalDate.now()) // Replace submissionDate with questionDate
                        .answer(dto.getAnswer())
                        .remarks(dto.getRemarks())
                        .marksAwarded(0)
                        .createdBy(employee.getEmployeeId())
                        .createdDt(now)
                        .build();
                saved.add(submissionRepository.save(s));
            }
            return new JsonResponse(true, saved, "Answers submitted");
        } catch (Exception e) {
            log.error("Error in submitComplianceAnswers: {}", e.getMessage(), e);
            return new JsonResponse(false, null, "Failed to submit answers: " + e.getMessage());
        } finally {
            log.info("Exited submitComplianceAnswers");
        }
    }
}
