package com.spearhead.ufc.controller;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import com.spearhead.ufc.dto.EmployeeProgressDTO;
import com.spearhead.ufc.dto.OptionDTO;
import com.spearhead.ufc.dto.QuestionWithOptionsDTO;
import com.spearhead.ufc.dto.SubmissionAnswerDTO;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import com.spearhead.ufc.model.JsonResponse;
import com.spearhead.ufc.model.Option;
import com.spearhead.ufc.model.QuestionBank;
import com.spearhead.ufc.model.RoleQuestionMapping;
import com.spearhead.ufc.model.Submission;
import com.spearhead.ufc.model.Employee;
import com.spearhead.ufc.repository.OptionRepository;
import com.spearhead.ufc.repository.RoleQuestionMappingRepository;
import com.spearhead.ufc.repository.SubmissionRepository;
import com.spearhead.ufc.utils.AuthUtil;
import jakarta.mail.internet.MimeMessage;

@RestController
@RequestMapping("/submission")
public class SubmissionController {

    private static final Logger log = LoggerFactory.getLogger(SubmissionController.class);

    @Autowired
    private AuthUtil authUtil;
    @Autowired
    private RoleQuestionMappingRepository roleQuestionMappingRepository;
    @Autowired
    private OptionRepository optionRepository;
    @Autowired
    private SubmissionRepository submissionRepository;
    // SubmissionStatus removed — status_id column dropped. No repository required.
    @Autowired
    private com.spearhead.ufc.repository.QuestionBankRepository questionBankRepository;
    @Autowired
    private com.spearhead.ufc.repository.EmployeeRepository employeeRepository;
    @Autowired
    private com.spearhead.ufc.repository.AnswerSubmissionTimeExtensionRepository answerSubmissionTimeExtensionRepository;
    @Autowired
    private com.spearhead.ufc.repository.EmployeeQuestionMappingRepository employeeQuestionMappingRepository;
    @Autowired
    private com.spearhead.ufc.repository.CalendarRepository calendarRepository;
    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String configuredFromEmail;

    private Double calculatePerDayIncentive(Employee employee, LocalDate referenceDate) {
        try {
            // Get employee incentive
            java.math.BigDecimal employeeIncentive = employee.getIncentive();
            if (employeeIncentive == null || employeeIncentive.doubleValue() <= 0) {
                return null;
            }

            // Get organization
            Integer orgId = employee.getOrg() != null ? employee.getOrg().getOrgId() : null;
            if (orgId == null) {
                return null;
            }

            // Get current month and year
            YearMonth yearMonth = YearMonth.from(referenceDate);
            int totalDaysInMonth = yearMonth.lengthOfMonth();

            // Get all calendar entries for this organization
            List<com.spearhead.ufc.model.Calendar> calendarEntries = calendarRepository.findAllByOrgOrgId(orgId);

            // Count holidays: only entries with isActive = true are holidays
            int holidayCount = 0;
            for (com.spearhead.ufc.model.Calendar cal : calendarEntries) {
                if (cal.getCalendarDate() != null &&
                        cal.getCalendarDate().getYear() == yearMonth.getYear() &&
                        cal.getCalendarDate().getMonthValue() == yearMonth.getMonthValue() &&
                        Boolean.TRUE.equals(cal.getIsActive())) { // isActive = true means HOLIDAY
                    holidayCount++;
                }
            }

            // Calculate working days: Total days in month - Holiday count
            int workingDays = totalDaysInMonth - holidayCount;
            if (workingDays <= 0) {
                workingDays = 1; // Fallback to avoid division by zero
            }

            // Calculate per-day incentive
            double perDayIncentive = employeeIncentive.doubleValue() / workingDays;

            return perDayIncentive;

        } catch (Exception e) {
            log.warn("Failed to calculate per-day incentive: {}", e.getMessage());
            return null;
        }
    }

    private Double calculateCostPerWeightagePoint(Employee employee, LocalDate referenceDate,
            List<QuestionWithOptionsDTO> questions) {
        try {
            Double perDayIncentive = calculatePerDayIncentive(employee, referenceDate);
            if (perDayIncentive == null || perDayIncentive <= 0 || questions == null || questions.isEmpty()) {
                return null;
            }

            double totalWeightage = questions.stream()
                    .map(QuestionWithOptionsDTO::getWeightage)
                    .filter(w -> w != null && w > 0)
                    .mapToDouble(Double::doubleValue)
                    .sum();

            if (totalWeightage <= 0) {
                return null;
            }

            return perDayIncentive / totalWeightage;
        } catch (Exception e) {
            log.warn("Failed to calculate cost per weightage point: {}", e.getMessage());
            return null;
        }
    }

    private Boolean isHolidayToday(Employee employee, LocalDate referenceDate) {
        try {
            // Get organization
            Integer orgId = employee.getOrg() != null ? employee.getOrg().getOrgId() : null;
            if (orgId == null) {
                return false; // No org, assume working day
            }

            // Get all calendar entries for this organization
            List<com.spearhead.ufc.model.Calendar> calendarEntries = calendarRepository.findAllByOrgOrgId(orgId);

            // Check if the reference date exists in calendar with isActive = true (HOLIDAY)
            for (com.spearhead.ufc.model.Calendar cal : calendarEntries) {
                if (cal.getCalendarDate() != null &&
                        cal.getCalendarDate().isEqual(referenceDate) &&
                        Boolean.TRUE.equals(cal.getIsActive())) { // isActive = true means HOLIDAY
                    return true; // It's a holiday
                }
            }

            return false; // Not a holiday (either date not in calendar or isActive = false)

        } catch (Exception e) {
            log.warn("Failed to check if date is holiday: {}", e.getMessage());
            return false; // Default to working day on error
        }
    }

