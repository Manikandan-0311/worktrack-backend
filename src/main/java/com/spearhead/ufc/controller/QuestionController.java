package com.spearhead.ufc.controller;

import com.spearhead.ufc.dto.RoleAccessDTO;
import com.spearhead.ufc.dto.EmployeeQuestionMappingDTO;
import com.spearhead.ufc.dto.MappingsRequest;
import com.spearhead.ufc.model.JsonResponse;
import com.spearhead.ufc.model.EmployeeQuestionMapping;
import com.spearhead.ufc.model.QuestionBank;
import com.spearhead.ufc.model.RoleQuestionMapping;
import com.spearhead.ufc.repository.EmployeeQuestionMappingRepository;
import com.spearhead.ufc.repository.QuestionBankRepository;
import com.spearhead.ufc.repository.RoleQuestionMappingRepository;
import com.spearhead.ufc.repository.EmployeeRepository;
import lombok.extern.slf4j.Slf4j;
import com.spearhead.ufc.utils.AuthUtil;
import com.spearhead.ufc.model.Employee;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/questionController")
@CrossOrigin(origins = "*")
public class QuestionController {

    @Autowired
    private RoleQuestionMappingRepository roleQuestionMappingRepository;

    @Autowired
    private EmployeeQuestionMappingRepository employeeQuestionMappingRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private QuestionBankRepository questionBankRepository;

    @Autowired
    private AuthUtil authUtil;

    @GetMapping("/getQuestionRoleAccess")
    public JsonResponse getQuestionRoleAccess(@RequestParam Integer questionId) {
        log.info("Entered getQuestionRoleAccess, questionId={}", questionId);
        try {
            if (questionId == null || questionId <= 0) {
                return JsonResponse.of(false, null, "questionId is required");
            }
            List<RoleQuestionMapping> mappings = roleQuestionMappingRepository.findByQuestionIdActive(questionId);
            List<RoleAccessDTO> result = mappings.stream()
                    .filter(m -> m.getRole() != null)
                    .map(m -> new RoleAccessDTO(
                            m.getRole().getRoleId(),
                            m.getRole().getRoleName()
                    ))
                    .collect(Collectors.toList());
            return JsonResponse.of(true, result, "Roles fetched successfully");
        } catch (Exception e) {
            log.error("Error in getQuestionRoleAccess", e);
            return JsonResponse.of(false, null, e.getMessage());
        }
    }

    @PostMapping("/saveQuestionMappings")
    public JsonResponse saveQuestionMappings(@org.springframework.web.bind.annotation.RequestHeader(value = "Authorization", required = false) String authHeader,
            @org.springframework.web.bind.annotation.RequestBody MappingsRequest request) {
        log.info("Entered saveQuestionMappings");
        try {
            if (authHeader == null || authHeader.trim().isEmpty()) {
                log.warn("Missing Authorization header in saveQuestionMappings");
                return JsonResponse.of(false, null, "Authorization header is required. Please provide a valid token.");
            }

            Employee tokenEmployee = authUtil.getEmployeeFromToken(authHeader);
            if (tokenEmployee == null) {
                log.warn("Invalid or expired token in saveQuestionMappings");
                return JsonResponse.of(false, null, "Invalid or expired token. Please login again.");
            }
            if (request == null || request.getMappings() == null || request.getMappings().isEmpty()) {
                return JsonResponse.of(false, null, "mappings array is required");
            }

            java.time.OffsetDateTime now = java.time.OffsetDateTime.now();
            java.util.List<EmployeeQuestionMapping> saved = new java.util.ArrayList<>();

            for (EmployeeQuestionMappingDTO dto : request.getMappings()) {
                if (dto == null || dto.getEmployee() == null || dto.getEmployee().getEmployeeId() == null
                        || dto.getQuestion() == null || dto.getQuestion().getQuestionId() == null) {
                    continue; // skip invalid entries
                }
                Integer empId = dto.getEmployee().getEmployeeId();
                Integer qId = dto.getQuestion().getQuestionId();

                EmployeeQuestionMapping existing = employeeQuestionMappingRepository
                        .findFirstByEmployee_EmployeeIdAndQuestion_QuestionId(empId, qId).orElse(null);

                if (existing != null) {
                    existing.setIsActive(dto.getIsActive() != null ? dto.getIsActive() : true);
                    existing.setUpdatedBy(tokenEmployee.getEmployeeId());
                    existing.setUpdatedDt(now);
                    saved.add(employeeQuestionMappingRepository.save(existing));
                } else {
                    EmployeeQuestionMapping m = new EmployeeQuestionMapping();
                    m.setEmployee(employeeRepository.getReferenceById(empId));
                    m.setQuestion(questionBankRepository.getReferenceById(qId.longValue()));
                    m.setIsActive(dto.getIsActive() != null ? dto.getIsActive() : true);
                    m.setCreatedBy(tokenEmployee.getEmployeeId());
                    m.setUpdatedBy(tokenEmployee.getEmployeeId());
                    m.setCreatedDt(now);
                    m.setUpdatedDt(now);
                    saved.add(employeeQuestionMappingRepository.save(m));
                }
            }

            return JsonResponse.of(true, saved, "Mappings saved successfully");
        } catch (Exception e) {
            log.error("Error in saveQuestionMappings", e);
            return JsonResponse.of(false, null, e.getMessage());
        }
    }
}