    @GetMapping("/progress")
    public JsonResponse getEmployeeProgress(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam(value = "date", required = false) String dateStr) {
        try {
            java.time.LocalDate filterDate = null;
            if (dateStr != null && !dateStr.isEmpty()) {
                filterDate = java.time.LocalDate.parse(dateStr);
            }
            Employee user = authUtil.getEmployeeFromToken(authHeader);
            if (user == null) {
                return new JsonResponse(false, null, "Invalid or expired token");
            }
            String roleName = (user.getRoleId() != null) ? user.getRoleId().getRoleName() : null;
            boolean isSuperAdmin = roleName != null && "SUPER_ADMIN".equalsIgnoreCase(roleName);
            Integer orgId = isSuperAdmin ? null : (user.getOrg() != null ? user.getOrg().getOrgId() : null);
            List<Employee> employees = isSuperAdmin
                    ? employeeRepository.findEmployeesByFilters(null, null)
                    : employeeRepository.findEmployeesByFilters(orgId, null);
            List<Object[]> rows = (filterDate != null)
                    ? submissionRepository.getEmployeeProgressByDateNative(java.sql.Date.valueOf(filterDate))
                    : submissionRepository.getEmployeeProgressNative();
            java.util.Map<Integer, Object[]> progressMap = new java.util.HashMap<>();
            for (Object[] row : rows) {
                Integer employeeId = (row[0] != null) ? ((Number) row[0]).intValue() : null;
                if (employeeId != null) {
                    progressMap.put(employeeId, row);
                }
            }
            List<EmployeeProgressDTO> result = new ArrayList<>();
            for (Employee emp : employees) {
                Object[] row = progressMap.get(emp.getEmployeeId());
                String employeeName = (emp.getFirstName() != null ? emp.getFirstName() : "") +
                        (emp.getLastName() != null && !emp.getLastName().isEmpty() ? (" " + emp.getLastName()) : "");
                java.time.LocalDate date = filterDate;
                int totalScore = 0;
                int answeredQuestions = 0;
                int totalQuestions = 0;
                if (row != null) {
                    java.sql.Date dateSql = (java.sql.Date) row[2];
                    date = (dateSql != null) ? dateSql.toLocalDate() : filterDate;
                    totalScore = (row[3] != null) ? ((Number) row[3]).intValue() : 0;
                    answeredQuestions = (row[4] != null) ? ((Number) row[4]).intValue() : 0;
                    totalQuestions = (row[5] != null) ? ((Number) row[5]).intValue() : 0;
                } else {
                    // If not answered, get totalQuestions from role mapping
                    if (emp.getRoleId() != null) {
                        totalQuestions = roleQuestionMappingRepository
                                .findByRoleIdAndIsActiveTrue(emp.getRoleId().getRoleId()).size();
                    }
                }
                result.add(new EmployeeProgressDTO(emp.getEmployeeId(), employeeName.trim(), date, totalScore,
                        totalQuestions, answeredQuestions));
            }
            return new JsonResponse(true, result, "Employee progress fetched successfully");
        } catch (Exception e) {
            log.error("Error in getEmployeeProgress: {}", e.getMessage(), e);
            return new JsonResponse(false, null, "Failed to load employee progress: " + e.getMessage());
        }
    }

    @GetMapping("/today")
    public JsonResponse getTodayQuestions(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam(value = "roleId", required = false) Integer roleId,
            @RequestParam(value = "date", required = false) LocalDate date) {
        log.info("Entered getTodayQuestions (roleId from token)");
        try {
            Employee employee = authUtil.getEmployeeFromToken(authHeader);
            if (employee == null) {
                return new JsonResponse(false, null, "Invalid or expired token");
            }

            // Check if employee's organization is active

            LocalDate today = (date != null) ? date : LocalDate.now();
            boolean hasSpecialPermission = !answerSubmissionTimeExtensionRepository
                    .findByEmployee_EmployeeIdAndQuestionDateAndIsActiveTrue(employee.getEmployeeId(), today)
                    .isEmpty();

            String roleName = (employee.getRoleId() != null) ? employee.getRoleId().getRoleName() : null;
            boolean isSuperAdmin = roleName != null && "SUPER_ADMIN".equalsIgnoreCase(roleName);

            List<QuestionWithOptionsDTO> result = new ArrayList<>();

            if (isSuperAdmin) {
                // For SUPER_ADMIN: fetch all active questions based on date range only
                List<QuestionBank> allQuestions = questionBankRepository.findByIsActiveTrue();
                for (QuestionBank question : allQuestions) {
                    // Filter to only include questions from active organizations
                    if (question.getOrg() == null || question.getOrg().getIsActive() == null
                            || !question.getOrg().getIsActive()) {
                        continue;
                    }

                    LocalDate from = question.getFromDate();
                    LocalDate to = question.getToDate();
                    boolean show = (from == null || !today.isBefore(from)) && (to == null || !today.isAfter(to));

                    if (!show) {
                        continue;
                    }

                    List<Option> options = optionRepository
                            .findByQuestion_QuestionIdAndIsActiveTrue(question.getQuestionId());
                    List<OptionDTO> optionDTOs = options.stream()
                            .map(o -> new OptionDTO(o.getOptionId(), o.getOptionValue()))
                            .collect(Collectors.toList());
                    String existingAnswer = null;
                    boolean submittedToday = false;
                    try {
                        Submission latest = submissionRepository
                                .findFirstByEmployee_EmployeeIdAndQuestionBank_QuestionIdOrderByCreatedDtDesc(
                                        employee.getEmployeeId(), question.getQuestionId());
                        if (latest != null && latest.getQuestionDate() != null
                                && latest.getQuestionDate().isEqual(today)) {
                            existingAnswer = latest.getAnswer();
                            submittedToday = true;
                        }
                    } catch (Exception ex) {
                    }

                    Double costPerQuestion = null;
                    QuestionWithOptionsDTO questionDto = new QuestionWithOptionsDTO(
                            question.getQuestionId(),
                            question.getQuestionText(),
                            question.getQuestionType(),
                            question.getWeightage(),
                            question.getFromDate(),
                            question.getToDate(),
                            optionDTOs,
                            existingAnswer,
                            submittedToday,
                            true,
                            true,
                                costPerQuestion);
                            questionDto.setReasonFlag(question.getReasonFlag());
                            questionDto.setValidAnswer(question.getValidAnswer());
                            result.add(questionDto);
                }
            } else {
                List<RoleQuestionMapping> mappings = roleQuestionMappingRepository.findByRoleIdAndIsActiveTrue(
                        roleId != null ? roleId : employee.getRoleId().getRoleId());

                if (employee.getOrg().getIsActive() == null || !employee.getOrg().getIsActive()) {
                    log.warn("Employee {} attempted to access questions from inactive organization",
                            employee.getEmployeeId());
                    return new JsonResponse(false, null, "Your organization is inactive. You cannot access questions.");
                }
                // Track questions we've already added to avoid duplicates
                java.util.Set<Integer> seenQuestions = new java.util.HashSet<>();

                // First add role-based questions
                for (RoleQuestionMapping mapping : mappings) {
                    QuestionBank question = mapping.getQuestion();
                    if (question == null || Boolean.FALSE.equals(question.getIsActive())) {
                        continue;
                    }

                    // Filter to only include questions from active organizations
                    if (mapping.getOrg() == null || mapping.getOrg().getIsActive() == null
                            || !mapping.getOrg().getIsActive()) {
                        continue;
                    }

                    Integer qId = question.getQuestionId();
                    if (qId == null || seenQuestions.contains(qId)) {
                        continue;
                    }

                    LocalDate from = question.getFromDate();
                    LocalDate to = question.getToDate();
                    boolean show = (from == null || !today.isBefore(from)) && (to == null || !today.isAfter(to));

                    if (!show) {
                        continue;
                    }

                    List<Option> options = optionRepository
                            .findByQuestion_QuestionIdAndIsActiveTrue(question.getQuestionId());
                    List<OptionDTO> optionDTOs = options.stream()
                            .map(o -> new OptionDTO(o.getOptionId(), o.getOptionValue()))
                            .collect(Collectors.toList());
                    String existingAnswer = null;
                    boolean submittedToday = false;
                    try {
                        Submission latest = submissionRepository
                                .findFirstByEmployee_EmployeeIdAndQuestionBank_QuestionIdOrderByCreatedDtDesc(
                                        employee.getEmployeeId(), question.getQuestionId());
                        if (latest != null && latest.getQuestionDate() != null
                                && latest.getQuestionDate().isEqual(today)) {
                            existingAnswer = latest.getAnswer();
                            submittedToday = true;
                        }
                    } catch (Exception ex) {
                    }

                    Double costPerQuestion = null;
                    QuestionWithOptionsDTO questionDto = new QuestionWithOptionsDTO(
                            question.getQuestionId(),
                            question.getQuestionText(),
                            question.getQuestionType(),
                            question.getWeightage(),
                            question.getFromDate(),
                            question.getToDate(),
                            optionDTOs,
                            existingAnswer,
                            submittedToday,
                            true,
                            true,
                                costPerQuestion);
                            questionDto.setReasonFlag(question.getReasonFlag());
                            questionDto.setValidAnswer(question.getValidAnswer());
                            result.add(questionDto);

                    seenQuestions.add(qId);
                }

                try {
                    java.util.List<com.spearhead.ufc.model.EmployeeQuestionMapping> empMappings = employeeQuestionMappingRepository
                            .findByEmployee_EmployeeIdAndIsActiveTrue(
                                    employee.getEmployeeId());
                    for (com.spearhead.ufc.model.EmployeeQuestionMapping empMap : empMappings) {
                        com.spearhead.ufc.model.QuestionBank question = empMap.getQuestion();
                        if (question == null || Boolean.FALSE.equals(question.getIsActive())) {
                            continue;
                        }

                        // Filter to only include questions from active organizations
                        if (question.getOrg() == null || question.getOrg().getIsActive() == null
                                || !question.getOrg().getIsActive()) {
                            continue;
                        }

                        Integer qId = question.getQuestionId();
                        if (qId == null || seenQuestions.contains(qId)) {
                            continue;
                        }

                        LocalDate from = question.getFromDate();
                        LocalDate to = question.getToDate();
                        boolean show = (from == null || !today.isBefore(from)) && (to == null || !today.isAfter(to));
                        if (!show) {
                            continue;
                        }

                        List<Option> options = optionRepository
                                .findByQuestion_QuestionIdAndIsActiveTrue(question.getQuestionId());
                        List<OptionDTO> optionDTOs = options.stream()
                                .map(o -> new OptionDTO(o.getOptionId(), o.getOptionValue()))
                                .collect(Collectors.toList());
                        String existingAnswer = null;
                        boolean submittedToday = false;
                        try {
                            Submission latest = submissionRepository
                                    .findFirstByEmployee_EmployeeIdAndQuestionBank_QuestionIdOrderByCreatedDtDesc(
                                            employee.getEmployeeId(), question.getQuestionId());
                            if (latest != null && latest.getQuestionDate() != null
                                    && latest.getQuestionDate().isEqual(today)) {
                                existingAnswer = latest.getAnswer();
                                submittedToday = true;
                            }
                        } catch (Exception ex) {
                        }

                        Double costPerQuestion = null;
                        QuestionWithOptionsDTO questionDto = new QuestionWithOptionsDTO(
                                question.getQuestionId(),
                                question.getQuestionText(),
                                question.getQuestionType(),
                                question.getWeightage(),
                                question.getFromDate(),
                                question.getToDate(),
                                optionDTOs,
                                existingAnswer,
                                submittedToday,
                                true,
                                true,
                                costPerQuestion);
                            questionDto.setReasonFlag(question.getReasonFlag());
                            questionDto.setValidAnswer(question.getValidAnswer());
                            result.add(questionDto);

                        seenQuestions.add(qId);
                    }
                } catch (Exception ex) {
                    log.warn("Failed to load employee-specific question mappings: {}", ex.getMessage());
                }
            }
            java.util.Map<String, Object> response = new java.util.HashMap<>();

            // Cost calculation rule:
            // daily incentive = employee incentive / org working days in month
            // cost per weightage point = daily incentive / total daily question weightage
            Double perDayIncentive = calculatePerDayIncentive(employee, today);
            Double costPerWeightagePoint = calculateCostPerWeightagePoint(employee, today, result);
            double totalDailyWeightage = result.stream()
                    .map(QuestionWithOptionsDTO::getWeightage)
                    .filter(w -> w != null && w > 0)
                    .mapToDouble(Double::doubleValue)
                    .sum();
            if (costPerWeightagePoint != null) {
                for (QuestionWithOptionsDTO dto : result) {
                    Double weightage = dto.getWeightage();
                    if (weightage == null || weightage <= 0) {
                        dto.setCost(0.0);
                        continue;
                    }
                    java.math.BigDecimal questionCost = java.math.BigDecimal
                            .valueOf(weightage)
                            .multiply(java.math.BigDecimal.valueOf(costPerWeightagePoint))
                            .setScale(2, java.math.RoundingMode.HALF_UP);
                    dto.setCost(questionCost.doubleValue());
                }
            }

            // Check if today is a holiday
            Boolean isHoliday = isHolidayToday(employee, today);

            response.put("questions", result);
            response.put("hasSpecialPermission", hasSpecialPermission);
            response.put("isHoliday", isHoliday);
                response.put("perDayIncentive", perDayIncentive);
                response.put("totalDailyWeightage", totalDailyWeightage);
                response.put("costPerWeightagePoint", costPerWeightagePoint);
            response.put("maintenancethersoldtime",
                    employee.getLocation() != null ? employee.getLocation().getMaintenanceThresholdEndTime().toString()
                            : null);

            return new JsonResponse(true, response, "Questions fetched successfully");
        } catch (Exception e) {
            log.error("Error in getTodayQuestions: {}", e.getMessage(), e);
            return new JsonResponse(false, null, "Failed to fetch questions: " + e.getMessage());
        } finally {
            log.info("Exited getTodayQuestions");
        }
    }

    @PostMapping("/answers")
    public JsonResponse submitAnswers(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam Integer roleId,
            @RequestBody List<SubmissionAnswerDTO> answers) {
        log.info("Entered submitAnswers with payload size={} roleId={}", answers != null ? answers.size() : 0, roleId);
        try {
            Employee employee = authUtil.getEmployeeFromToken(authHeader);
            if (employee == null) {
                return new JsonResponse(false, null, "Invalid or expired token");
            }

            if (answers == null || answers.isEmpty()) {
                return new JsonResponse(false, null, "answers array is required");
            }

            // SubmissionStatus status = submissionStatusRepository
            // .findFirstByOrg_OrgIdAndIsActiveTrue(orgId)
            // .orElse(null);
            // if (status == null) {
            // return new JsonResponse(false, null, "No active submission status configured
            // for org");
            // }

            OffsetDateTime now = OffsetDateTime.now();
            List<QuestionWithOptionsDTO> result = new ArrayList<>();
            for (SubmissionAnswerDTO dto : answers) {
                if (dto.getQuestionId() == null)
                    continue;
                QuestionBank qref = new QuestionBank();
                qref.setQuestionId(dto.getQuestionId());
                // Attempt to find an existing submission for same employee/question/date
                java.time.LocalDate questionDate = dto.getQuestionDate() != null ? dto.getQuestionDate()
                        : java.time.LocalDate.now();

                java.util.Optional<Submission> existingOpt = submissionRepository
                        .findByEmployee_EmployeeIdAndQuestionBank_QuestionIdAndQuestionDate(employee.getEmployeeId(),
                                dto.getQuestionId(), questionDate);

                if (existingOpt.isPresent()) {
                    Submission existing = existingOpt.get();
                    existing.setAnswer(dto.getAnswer());
                    existing.setRemarks(dto.getRemarks());
                    existing.setMarksAwarded(dto.getWeightage() != null ? dto.getWeightage().intValue() : 0);
                    existing.setUpdatedBy(employee.getEmployeeId());
                    existing.setUpdatedDt(now);
                    submissionRepository.save(existing);

                    // If employee worked on a holiday, create a special permission time extension
                    // request
                    if (dto.getIsHoliday() == true) {
                        createHolidayTimeExtensionRequest(employee, questionDate, now);
                    }
                } else {
                    Submission s = Submission.builder()
                            .employee(employee)
                            .questionBank(qref)
                            .questionDate(questionDate)
                            .answer(dto.getAnswer())
                            .remarks(dto.getRemarks())
                            .isActive(dto.getIsHoliday() == false ? true : false)
                            .marksAwarded(dto.getWeightage() != null ? dto.getWeightage().intValue() : 0)
                            .createdBy(employee.getEmployeeId())
                            .createdDt(now)
                            .build();
                    try {
                        submissionRepository.save(s);

                        // If employee worked on a holiday, create a special permission time extension
                        // request
                        if (dto.getIsHoliday() == true) {
                            createHolidayTimeExtensionRequest(employee, questionDate, now);
                        }
                    } catch (org.springframework.dao.DataIntegrityViolationException dive) {
                        java.util.Optional<Submission> retry = submissionRepository
                                .findByEmployee_EmployeeIdAndQuestionBank_QuestionIdAndQuestionDate(
                                        employee.getEmployeeId(), dto.getQuestionId(), questionDate);
                        if (retry.isPresent()) {
                            Submission existing = retry.get();
                            existing.setAnswer(dto.getAnswer());
                            existing.setRemarks(dto.getRemarks());
                            existing.setMarksAwarded(dto.getWeightage() != null ? dto.getWeightage().intValue() : 0);
                            existing.setUpdatedBy(employee.getEmployeeId());
                            existing.setUpdatedDt(now);
                            submissionRepository.save(existing);
                        } else {
                            throw dive; // rethrow if still failing
                        }
                    }
                }
                QuestionBank q = null;
                try {
                    java.util.Optional<QuestionBank> qOpt = questionBankRepository
                            .findById(Long.valueOf(dto.getQuestionId()));
                    if (qOpt.isPresent()) {
                        q = qOpt.get();
                    }
                } catch (Exception ex) {
                }
                if (q == null) {
                    // fallback: only return questionId and answer
                    result.add(new QuestionWithOptionsDTO(dto.getQuestionId(), null, null, null, null, null,
                            new ArrayList<>(), dto.getAnswer(), true, true, true, null));
                    continue;
                }
                List<Option> options = optionRepository.findByQuestion_QuestionIdAndIsActiveTrue(q.getQuestionId());
                List<OptionDTO> optionDTOs = options.stream()
                        .map(o -> new OptionDTO(o.getOptionId(), o.getOptionValue()))
                        .collect(Collectors.toList());
                result.add(new QuestionWithOptionsDTO(
                        q.getQuestionId(), q.getQuestionText(), q.getQuestionType(), q.getWeightage(),
                        q.getFromDate(), q.getToDate(), optionDTOs, dto.getAnswer(), true, true, true, null));
            }

            try {
                LocalDate reportDate = answers.stream()
                        .map(SubmissionAnswerDTO::getQuestionDate)
                        .filter(java.util.Objects::nonNull)
                        .findFirst()
                        .orElse(LocalDate.now());
                String toEmail = resolveRecipientEmail(employee);
                Path reportPath = generateDailyComplianceExcel(employee, answers, reportDate);
                sendDailyComplianceEmail(employee, toEmail, reportPath);
            } catch (Exception ex) {
                log.warn("Failed to generate/send daily compliance email: {}", ex.getMessage(), ex);
            }
            return new JsonResponse(true, result, "Answers submitted");
        } catch (Exception e) {
            log.error("Error in submitAnswers: {}", e.getMessage(), e);
            return new JsonResponse(false, null, "Failed to submit answers: " + e.getMessage());
        } finally {
            log.info("Exited submitAnswers");
        }
    }

    private String resolveRecipientEmail(Employee employee) {
        String toEmail = null;
        try {
            Integer reportingId = employee.getReportingTo();
            if (reportingId != null) {
                Employee manager = employeeRepository.findById(reportingId).orElse(null);
                if (manager != null && manager.getEmailId() != null && !manager.getEmailId().isBlank()) {
                    toEmail = manager.getEmailId().trim();
                }
            }
        } catch (Exception ignore) {
            log.warn("Manager lookup failed for employee {}: {}", employee.getEmployeeId(), ignore.getMessage());
        }

        if (toEmail == null || toEmail.isBlank()) {
            toEmail = employee.getEmailId();
        }

        return toEmail != null ? toEmail.trim() : null;
    }

    private Path generateDailyComplianceExcel(Employee employee, List<SubmissionAnswerDTO> answers, LocalDate reportDate)
            throws java.io.IOException {
        Path employeeFolder = Paths.get("reports", "daily-compliance", String.valueOf(employee.getEmployeeId()));
        Files.createDirectories(employeeFolder);

        String fileName = "Daily_Compliance_" + reportDate.format(DateTimeFormatter.BASIC_ISO_DATE)
                + "_" + System.currentTimeMillis() + ".xlsx";
        Path reportPath = employeeFolder.resolve(fileName);

        String orgName = employee.getOrg() != null && employee.getOrg().getOrgName() != null ? employee.getOrg().getOrgName()
                : "UFC";
        String employeeName = ((employee.getFirstName() != null ? employee.getFirstName() : "")
                + (employee.getLastName() != null && !employee.getLastName().isBlank() ? " " + employee.getLastName() : ""))
                .trim();

        DateTimeFormatter displayDate = DateTimeFormatter.ofPattern("dd-MM-yyyy");

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Daily Compliance");

            Font titleFont = workbook.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 12);

            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());

            CellStyle titleStyle = workbook.createCellStyle();
            titleStyle.setAlignment(HorizontalAlignment.CENTER);
            titleStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            titleStyle.setFont(titleFont);

            CellStyle tableHeaderStyle = workbook.createCellStyle();
            tableHeaderStyle.setFont(headerFont);
            tableHeaderStyle.setFillForegroundColor(IndexedColors.BLUE.getIndex());
            tableHeaderStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            tableHeaderStyle.setAlignment(HorizontalAlignment.CENTER);
            tableHeaderStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            tableHeaderStyle.setBorderBottom(BorderStyle.THIN);
            tableHeaderStyle.setBorderTop(BorderStyle.THIN);
            tableHeaderStyle.setBorderLeft(BorderStyle.THIN);
            tableHeaderStyle.setBorderRight(BorderStyle.THIN);

            CellStyle dataStyle = workbook.createCellStyle();
            dataStyle.setWrapText(true);
            dataStyle.setVerticalAlignment(VerticalAlignment.TOP);
            dataStyle.setBorderBottom(BorderStyle.THIN);
            dataStyle.setBorderTop(BorderStyle.THIN);
            dataStyle.setBorderLeft(BorderStyle.THIN);
            dataStyle.setBorderRight(BorderStyle.THIN);

            Row row0 = sheet.createRow(0);
            Cell orgCell = row0.createCell(0);
            orgCell.setCellValue(orgName);
            orgCell.setCellStyle(titleStyle);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 4));

            Row row1 = sheet.createRow(1);
            Cell reportCell = row1.createCell(0);
            reportCell.setCellValue("Report: Daily Compliance");
            reportCell.setCellStyle(titleStyle);
            sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, 4));

            Row row2 = sheet.createRow(2);
            Cell staffNameCell = row2.createCell(0);
            staffNameCell.setCellValue("Staff Name: " + employeeName);
            staffNameCell.setCellStyle(titleStyle);
            sheet.addMergedRegion(new CellRangeAddress(2, 2, 0, 4));

            Row row3 = sheet.createRow(3);
            Cell dateCell = row3.createCell(0);
            dateCell.setCellValue("Compliance Date: " + reportDate.format(displayDate));
            dateCell.setCellStyle(titleStyle);
            sheet.addMergedRegion(new CellRangeAddress(3, 3, 0, 4));

            Row headerRow = sheet.createRow(6);
            String[] headers = { "S.No", "Question", "Answer", "Reason", "Weightage" };
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(tableHeaderStyle);
            }

            java.util.Map<Integer, String> questionTextCache = new java.util.HashMap<>();
            int rowIndex = 7;
            int serialNo = 1;
            for (SubmissionAnswerDTO dto : answers) {
                Row dataRow = sheet.createRow(rowIndex++);

                String questionText = dto.getQuestionText();
                if ((questionText == null || questionText.isBlank()) && dto.getQuestionId() != null) {
                    questionText = questionTextCache.computeIfAbsent(dto.getQuestionId(), qId -> {
                        try {
                            return questionBankRepository.findById(Long.valueOf(qId))
                                    .map(QuestionBank::getQuestionText)
                                    .orElse("");
                        } catch (Exception ex) {
                            return "";
                        }
                    });
                }

                Cell c0 = dataRow.createCell(0);
                c0.setCellValue(serialNo++);
                c0.setCellStyle(dataStyle);

                Cell c1 = dataRow.createCell(1);
                c1.setCellValue(questionText != null ? questionText : "");
                c1.setCellStyle(dataStyle);

                Cell c2 = dataRow.createCell(2);
                c2.setCellValue(dto.getAnswer() != null ? dto.getAnswer() : "");
                c2.setCellStyle(dataStyle);

                Cell c3 = dataRow.createCell(3);
                c3.setCellValue(dto.getRemarks() != null && !dto.getRemarks().isBlank() ? dto.getRemarks() : "-");
                c3.setCellStyle(dataStyle);

                Cell c4 = dataRow.createCell(4);
                c4.setCellValue(dto.getWeightage() != null ? dto.getWeightage() : 0);
                c4.setCellStyle(dataStyle);
            }

            sheet.setColumnWidth(0, 3500);
            sheet.setColumnWidth(1, 30000);
            sheet.setColumnWidth(2, 15000);
            sheet.setColumnWidth(3, 15000);
            sheet.setColumnWidth(4, 5000);

            try (FileOutputStream out = new FileOutputStream(reportPath.toFile())) {
                workbook.write(out);
            }
        }

        return reportPath;
    }

    private void sendDailyComplianceEmail(Employee employee, String toEmail, Path reportPath) {
        if (toEmail == null || toEmail.isBlank()) {
            log.warn("Skipping daily compliance email because recipient email is empty for employee {}",
                    employee.getEmployeeId());
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            if (configuredFromEmail != null && !configuredFromEmail.isBlank()) {
                helper.setFrom(configuredFromEmail.trim());
            }
            helper.setTo(toEmail.trim());

            String commonMail = employee.getOrg() != null ? employee.getOrg().getCommonMail() : null;
            if (commonMail != null && !commonMail.isBlank()) {
                helper.setCc(commonMail.trim());
            }

            if (employee.getEmailId() != null && !employee.getEmailId().isBlank()) {
                helper.setReplyTo(employee.getEmailId().trim());
            }

            helper.setSubject("UFC — Daily Compliance Submission Report");
            helper.setText(
                    "Dear User,\n\n"
                            + "The Daily Compliance for today has been successfully submitted. Please find the attached Excel file containing the compliance questionnaire along with the responses provided by the staff.\n\n"
                            + "Kindly review the attached report for your reference and records. If you notice any discrepancies or require corrections, please contact the concerned team.\n\n"
                            + "This is an auto-generated email sent for Daily Compliance reporting purposes only. Please do not reply to this email.\n\n"
                            + "Best regards,\n"
                            + "UFC Support Team",
                    false);
            helper.addAttachment(reportPath.getFileName().toString(), reportPath.toFile());

            mailSender.send(message);
            log.info("Daily compliance email sent for employee {} to {} with attachment {}",
                    employee.getEmployeeId(), toEmail, reportPath);
        } catch (Exception ex) {
            throw new RuntimeException("Unable to send daily compliance email", ex);
        }
    }

    @GetMapping("/employee-history")
    public JsonResponse getEmployeeQuestionHistory(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam("employeeId") Integer employeeId,
            @RequestParam("date") String dateStr) {
        try {
            Employee admin = authUtil.getEmployeeFromToken(authHeader);
            if (admin == null) {
                return new JsonResponse(false, null, "Invalid or expired token");
            }
            Employee employee = employeeRepository.findById(employeeId).orElse(null);
            if (employee == null) {
                return new JsonResponse(false, null, "Employee not found");
            }
            java.time.LocalDate selectedDate;
            try {
                selectedDate = java.time.LocalDate.parse(dateStr);
            } catch (Exception ex) {
                return new JsonResponse(false, null, "Invalid date format. Use YYYY-MM-DD");
            }

            // Fetch all active submissions for this employee on the selected question_date at once
            List<Submission> daySubmissions = submissionRepository
                    .findByEmployee_EmployeeIdAndQuestionDateGreaterThanEqualAndQuestionDateLessThanAndIsActiveTrue(
                            employeeId, selectedDate, selectedDate.plusDays(1));
            java.util.Map<Integer, Submission> submissionByQuestionId = new java.util.HashMap<>();
            if (daySubmissions != null) {
                for (Submission s : daySubmissions) {
                    if (s.getQuestionBank() != null) {
                        submissionByQuestionId.put(s.getQuestionBank().getQuestionId(), s);
                    }
                }
            }

            // Org working days for this month
            YearMonth ym = YearMonth.from(selectedDate);
            Integer orgId = employee.getOrg() != null ? employee.getOrg().getOrgId() : null;
            int orgWorkingDays = ym.lengthOfMonth();
            if (orgId != null) {
                try {
                    LocalDate mStart = ym.atDay(1);
                    LocalDate mEnd = mStart.plusMonths(1);
                    long wDays = calendarRepository.countWorkingDays(orgId, mStart, mEnd);
                    orgWorkingDays = (int) Math.max(1, Math.min(wDays, ym.lengthOfMonth()));
                } catch (Exception ex) {
                    log.warn("Could not fetch working days for orgId={}: {}", orgId, ex.getMessage());
                }
            }

            java.math.BigDecimal totalEmpIncentive = (employee.getIncentive() != null)
                    ? employee.getIncentive() : java.math.BigDecimal.ZERO;
            java.math.BigDecimal perDayIncentiveBD = (orgWorkingDays > 0
                    && totalEmpIncentive.compareTo(java.math.BigDecimal.ZERO) > 0)
                            ? totalEmpIncentive.divide(java.math.BigDecimal.valueOf(orgWorkingDays), 4,
                                    java.math.RoundingMode.HALF_UP)
                            : java.math.BigDecimal.ZERO;

            // option value → ID cache for resolving radio/multiselect answers
            java.util.Map<Integer, java.util.Map<String, Integer>> optionValueToIdCache = new java.util.HashMap<>();

            Integer roleId = (employee.getRoleId() != null) ? employee.getRoleId().getRoleId() : null;
            List<RoleQuestionMapping> mappings = roleQuestionMappingRepository.findByRoleIdAndIsActiveTrue(roleId);

            double dayTotalWeightage = 0.0;
            double dayValidWeightage = 0.0;

            List<com.spearhead.ufc.dto.EmployeeQuestionHistoryDTO> result = new ArrayList<>();
            for (RoleQuestionMapping m : mappings) {
                QuestionBank q = m.getQuestion();
                if (q == null || Boolean.FALSE.equals(q.getIsActive()))
                    continue;
                java.time.LocalDate from = q.getFromDate();
                java.time.LocalDate to = q.getToDate();
                java.time.LocalDate created = (q.getCreatedDt() != null) ? q.getCreatedDt().toLocalDate() : null;
                boolean show;
                if (from != null && to != null) {
                    show = !selectedDate.isBefore(from) && !selectedDate.isAfter(to);
                } else if (from != null) {
                    show = !selectedDate.isBefore(from);
                } else if (to != null) {
                    show = (created != null) && selectedDate.isEqual(created);
                } else {
                    show = true;
                }
                if (!show)
                    continue;

                List<Option> options = optionRepository.findByQuestion_QuestionIdAndIsActiveTrue(q.getQuestionId());
                List<OptionDTO> optionDTOs = options.stream()
                        .map(o -> new OptionDTO(o.getOptionId(), o.getOptionValue()))
                        .collect(Collectors.toList());

                // Look up submission by question_date (not created_dt)
                Submission sub = submissionByQuestionId.get(q.getQuestionId());
                String answer = null;
                boolean submittedOnDate = sub != null;
                int marksAwarded = 0;
                String remarks = "";
                double validWeightage = 0.0;
                boolean isValid = false;

                if (sub != null) {
                    answer = sub.getAnswer();
                    marksAwarded = sub.getMarksAwarded();
                    remarks = sub.getRemarks() != null ? sub.getRemarks() : "";

                    String questionType = q.getQuestionType() != null
                            ? q.getQuestionType().trim().toLowerCase() : "";
                    Double weightage = q.getWeightage();

                    if (weightage != null && weightage > 0) {
                        if ("radio".equals(questionType) || "multiselect".equals(questionType)) {
                            // NULL valid_answer → skip entirely (no total, no valid)
                            java.util.Set<Integer> validAnswerIds = historyParseIds(q.getValidAnswer());
                            if (!validAnswerIds.isEmpty()) {
                                dayTotalWeightage += weightage;
                                java.util.Set<Integer> submittedIds = historyResolveAnswerIds(
                                        answer, q.getQuestionId(), optionValueToIdCache);
                                if (!java.util.Collections.disjoint(submittedIds, validAnswerIds)) {
                                    dayValidWeightage += weightage;
                                    validWeightage = weightage;
                                    isValid = true;
                                }
                            }
                        } else {
                            // text/number/other: always in total; valid if answer non-empty
                            dayTotalWeightage += weightage;
                            if (answer != null && !answer.isBlank()) {
                                dayValidWeightage += weightage;
                                validWeightage = weightage;
                                isValid = true;
                            }
                        }
                    }
                }

                String employeeFullName = (employee.getFirstName() != null ? employee.getFirstName() : "")
                        + (employee.getLastName() != null && !employee.getLastName().isEmpty()
                                ? (" " + employee.getLastName())
                                : "");
                com.spearhead.ufc.dto.EmployeeQuestionHistoryDTO dto =
                        new com.spearhead.ufc.dto.EmployeeQuestionHistoryDTO(
                                employee.getEmployeeId(),
                                employeeFullName.trim(),
                                selectedDate,
                                q.getQuestionId(),
                                q.getQuestionText(),
                                q.getQuestionType(),
                                q.getWeightage(),
                                q.getFromDate(),
                                q.getToDate(),
                                optionDTOs,
                                answer,
                                submittedOnDate,
                                marksAwarded,
                                remarks);
                dto.setValidWeightage(validWeightage);
                dto.setIsValid(isValid);
                dto.setValidAnswer(q.getValidAnswer());
                result.add(dto);
            }

            // Day-level incentive calculation
            java.math.BigDecimal dayIncentiveAmount = java.math.BigDecimal.ZERO;
            if (dayTotalWeightage > 0) {
                java.math.BigDecimal costPerWeightage = perDayIncentiveBD.divide(
                        java.math.BigDecimal.valueOf(dayTotalWeightage), 8,
                        java.math.RoundingMode.HALF_UP);
                dayIncentiveAmount = costPerWeightage
                        .multiply(java.math.BigDecimal.valueOf(dayValidWeightage));
            }

            java.util.Map<String, Object> data = new java.util.HashMap<>();
            data.put("questions", result);
            data.put("perDayIncentive",
                    perDayIncentiveBD.setScale(2, java.math.RoundingMode.HALF_UP).doubleValue());
            data.put("incentiveAmount", (int) Math.round(dayIncentiveAmount.doubleValue()));
            data.put("totalWeightage", java.math.BigDecimal.valueOf(dayTotalWeightage)
                    .setScale(2, java.math.RoundingMode.HALF_UP).doubleValue());
            data.put("validWeightage", java.math.BigDecimal.valueOf(dayValidWeightage)
                    .setScale(2, java.math.RoundingMode.HALF_UP).doubleValue());
            data.put("organizationWorkingDays", orgWorkingDays);

            return new JsonResponse(true, data, "Employee question history fetched successfully");
        } catch (Exception e) {
            log.error("Error in getEmployeeQuestionHistory: {}", e.getMessage(), e);
            return new JsonResponse(false, null, "Failed to load employee question history: " + e.getMessage());
        }
    }

    private java.util.Set<Integer> historyParseIds(String csvIds) {
        java.util.Set<Integer> ids = new java.util.HashSet<>();
        if (csvIds == null || csvIds.isBlank()) return ids;
        for (String part : csvIds.split(",")) {
            if (part == null) continue;
            String trimmed = part.trim();
            if (trimmed.matches("\\d+")) {
                ids.add(Integer.valueOf(trimmed));
            }
        }
        return ids;
    }

    private java.util.Set<Integer> historyResolveAnswerIds(String rawAnswer, Integer questionId,
            java.util.Map<Integer, java.util.Map<String, Integer>> optionValueToIdCache) {
        java.util.Set<Integer> ids = new java.util.HashSet<>();
        if (rawAnswer == null || rawAnswer.isBlank()) return ids;
        java.util.Map<String, Integer> valueToId = optionValueToIdCache.computeIfAbsent(questionId, qId -> {
            java.util.Map<String, Integer> lookup = new java.util.HashMap<>();
            List<Option> opts = optionRepository.findByQuestion_QuestionId(qId);
            if (opts != null) {
                for (Option o : opts) {
                    if (o != null && o.getOptionValue() != null && !o.getOptionValue().isBlank()) {
                        lookup.put(o.getOptionValue().trim().toLowerCase(), o.getOptionId());
                    }
                }
            }
            return lookup;
        });
        String normalized = rawAnswer.replace("[", "").replace("]", "").replace("\"", "").trim();
        if (normalized.isEmpty()) return ids;
        for (String part : normalized.split(",")) {
            if (part == null) continue;
            String token = part.trim();
            if (token.isEmpty()) continue;
            if (token.matches("\\d+")) {
                ids.add(Integer.valueOf(token));
            } else {
                Integer resolvedId = valueToId.get(token.toLowerCase());
                if (resolvedId != null) ids.add(resolvedId);
            }
        }
        return ids;
    }

    /**
     * Create a special permission time extension request for holiday work
     * Only creates ONE request per employee per date
     */
    private void createHolidayTimeExtensionRequest(Employee employee, LocalDate questionDate, OffsetDateTime now) {
        try {
            // Check if extension already exists for this employee on this date
            java.util.List<com.spearhead.ufc.model.AnswerSubmissionTimeExtension> existingExtensions = answerSubmissionTimeExtensionRepository
                    .findByEmployee_EmployeeIdAndQuestionDateAndIsActiveTrue(
                            employee.getEmployeeId(), questionDate);

            if (existingExtensions != null && !existingExtensions.isEmpty()) {
                log.info("Time extension already exists for employee {} on date {}",
                        employee.getEmployeeId(), questionDate);
                return;
            }

            // Create new time extension request
            com.spearhead.ufc.model.AnswerSubmissionTimeExtension extension = com.spearhead.ufc.model.AnswerSubmissionTimeExtension
                    .builder()
                    .employee(employee)
                    .questionDate(questionDate)
                    .grantedBy(employee) // Current user (who submitted the answer)
                    .isActive(false) // Pending approval
                    .isHoliday(true) // Marks this as holiday work
                    .reason("Worked on a holiday; kindly approve")
                    .createdDt(now)
                    .build();

            answerSubmissionTimeExtensionRepository.save(extension);
            log.info("Created time extension for employee {} on date {} for holiday work",
                    employee.getEmployeeId(), questionDate);
        } catch (Exception e) {
            log.error("Error creating holiday time extension for employee {} on date {}: {}",
                    employee.getEmployeeId(), questionDate, e.getMessage());
            // Don't throw exception - continue with submission save
        }
    }
}
