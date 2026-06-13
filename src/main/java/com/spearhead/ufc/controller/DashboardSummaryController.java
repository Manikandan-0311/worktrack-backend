package com.spearhead.ufc.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.spearhead.ufc.dto.DashboardDTO;
import com.spearhead.ufc.model.DashboardSummary;
import com.spearhead.ufc.service.DashboardSummaryService;
import com.spearhead.ufc.utils.DashboardSummaryProjection;
import com.spearhead.ufc.utils.AuthUtil;
import com.spearhead.ufc.model.Employee;
import com.spearhead.ufc.model.Submission;
import com.spearhead.ufc.model.EmployeeLocationAccess;
import com.spearhead.ufc.repository.EmployeeRepository;
import com.spearhead.ufc.repository.EmployeeLocationAccessRepository;
import com.spearhead.ufc.repository.OptionRepository;
import com.spearhead.ufc.repository.SubmissionRepository;

import com.spearhead.ufc.jms.ReportQueueProducer;
import com.spearhead.ufc.jms.ReportRequest;
import com.spearhead.ufc.jms.ReportStatusStore;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.UUID;
import java.sql.Date;

@RestController
@RequestMapping("/dashboardSummary")
@CrossOrigin(origins = "*")
public class DashboardSummaryController {

	private static final Logger log = LoggerFactory.getLogger(DashboardSummaryController.class);
	private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

	@Autowired
	private DashboardSummaryService dashboardSummaryService;

	@Autowired
	private AuthUtil authUtil;

	@Autowired
	private EmployeeRepository employeeRepository;

	@Autowired
	private SubmissionRepository submissionRepository;

	@Autowired
	private com.spearhead.ufc.repository.OrgRepository orgRepository;

	@Autowired
	private com.spearhead.ufc.repository.CalendarRepository calendarRepository;

	@Autowired
	private EmployeeLocationAccessRepository employeeLocationAccessRepository;

	@Autowired
	private OptionRepository optionRepository;

	@Autowired
	private ReportQueueProducer reportQueueProducer;

	@Autowired
	private ReportStatusStore reportStatusStore;

	@GetMapping("/summary")
	public ResponseEntity<Map<String, Object>> getSummary(@RequestParam(required = true) Integer orgId,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
			@RequestParam(required = false) Integer locationId, @RequestParam(required = false) Integer departmentId,
			@RequestParam(required = false) Integer employeeId) {

		log.info("Entered into getSummary method - DashboardSummaryController");
		Map<String, Object> resp = new HashMap<>();

		try {
			// Validate organization exists and is active
			java.util.Optional<com.spearhead.ufc.model.Org> orgOpt = orgRepository.findById(orgId);
			if (orgOpt.isEmpty()) {
				resp.put("success", false);
				resp.put("message", "Organization not found");
				return ResponseEntity.badRequest().body(resp);
			}
			com.spearhead.ufc.model.Org org = orgOpt.get();
			if (org.getIsActive() != null && !org.getIsActive()) {
				resp.put("success", false);
				resp.put("message", "Organization is inactive");
				return ResponseEntity.badRequest().body(resp);
			}

			List<DashboardSummary> summary = dashboardSummaryService.getSummaryByFilter(orgId,
					Optional.ofNullable(startDate), Optional.ofNullable(endDate), Optional.ofNullable(locationId),
					Optional.ofNullable(departmentId), Optional.ofNullable(employeeId));
			resp.put("success", true);
			resp.put("message", "Dashboard summary fetched successfully");
			resp.put("data", summary);
			// Provide tooltip labels for frontend action icons
			resp.put("tooltips", Map.of("edit", "Edit", "view", "View"));
			return ResponseEntity.ok(resp);

		} catch (Exception e) {
			log.error("Error occurred in getSummary method - DashboardSummaryController", e);
			resp.put("success", false);
			resp.put("message", "Failed to fetch dashboard summary. Please contact the administrator.");
			return ResponseEntity.internalServerError().body(resp);
		} finally {
			log.info("Exiting getSummary method - DashboardSummaryController");
		}
	}

	@GetMapping("/summaries")
	public ResponseEntity<Map<String, Object>> getSummaries(@RequestParam Integer orgId,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
			@RequestParam(required = false) Integer locationId, @RequestParam(required = false) Integer departmentId,
			@RequestParam(required = false) Integer employeeId) {

		log.info("Entered into getSummaries method - DashboardSummaryController");
		Map<String, Object> resp = new HashMap<>();

		try {
			// Validate organization exists and is active
			java.util.Optional<com.spearhead.ufc.model.Org> orgOpt = orgRepository.findById(orgId);
			if (orgOpt.isEmpty()) {
				resp.put("success", false);
				resp.put("message", "Organization not found");
				return ResponseEntity.badRequest().body(resp);
			}
			com.spearhead.ufc.model.Org org = orgOpt.get();
			if (org.getIsActive() != null && !org.getIsActive()) {
				resp.put("success", false);
				resp.put("message", "Organization is inactive");
				return ResponseEntity.badRequest().body(resp);
			}

			List<DashboardSummaryProjection> summaries = dashboardSummaryService.getFilteredSummaries(orgId, startDate,
					endDate, locationId, departmentId, employeeId);

			resp.put("success", true);
			resp.put("message", "Dashboard summaries fetched successfully");
			resp.put("data", summaries);
			// Provide tooltip labels for frontend action icons
			resp.put("tooltips", Map.of("edit", "Edit", "view", "View"));
			return ResponseEntity.ok(resp);

		} catch (Exception e) {
			log.error("Error occurred in getSummaries method - DashboardSummaryController", e);
			resp.put("success", false);
			resp.put("message", "Failed to fetch dashboard summaries. Please contact the administrator.");
			return ResponseEntity.internalServerError().body(resp);
		} finally {
			log.info("Exiting getSummaries method - DashboardSummaryController");
		}
	}

	@GetMapping("/newSummary")
	public ResponseEntity<Map<String, Object>> getDashboard(@RequestParam Integer orgId,
			@RequestParam(required = false) Integer locationId, @RequestParam(required = false) Integer departmentId) {

		log.info("Entered into getDashboard method - DashboardSummaryController");
		Map<String, Object> resp = new HashMap<>();

		try {
			// Validate organization exists and is active
			java.util.Optional<com.spearhead.ufc.model.Org> orgOpt = orgRepository.findById(orgId);
			if (orgOpt.isEmpty()) {
				resp.put("success", false);
				resp.put("message", "Organization not found");
				return ResponseEntity.badRequest().body(resp);
			}
			com.spearhead.ufc.model.Org org = orgOpt.get();
			if (org.getIsActive() != null && !org.getIsActive()) {
				resp.put("success", false);
				resp.put("message", "Organization is inactive");
				return ResponseEntity.badRequest().body(resp);
			}

			DashboardDTO dto = dashboardSummaryService.getDashboardData(orgId, locationId, departmentId);
			resp.put("success", true);
			resp.put("message", "Dashboard data fetched successfully");
			resp.put("data", dto);
			// Provide tooltip labels for frontend action icons
			resp.put("tooltips", Map.of("edit", "Edit", "view", "View"));
			return ResponseEntity.ok(resp);

		} catch (Exception e) {
			log.error("Error occurred in getDashboard method - DashboardSummaryController", e);
			resp.put("success", false);
			resp.put("message", "Failed to fetch dashboard data. Please contact the administrator.");
			return ResponseEntity.internalServerError().body(resp);
		} finally {
			log.info("Exiting getDashboard method - DashboardSummaryController");
		}
	}

	@GetMapping("/submission-status")
	public ResponseEntity<Map<String, Object>> getSubmissionStatus(
			@RequestHeader("Authorization") String authHeader,
			@RequestParam(required = false) Integer orgId,
			@RequestParam(required = true) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate date) {

		log.info("Entered getSubmissionStatus - orgIdParam={}, date={}", orgId, date);
		Map<String, Object> resp = new HashMap<>();

		try {
			Employee user = authUtil.getEmployeeFromToken(authHeader);
			if (user == null) {
				resp.put("success", false);
				resp.put("message", "Invalid or expired token");
				return ResponseEntity.status(401).body(resp);
			}

			// Determine if user is super-admin (role name containing 'SUPER')
			String roleName = (user.getRoleId() != null) ? user.getRoleId().getRoleName() : null;
			boolean isSuperAdmin = roleName != null && roleName.toUpperCase().contains("SUPER");

			Integer effectiveOrgId = null;
			if (isSuperAdmin) {
				effectiveOrgId = orgId;
			} else {
				effectiveOrgId = (user.getOrg() != null) ? user.getOrg().getOrgId() : null;
				if (effectiveOrgId == null) {
					resp.put("success", false);
					resp.put("message", "Organization not determined for user");
					return ResponseEntity.badRequest().body(resp);
				}
			}

			// Validate organization exists and is active
			if (effectiveOrgId != null) {
				java.util.Optional<com.spearhead.ufc.model.Org> orgOpt = orgRepository.findById(effectiveOrgId);
				if (orgOpt.isEmpty()) {
					resp.put("success", false);
					resp.put("message", "Organization not found");
					return ResponseEntity.badRequest().body(resp);
				}
				com.spearhead.ufc.model.Org org = orgOpt.get();
				if (org.getIsActive() != null && !org.getIsActive()) {
					resp.put("success", false);
					resp.put("message", "Organization is inactive");
					return ResponseEntity.badRequest().body(resp);
				}
			}

			List<Employee> employees = employeeRepository.findEmployeesByFilters(effectiveOrgId, null);
			int totalEmployees = (employees != null) ? employees.size() : 0;
			int submittedEmployees = 0;

			// Org working days cache (per orgId, for the month containing the requested date)
			YearMonth dateYm = YearMonth.from(date);
			LocalDate monthStart = dateYm.atDay(1);
			LocalDate monthEnd = monthStart.plusMonths(1);
			Map<Integer, Integer> orgWorkingDaysCache = new HashMap<>();

			// Option value → ID cache shared across all employees
			Map<Integer, Map<String, Integer>> optionValueToIdCache = new HashMap<>();

			List<Map<String, Object>> employeeDetails = new ArrayList<>();
			for (Employee e : employees) {
				int answersCount = 0;
				boolean submitted = false;
				double dayTotalWeightage = 0.0;
				double dayValidWeightage = 0.0;
				java.math.BigDecimal dayIncentiveAmount = java.math.BigDecimal.ZERO;
				java.math.BigDecimal perDayIncentiveBD = java.math.BigDecimal.ZERO;

				try {
					// Fetch only active submissions for this employee on the selected question_date
					List<Submission> subs = submissionRepository
							.findByEmployee_EmployeeIdAndQuestionDateGreaterThanEqualAndQuestionDateLessThanAndIsActiveTrue(
									e.getEmployeeId(), date, date.plusDays(1));

					if (subs != null && !subs.isEmpty()) {
						answersCount = subs.size();
						submitted = true;
						submittedEmployees++;

						// Org working days for this month
						Integer empOrgId = (e.getOrg() != null) ? e.getOrg().getOrgId() : null;
						int orgWorkingDays = dateYm.lengthOfMonth();
						if (empOrgId != null) {
							if (!orgWorkingDaysCache.containsKey(empOrgId)) {
								try {
									long wDays = calendarRepository.countWorkingDays(empOrgId, monthStart, monthEnd);
									orgWorkingDaysCache.put(empOrgId,
											(int) Math.max(1, Math.min(wDays, dateYm.lengthOfMonth())));
								} catch (Exception ex) {
									log.warn("Could not fetch working days for orgId={}: {}", empOrgId, ex.getMessage());
									orgWorkingDaysCache.put(empOrgId, dateYm.lengthOfMonth());
								}
							}
							orgWorkingDays = orgWorkingDaysCache.get(empOrgId);
						}

						java.math.BigDecimal totalEmpIncentive = (e.getIncentive() != null)
								? e.getIncentive() : java.math.BigDecimal.ZERO;
						if (orgWorkingDays > 0 && totalEmpIncentive.compareTo(java.math.BigDecimal.ZERO) > 0) {
							perDayIncentiveBD = totalEmpIncentive.divide(
									java.math.BigDecimal.valueOf(orgWorkingDays), 4,
									java.math.RoundingMode.HALF_UP);
						}

						// Apply question-type rules (same as top-employees-month)
						for (Submission s : subs) {
							if (s.getQuestionBank() == null) continue;
							com.spearhead.ufc.model.QuestionBank q = s.getQuestionBank();
							String questionType = q.getQuestionType() != null
									? q.getQuestionType().trim().toLowerCase() : "";
							Double weightage = q.getWeightage();
							if (weightage == null || weightage <= 0) continue;

							if ("radio".equals(questionType) || "multiselect".equals(questionType)) {
								Set<Integer> validAnswerIds = parseIds(q.getValidAnswer());
								if (validAnswerIds.isEmpty()) {
									// radio/multiselect + NULL valid_answer → skip entirely
									continue;
								}
								dayTotalWeightage += weightage;
								Set<Integer> submittedAnswerIds = resolveAnswerIds(
										s.getAnswer(), q.getQuestionId(), optionValueToIdCache);
								if (!java.util.Collections.disjoint(submittedAnswerIds, validAnswerIds)) {
									dayValidWeightage += weightage;
								}
							} else {
								// text/number/other: always in total; valid if answer non-empty
								dayTotalWeightage += weightage;
								String answer = s.getAnswer();
								if (answer != null && !answer.isBlank()) {
									dayValidWeightage += weightage;
								}
							}
						}

						// Day incentive = perDayIncentive * (dayValidWeightage / dayTotalWeightage)
						if (dayTotalWeightage > 0 && perDayIncentiveBD.compareTo(java.math.BigDecimal.ZERO) > 0) {
							java.math.BigDecimal costPerWeightage = perDayIncentiveBD.divide(
									java.math.BigDecimal.valueOf(dayTotalWeightage), 8,
									java.math.RoundingMode.HALF_UP);
							dayIncentiveAmount = costPerWeightage
									.multiply(java.math.BigDecimal.valueOf(dayValidWeightage));
						}
					}
				} catch (Exception ex) {
					log.warn("Error processing employee {} for submission-status: {}", e.getEmployeeId(), ex.getMessage());
				}

				Map<String, Object> ed = new HashMap<>();
				ed.put("employeeId", e.getEmployeeId());
				ed.put("employeeName", (e.getFirstName() == null ? "" : e.getFirstName())
						+ (e.getLastName() != null ? " " + e.getLastName() : ""));
				ed.put("employeeCode", e.getEmployeeCode() != null ? e.getEmployeeCode() : "");
				ed.put("answersCount", answersCount);
				ed.put("totalWeightage", java.math.BigDecimal.valueOf(dayTotalWeightage)
						.setScale(2, java.math.RoundingMode.HALF_UP).doubleValue());
				ed.put("validWeightage", java.math.BigDecimal.valueOf(dayValidWeightage)
						.setScale(2, java.math.RoundingMode.HALF_UP).doubleValue());
				ed.put("incentiveAmount", (int) Math.round(dayIncentiveAmount.doubleValue()));
				ed.put("perDayIncentive",
						perDayIncentiveBD.setScale(2, java.math.RoundingMode.HALF_UP).doubleValue());
				ed.put("submitted", submitted);
				employeeDetails.add(ed);
			}

			int unsubmittedEmployees = totalEmployees - submittedEmployees;
			Map<String, Object> data = new HashMap<>();
			data.put("totalEmployees", totalEmployees);
			data.put("submittedEmployees", submittedEmployees);
			data.put("unsubmittedEmployees", unsubmittedEmployees);
			data.put("employees", employeeDetails);
			resp.put("success", true);
			resp.put("message", "Submission status fetched successfully");
			resp.put("data", data);
			return ResponseEntity.ok(resp);
		} catch (Exception e) {
			log.error("Error in getSubmissionStatus", e);
			resp.put("success", false);
			resp.put("message", "Failed to fetch submission status: " + e.getMessage());
			return ResponseEntity.internalServerError().body(resp);
		} finally {
			log.info("Exiting getSubmissionStatus");
		}
	}

	@GetMapping("/top-employees-month")
	public ResponseEntity<Map<String, Object>> getTopEmployeesForMonth(
			@RequestHeader("Authorization") String authHeader,
			@RequestParam("month") String monthStr,
			@RequestParam(required = false) Integer orgId,
			@RequestParam(required = false) String orgIds,
			@RequestParam(required = false) String location,
			@RequestParam(required = false) String locationIds,
			@RequestParam(required = false) String employeeIds,
			@RequestParam(required = false, defaultValue = "10") Integer top,
			@RequestParam(required = false) Integer page,
			@RequestParam(required = false) Integer count) {

		log.info("Entered getTopEmployeesForMonth - month={}, orgIdParam={}, top={}", monthStr, orgId, top);
		Map<String, Object> resp = new HashMap<>();
		try {
			Employee user = authUtil.getEmployeeFromToken(authHeader);
			if (user == null) {
				resp.put("success", false);
				resp.put("message", "Invalid or expired token");
				return ResponseEntity.status(401).body(resp);
			}

			String roleName = (user.getRoleId() != null) ? user.getRoleId().getRoleName() : null;
			boolean isSuperAdmin = roleName != null && roleName.toUpperCase().contains("SUPER");

			Integer effectiveOrgId = null;
			if (isSuperAdmin) {
				effectiveOrgId = orgId;
			} else {
				effectiveOrgId = (user.getOrg() != null) ? user.getOrg().getOrgId() : null;
				if (effectiveOrgId == null) {
					resp.put("success", false);
					resp.put("message", "Organization not determined for user");
					return ResponseEntity.badRequest().body(resp);
				}
			}

			Set<Integer> orgIdSet = parseIdSet(orgIds);
			if ((orgIdSet == null || orgIdSet.isEmpty()) && orgId != null && orgId != 0) {
				orgIdSet = new HashSet<>();
				orgIdSet.add(orgId);
			}

			String locationFilterRaw = (locationIds == null || locationIds.trim().isEmpty()) ? location : locationIds;
			Set<Integer> requestedLocationIdSet = parseIdSet(locationFilterRaw);
			Integer requestedSelectedLocationId = parseFirstId(locationFilterRaw);
			Set<Integer> employeeIdSet = parseIdSet(employeeIds);

			if (!isSuperAdmin) {
				orgIdSet = new HashSet<>();
				orgIdSet.add(effectiveOrgId);
			}

			// Build authorized branch set from token user's default location + active employee_location_access
			Set<Integer> authorizedLocationIds = new HashSet<>();
			Integer defaultLocationId = null;
			if (user.getLocation() != null && user.getLocation().getLocationId() != null) {
				defaultLocationId = user.getLocation().getLocationId();
				authorizedLocationIds.add(defaultLocationId);
			}
			List<EmployeeLocationAccess> accessList = employeeLocationAccessRepository
					.findByEmployeeEmployeeIdAndIsActiveTrue(user.getEmployeeId());
			if (accessList != null) {
				for (EmployeeLocationAccess access : accessList) {
					if (access.getLocation() != null && access.getLocation().getLocationId() != null) {
						authorizedLocationIds.add(access.getLocation().getLocationId());
					}
				}
			}

			// Summary scope: always all authorized branches; for super-admin fallback to request when no access rows exist.
			Set<Integer> summaryLocationIds = new HashSet<>(authorizedLocationIds);
			if (summaryLocationIds.isEmpty() && isSuperAdmin) {
				summaryLocationIds.addAll(requestedLocationIdSet);
			}

			// Employee list scope: single selected branch (requested branch, else default branch).
			Integer selectedLocationId = null;
			if (requestedSelectedLocationId != null) {
				if (summaryLocationIds.isEmpty() || summaryLocationIds.contains(requestedSelectedLocationId)) {
					selectedLocationId = requestedSelectedLocationId;
				}
			}
			if (selectedLocationId == null && defaultLocationId != null) {
				if (summaryLocationIds.isEmpty() || summaryLocationIds.contains(defaultLocationId)) {
					selectedLocationId = defaultLocationId;
				}
			}
			if (selectedLocationId == null && !summaryLocationIds.isEmpty()) {
				selectedLocationId = summaryLocationIds.stream().sorted().findFirst().orElse(null);
			}

			if (effectiveOrgId != null && !isSuperAdmin) {
				java.util.Optional<com.spearhead.ufc.model.Org> orgOpt = orgRepository.findById(effectiveOrgId);
				if (orgOpt.isEmpty()) {
					resp.put("success", false);
					resp.put("message", "Organization not found");
					return ResponseEntity.badRequest().body(resp);
				}
				com.spearhead.ufc.model.Org org = orgOpt.get();
				if (org.getIsActive() != null && !org.getIsActive()) {
					resp.put("success", false);
					resp.put("message", "Organization is inactive");
					return ResponseEntity.badRequest().body(resp);
				}
			}

			YearMonth ym;
			try {
				DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM");
				ym = YearMonth.parse(monthStr, fmt);
			} catch (Exception ex) {
				resp.put("success", false);
				resp.put("message", "Invalid month format. Use yyyy-MM");
				return ResponseEntity.badRequest().body(resp);
			}

			LocalDate start = ym.atDay(1);
			LocalDate end = start.plusMonths(1); // exclusive month end
			LocalDate today = LocalDate.now();
			YearMonth currentMonth = YearMonth.from(today);
			LocalDate effectiveEnd = end;

			// Align with employee-month-progress: for current month, calculate only up to today.
			if (ym.equals(currentMonth)) {
				effectiveEnd = today.plusDays(1); // exclusive end, so include today
			} else if (ym.isAfter(currentMonth)) {
				// Future month: no eligible range
				effectiveEnd = start;
			}

			Date startDate = Date.valueOf(start);
			Date endDate = Date.valueOf(effectiveEnd);

			// Get all employees in the organization(s)
			Integer orgFilterForEmployees = null;
			if (orgIdSet != null && orgIdSet.size() == 1) {
				orgFilterForEmployees = orgIdSet.iterator().next();
			}
			List<Employee> allEmployees = employeeRepository.findEmployeesByFilters(orgFilterForEmployees, null);
			if (allEmployees == null) {
				allEmployees = new ArrayList<>();
			}

			if (orgIdSet != null && !orgIdSet.isEmpty()) {
				Set<Integer> allowedOrgIds = orgIdSet;
				allEmployees = allEmployees.stream()
						.filter(emp -> emp.getOrg() != null && emp.getOrg().getOrgId() != null
								&& allowedOrgIds.contains(emp.getOrg().getOrgId()))
						.toList();
			}
			if (summaryLocationIds != null && !summaryLocationIds.isEmpty()) {
				Set<Integer> allowedLocationIds = summaryLocationIds;
				allEmployees = allEmployees.stream()
						.filter(emp -> emp.getLocation() != null && emp.getLocation().getLocationId() != null
								&& allowedLocationIds.contains(emp.getLocation().getLocationId()))
						.toList();
			}

			// Filter to include only employees from active organizations
			allEmployees = allEmployees.stream()
					.filter(emp -> emp.getOrg() != null && emp.getOrg().getIsActive() != null && emp.getOrg().getIsActive())
					.toList();

			Set<Integer> eligibleEmployeeIds = new HashSet<>();
			for (Employee emp : allEmployees) {
				if (emp.getEmployeeId() != null) {
					eligibleEmployeeIds.add(emp.getEmployeeId());
				}
			}

			// Get submitted employees with scores for the month
			Integer submissionsOrgFilter = null;
			if (!isSuperAdmin) {
				submissionsOrgFilter = effectiveOrgId;
			} else if (orgIdSet != null && orgIdSet.size() == 1) {
				submissionsOrgFilter = orgIdSet.iterator().next();
			}
			List<Object[]> submittedRows = submissionRepository.getTopEmployeesByQuestionDateRange(startDate, endDate,
					submissionsOrgFilter);

			Map<Integer, Object[]> submittedMap = new HashMap<>();
			if (submittedRows != null) {
				for (Object[] r : submittedRows) {
					if (r != null && r[0] != null) {
						Integer empId = ((Number) r[0]).intValue();
						if (eligibleEmployeeIds.isEmpty() || eligibleEmployeeIds.contains(empId)) {
							submittedMap.put(empId, r);
						}
					}
				}
			}

			List<Map<String, Object>> allEmployeesList = new ArrayList<>();
			java.math.BigDecimal totalIncentiveAmount = java.math.BigDecimal.ZERO;
			int totalDaysInMonth = ym.lengthOfMonth();
			LocalDate fullMonthEndExclusive = end;
			Map<Integer, Integer> orgWorkingDaysCache = new HashMap<>();
			Map<Integer, Integer> employeeLocationById = new HashMap<>();

			Map<Integer, Map<String, Integer>> optionValueToIdCache = new HashMap<>();
			if (allEmployees != null) {
				for (Employee emp : allEmployees) {
					if (emp.getEmployeeId() == null)
						continue;

					Map<String, Object> item = new HashMap<>();
					item.put("employeeId", emp.getEmployeeId());
					item.put("employeeName", (emp.getFirstName() != null ? emp.getFirstName() : "") +
							(emp.getLastName() != null ? " " + emp.getLastName() : ""));
					item.put("employeeCode", emp.getEmployeeCode() != null ? emp.getEmployeeCode() : "");
					item.put("locationId", emp.getLocation() != null ? emp.getLocation().getLocationId() : null);
					employeeLocationById.put(emp.getEmployeeId(),
							emp.getLocation() != null ? emp.getLocation().getLocationId() : null);

					// Calculate incentive for each employee
					java.math.BigDecimal employeeIncentive = java.math.BigDecimal.ZERO;
					long staffWorkingDays = 0;
					int companyWorkingDaysInMonth = totalDaysInMonth;
					java.math.BigDecimal perDayIncentive = java.math.BigDecimal.ZERO;
					double validWeightage = 0.0;

					// Check if employee has submissions for this month
					if (submittedMap.containsKey(emp.getEmployeeId())) {
						Object[] r = submittedMap.get(emp.getEmployeeId());
						Number totalScore = (r[2] != null) ? (Number) r[2] : 0;
						item.put("totalScore", totalScore.intValue());
						item.put("answered", true);

						// Calculate incentive only if employee has incentive configured
						if (emp.getIncentive() != null && emp.getIncentive().doubleValue() > 0) {
							java.math.BigDecimal totalEmpIncentive = emp.getIncentive();

							Integer employeeOrgId = (emp.getOrg() != null) ? emp.getOrg().getOrgId() : null;
							if (employeeOrgId != null) {
								if (!orgWorkingDaysCache.containsKey(employeeOrgId)) {
									int computedWorkingDays = totalDaysInMonth;
									try {
										long workingDays = calendarRepository.countWorkingDays(employeeOrgId, start,
												fullMonthEndExclusive);
										computedWorkingDays = (int) Math.max(0, Math.min(workingDays, totalDaysInMonth));
									} catch (Exception ex) {
										log.warn("Could not fetch full-month working days for orgId={}, using month days fallback: {}",
												employeeOrgId, ex.getMessage());
									}
									orgWorkingDaysCache.put(employeeOrgId, computedWorkingDays);
								}
								companyWorkingDaysInMonth = orgWorkingDaysCache.get(employeeOrgId);
							}

							if (companyWorkingDaysInMonth > 0) {
								perDayIncentive = totalEmpIncentive.divide(
										java.math.BigDecimal.valueOf(companyWorkingDaysInMonth), 4,
										java.math.RoundingMode.HALF_UP);

								Map<String, Object> calc = calculateValidAnswerBasedIncentive(
										emp,
										start,
										effectiveEnd,
										optionValueToIdCache);
								employeeIncentive = (java.math.BigDecimal) calc.getOrDefault("employeeIncentive",
										java.math.BigDecimal.ZERO);
								staffWorkingDays = ((Number) calc.getOrDefault("staffWorkingDays", 0L)).longValue();
								validWeightage = ((Number) calc.getOrDefault("validWeightage", 0.0)).doubleValue();
							}
						}
					} else {
						item.put("totalScore", 0);
						item.put("answered", false);
					}

					item.put("incentiveAmount", (int) Math.round(employeeIncentive.doubleValue()));
					item.put("staffWorkingDays", staffWorkingDays);
					item.put("organizationWorkingDays", companyWorkingDaysInMonth);
					item.put("perDayIncentive", perDayIncentive.setScale(2, java.math.RoundingMode.HALF_UP).doubleValue());
					item.put("validWeightage", java.math.BigDecimal.valueOf(validWeightage)
							.setScale(2, java.math.RoundingMode.HALF_UP)
							.doubleValue());
					totalIncentiveAmount = totalIncentiveAmount.add(employeeIncentive);
					allEmployeesList.add(item);
				}
			}

			allEmployeesList.sort((a, b) -> {
				boolean answeredA = (Boolean) a.getOrDefault("answered", false);
				boolean answeredB = (Boolean) b.getOrDefault("answered", false);
				if (answeredA != answeredB) {
					return answeredB ? 1 : -1; // answered items first
				}
				Integer scoreA = (Integer) a.getOrDefault("totalScore", 0);
				Integer scoreB = (Integer) b.getOrDefault("totalScore", 0);
				return scoreB.compareTo(scoreA); // descending order by score
			});

			Set<Integer> selectedLocationIdsForList = new HashSet<>();
			if (requestedLocationIdSet != null && !requestedLocationIdSet.isEmpty()) {
				if (summaryLocationIds != null && !summaryLocationIds.isEmpty()) {
					for (Integer locId : requestedLocationIdSet) {
						if (summaryLocationIds.contains(locId)) {
							selectedLocationIdsForList.add(locId);
						}
					}
				} else {
					selectedLocationIdsForList.addAll(requestedLocationIdSet);
				}
			}
			if (selectedLocationIdsForList.isEmpty() && selectedLocationId != null) {
				selectedLocationIdsForList.add(selectedLocationId);
			}

			final Set<Integer> selectedLocationIdsForListFinal = selectedLocationIdsForList;
			List<Map<String, Object>> branchEmployees = allEmployeesList.stream()
					.filter(item -> {
						Integer empId = (Integer) item.get("employeeId");
						Integer empLocationId = employeeLocationById.get(empId);
						if (selectedLocationIdsForListFinal != null && !selectedLocationIdsForListFinal.isEmpty()
								&& (empLocationId == null || !selectedLocationIdsForListFinal.contains(empLocationId))) {
							return false;
						}
						if (employeeIdSet != null && !employeeIdSet.isEmpty()) {
							return empId != null && employeeIdSet.contains(empId);
						}
						return true;
					})
					.toList();

			int pageValue = (page == null || page < 0) ? 0 : page;
			int countValue = (count == null || count <= 0) ? (top != null ? top : 10) : count;
			int totalRecords = branchEmployees.size();
			int fromIndex = Math.min(pageValue * countValue, totalRecords);
			int toIndex = Math.min(fromIndex + countValue, totalRecords);
			List<Map<String, Object>> pagedEmployees = new ArrayList<>(branchEmployees.subList(fromIndex, toIndex));
			pagedEmployees.forEach(item -> item.remove("locationId"));

			// Summary totals: always computed from ALL authorized employees regardless of any filter
			// (filter only affects the paginated employee list and totalRecords)
			java.math.BigDecimal allEmployeesTotalIncentive = allEmployeesList.stream()
					.map(item -> java.math.BigDecimal.valueOf(
							((Number) item.getOrDefault("incentiveAmount", 0)).doubleValue()))
					.reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);

			double totalWeightage = allEmployeesList.stream()
					.mapToDouble(item -> ((Number) item.getOrDefault("validWeightage", 0.0)).doubleValue())
					.sum();

			// Calculate total working days for the authorized summary scope (stable across branch pagination/filtering)
			int totalWorkingDaysInMonth = totalDaysInMonth;
			try {
				Set<Integer> summaryOrgIds = new HashSet<>();
				for (Employee emp : allEmployees) {
					if (emp.getOrg() != null && emp.getOrg().getOrgId() != null) {
						summaryOrgIds.add(emp.getOrg().getOrgId());
					}
				}
				if (!summaryOrgIds.isEmpty()) {
					int maxWorkingDays = 0;
					for (Integer summaryOrgId : summaryOrgIds) {
						long workingDays = calendarRepository.countWorkingDays(summaryOrgId, start, fullMonthEndExclusive);
						int normalizedDays = (int) Math.max(0, Math.min(workingDays, totalDaysInMonth));
						maxWorkingDays = Math.max(maxWorkingDays, normalizedDays);
					}
					totalWorkingDaysInMonth = maxWorkingDays;
				}
				log.info("Working days calculation: month={}, totalDays={}, workingDays={}",
						monthStr, totalDaysInMonth, totalWorkingDaysInMonth);
			} catch (Exception e) {
				log.warn("Could not fetch calendar entries, using default month length: {}", e.getMessage());
			}

			resp.put("success", true);
			resp.put("message", "All employees fetched successfully");
			Map<String, Object> data = new HashMap<>();
			data.put("month", monthStr);
			data.put("employees", pagedEmployees);
			data.put("totalIncentiveAmount", (int) Math.round(allEmployeesTotalIncentive.doubleValue()));
			data.put("totalWorkingDays", totalWorkingDaysInMonth);
			Map<String, Object> branchMonthlySummary = new HashMap<>();
			branchMonthlySummary.put("title", "Branch Monthly Summary");
			branchMonthlySummary.put("totalEmployees", allEmployeesList.size());
			branchMonthlySummary.put("totalIncentiveInr", (int) Math.round(allEmployeesTotalIncentive.doubleValue()));
			branchMonthlySummary.put("totalWeightage", java.math.BigDecimal.valueOf(totalWeightage)
					.setScale(2, java.math.RoundingMode.HALF_UP).doubleValue());
			branchMonthlySummary.put("totalWorkingDays", totalWorkingDaysInMonth);
			data.put("branchMonthlySummary", branchMonthlySummary);
			data.put("selectedBranchId", selectedLocationId);
			data.put("page", pageValue);
			data.put("count", countValue);
			data.put("totalRecords", totalRecords);
			resp.put("data", data);
			return ResponseEntity.ok(resp);
		} catch (Exception e) {
			log.error("Error in getTopEmployeesForMonth", e);
			resp.put("success", false);
			resp.put("message", "Failed to fetch employees: " + e.getMessage());
			return ResponseEntity.internalServerError().body(resp);
		} finally {
			log.info("Exiting getTopEmployeesForMonth");
		}
	}

	private Set<Integer> parseIdSet(String ids) {
		if (ids == null || ids.trim().isEmpty()) {
			return new HashSet<>();
		}
		Set<Integer> result = new HashSet<>();
		String[] parts = ids.split(",");
		for (String part : parts) {
			String trimmed = part.trim();
			if (trimmed.isEmpty()) {
				continue;
			}
			try {
				Integer value = Integer.parseInt(trimmed);
				if (value != 0) {
					result.add(value);
				}
			} catch (NumberFormatException ex) {
				log.debug("Ignoring invalid id value: {}", trimmed);
			}
		}
		return result;
	}

	private Integer parseFirstId(String ids) {
		if (ids == null || ids.trim().isEmpty()) {
			return null;
		}
		String[] parts = ids.split(",");
		for (String part : parts) {
			String trimmed = part.trim();
			if (trimmed.isEmpty()) {
				continue;
			}
			try {
				Integer value = Integer.parseInt(trimmed);
				if (value != 0) {
					return value;
				}
			} catch (NumberFormatException ex) {
				log.debug("Ignoring invalid id value while parsing first id: {}", trimmed);
			}
		}
		return null;
	}

	private LocalDate toLocalDateValue(Object value) {
		if (value == null) {
			return null;
		}
		if (value instanceof LocalDate localDate) {
			return localDate;
		}
		if (value instanceof java.sql.Date sqlDate) {
			return sqlDate.toLocalDate();
		}
		if (value instanceof java.sql.Timestamp timestamp) {
			return timestamp.toLocalDateTime().toLocalDate();
		}
		if (value instanceof java.time.Instant instant) {
			return instant.atZone(java.time.ZoneId.systemDefault()).toLocalDate();
		}
		if (value instanceof java.util.Date date) {
			return date.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate();
		}
		return null;
	}

	private LocalDateTime toLocalDateTimeValue(Object value) {
		if (value == null) {
			return null;
		}
		if (value instanceof LocalDateTime localDateTime) {
			return localDateTime;
		}
		if (value instanceof java.sql.Timestamp timestamp) {
			return timestamp.toLocalDateTime();
		}
		if (value instanceof java.time.Instant instant) {
			return instant.atZone(java.time.ZoneId.systemDefault()).toLocalDateTime();
		}
		if (value instanceof java.util.Date date) {
			return date.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime();
		}
		return null;
	}

	private Object getColumnValue(Object[] row, int preferredIndex, int fallbackIndex) {
		if (row == null) {
			return null;
		}
		if (preferredIndex >= 0 && row.length > preferredIndex) {
			return row[preferredIndex];
		}
		if (fallbackIndex >= 0 && row.length > fallbackIndex) {
			return row[fallbackIndex];
		}
		return null;
	}

	private Map<String, Object> calculateValidAnswerBasedIncentive(
			Employee employee,
			LocalDate submittedAtStart,
			LocalDate submittedAtEnd,
			Map<Integer, Map<String, Integer>> optionValueToIdCache) {
		Map<String, Object> result = new HashMap<>();
		java.math.BigDecimal totalEmployeeIncentive = java.math.BigDecimal.ZERO;
		double totalValidWeightage = 0.0;
		long totalStaffWorkingDays = 0;

		java.math.BigDecimal totalEmpIncentive = (employee.getIncentive() != null)
				? employee.getIncentive() : java.math.BigDecimal.ZERO;
		Integer employeeOrgId = (employee.getOrg() != null) ? employee.getOrg().getOrgId() : null;

		// Fetch submissions by submitted_at range (covers time-extension cross-month submissions)
		java.time.ZoneId zoneId = java.time.ZoneId.systemDefault();
		java.time.OffsetDateTime startDateTime = submittedAtStart.atStartOfDay(zoneId).toOffsetDateTime();
		java.time.OffsetDateTime endDateTime = submittedAtEnd.atStartOfDay(zoneId).toOffsetDateTime();

		List<Submission> submissions = submissionRepository.findActiveByEmployeeAndSubmittedAtRange(
				employee.getEmployeeId(), startDateTime, endDateTime);
		if (submissions == null || submissions.isEmpty()) {
			result.put("employeeIncentive", java.math.BigDecimal.ZERO);
			result.put("staffWorkingDays", 0L);
			result.put("validWeightage", 0.0);
			return result;
		}

		// Group by YearMonth of question_date — each month uses its own working days
		Map<YearMonth, List<Submission>> byMonth = submissions.stream()
				.filter(s -> s.getQuestionDate() != null)
				.collect(java.util.stream.Collectors.groupingBy(s -> YearMonth.from(s.getQuestionDate())));

		// Cache to avoid redundant DB calls per month
		Map<YearMonth, Integer> monthWorkingDaysCache = new HashMap<>();

		for (Map.Entry<YearMonth, List<Submission>> monthEntry : byMonth.entrySet()) {
			YearMonth ym = monthEntry.getKey();
			List<Submission> monthSubmissions = monthEntry.getValue();

			// Working days for this specific month from the organization calendar
			int orgWorkingDays = monthWorkingDaysCache.computeIfAbsent(ym, m -> {
				int totalDays = m.lengthOfMonth();
				if (employeeOrgId == null) return totalDays;
				try {
					LocalDate mStart = m.atDay(1);
					LocalDate mEnd = mStart.plusMonths(1);
					long wDays = calendarRepository.countWorkingDays(employeeOrgId, mStart, mEnd);
					return (int) Math.max(1, Math.min(wDays, totalDays));
				} catch (Exception ex) {
					log.warn("Could not fetch working days for month={}, orgId={}: {}", m, employeeOrgId, ex.getMessage());
					return m.lengthOfMonth();
				}
			});

			// Per-day incentive rate for this month
			java.math.BigDecimal perDayIncentiveForMonth = (orgWorkingDays > 0
						&& totalEmpIncentive.compareTo(java.math.BigDecimal.ZERO) > 0)
						? totalEmpIncentive.divide(java.math.BigDecimal.valueOf(orgWorkingDays), 4,
								java.math.RoundingMode.HALF_UP)
						: java.math.BigDecimal.ZERO;

			// Group by question_date within this month
			Map<LocalDate, List<Submission>> byDay = monthSubmissions.stream()
					.collect(java.util.stream.Collectors.groupingBy(Submission::getQuestionDate));
			totalStaffWorkingDays += byDay.size();

			for (Map.Entry<LocalDate, List<Submission>> dayEntry : byDay.entrySet()) {
				List<Submission> daySubmissions = dayEntry.getValue();
				if (daySubmissions == null || daySubmissions.isEmpty()) continue;

				double dayTotalWeightage = 0.0;
				double dayValidWeightage = 0.0;

				for (Submission submission : daySubmissions) {
					if (submission == null || submission.getQuestionBank() == null) continue;
					com.spearhead.ufc.model.QuestionBank question = submission.getQuestionBank();
					String questionType = question.getQuestionType() != null
							? question.getQuestionType().trim().toLowerCase() : "";

					Double weightage = question.getWeightage();
					if (weightage == null || weightage <= 0) continue;

					if ("radio".equals(questionType) || "multiselect".equals(questionType)) {
						Set<Integer> validAnswerIds = parseIds(question.getValidAnswer());
						if (validAnswerIds.isEmpty()) {
							// valid_answer is NULL — skip entirely, don't count in total or valid
							continue;
						}
						// valid_answer configured — add to total, then check if answer matches
						dayTotalWeightage += weightage;
						Set<Integer> submittedAnswerIds = resolveAnswerIds(
								submission.getAnswer(),
								question.getQuestionId(),
								optionValueToIdCache);
						if (!java.util.Collections.disjoint(submittedAnswerIds, validAnswerIds)) {
							dayValidWeightage += weightage;
						}
					} else {
						// text/number/other: always count in total; valid if answer is non-empty
						dayTotalWeightage += weightage;
						String answer = submission.getAnswer();
						if (answer != null && !answer.isBlank()) {
							dayValidWeightage += weightage;
						}
					}
				}

				if (dayTotalWeightage > 0) {
					java.math.BigDecimal costPerWeightage = perDayIncentiveForMonth
							.divide(java.math.BigDecimal.valueOf(dayTotalWeightage), 8,
									java.math.RoundingMode.HALF_UP);
					java.math.BigDecimal dayIncentive = costPerWeightage
							.multiply(java.math.BigDecimal.valueOf(dayValidWeightage));
					totalEmployeeIncentive = totalEmployeeIncentive.add(dayIncentive);
					totalValidWeightage += dayValidWeightage;
				}
			}
		}

		result.put("employeeIncentive", totalEmployeeIncentive.setScale(2, java.math.RoundingMode.HALF_UP));
		result.put("staffWorkingDays", totalStaffWorkingDays);
		result.put("validWeightage", totalValidWeightage);
		return result;
	}

	private Set<Integer> parseIds(String csvIds) {
		Set<Integer> ids = new HashSet<>();
		if (csvIds == null || csvIds.isBlank()) {
			return ids;
		}
		for (String part : csvIds.split(",")) {
			if (part == null) {
				continue;
			}
			String trimmed = part.trim();
			if (trimmed.matches("\\d+")) {
				ids.add(Integer.valueOf(trimmed));
			}
		}
		return ids;
	}

	private Set<Integer> resolveAnswerIds(String rawAnswer, Integer questionId,
			Map<Integer, Map<String, Integer>> optionValueToIdCache) {
		Set<Integer> ids = new HashSet<>();
		if (rawAnswer == null || rawAnswer.isBlank()) {
			return ids;
		}

		Map<String, Integer> valueToId = optionValueToIdCache.computeIfAbsent(questionId, qId -> {
			Map<String, Integer> lookup = new HashMap<>();
			List<com.spearhead.ufc.model.Option> options = optionRepository.findByQuestion_QuestionId(qId);
			if (options != null) {
				for (com.spearhead.ufc.model.Option option : options) {
					if (option != null && option.getOptionValue() != null && !option.getOptionValue().isBlank()) {
						lookup.put(option.getOptionValue().trim().toLowerCase(), option.getOptionId());
					}
				}
			}
			return lookup;
		});

		String normalized = rawAnswer.replace("[", "")
				.replace("]", "")
				.replace("\"", "")
				.trim();
		if (normalized.isEmpty()) {
			return ids;
		}

		for (String part : normalized.split(",")) {
			if (part == null) {
				continue;
			}
			String token = part.trim();
			if (token.isEmpty()) {
				continue;
			}
			if (token.matches("\\d+")) {
				ids.add(Integer.valueOf(token));
			} else {
				Integer resolvedId = valueToId.get(token.toLowerCase());
				if (resolvedId != null) {
					ids.add(resolvedId);
				}
			}
		}
		return ids;
	}

	private int toIntValue(Object value) {
		if (value == null) {
			return 0;
		}
		if (value instanceof Number number) {
			return number.intValue();
		}
		if (value instanceof String str) {
			try {
				return Integer.parseInt(str);
			} catch (NumberFormatException ex) {
				log.debug("Unable to parse integer value '{}'", str);
			}
		}
		return 0;
	}

	@GetMapping("/employee-month-progress")
	public ResponseEntity<Map<String, Object>> getEmployeeMonthProgress(
			@RequestHeader("Authorization") String authHeader,
			@RequestParam("employeeId") Integer employeeId,
			@RequestParam("month") String monthStr) {

		log.info("Entered getEmployeeMonthProgress - employeeId={}, month={}", employeeId, monthStr);
		Map<String, Object> resp = new HashMap<>();
		try {
			Employee user = authUtil.getEmployeeFromToken(authHeader);
			if (user == null) {
				resp.put("success", false);
				resp.put("message", "Invalid or expired token");
				return ResponseEntity.status(401).body(resp);
			}

			YearMonth ym;
			try {
				DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM");
				ym = YearMonth.parse(monthStr, fmt);
			} catch (Exception ex) {
				resp.put("success", false);
				resp.put("message", "Invalid month format. Use yyyy-MM");
				return ResponseEntity.badRequest().body(resp);
			}

			LocalDate start = ym.atDay(1);
			LocalDate end = start.plusMonths(1);
			// For the current month only process up to today; past months use the full month
			LocalDate effectiveEnd = YearMonth.now().equals(ym) ? LocalDate.now().plusDays(1) : end;

			// Fetch base daily data (totalScore, answeredQuestions, totalQuestions, submittedDateTime)
			java.sql.Date startDate = java.sql.Date.valueOf(start);
			java.sql.Date endDate = java.sql.Date.valueOf(end);
			List<Object[]> rows = submissionRepository.getEmployeeDailyProgressNative(employeeId, startDate, endDate);

			// Fetch employee entity
			Optional<Employee> empOpt = employeeRepository.findById(employeeId);
			Employee emp = empOpt.orElse(null);
			String employeeName = emp != null
					? (emp.getFirstName() == null ? "" : emp.getFirstName())
							+ (emp.getLastName() != null ? " " + emp.getLastName() : "")
					: "";

			// Org working days for this month
			Integer orgId = emp != null && emp.getOrg() != null ? emp.getOrg().getOrgId() : null;
			int orgWorkingDays = ym.lengthOfMonth();
			if (orgId != null) {
				try {
					long wDays = calendarRepository.countWorkingDays(orgId, start, end);
					orgWorkingDays = (int) Math.max(1, Math.min(wDays, ym.lengthOfMonth()));
				} catch (Exception ex) {
					log.warn("Could not fetch working days for orgId={}: {}", orgId, ex.getMessage());
				}
			}

			java.math.BigDecimal totalEmpIncentive = (emp != null && emp.getIncentive() != null)
					? emp.getIncentive() : java.math.BigDecimal.ZERO;
			java.math.BigDecimal perDayIncentiveBD = (orgWorkingDays > 0
					&& totalEmpIncentive.compareTo(java.math.BigDecimal.ZERO) > 0)
							? totalEmpIncentive.divide(java.math.BigDecimal.valueOf(orgWorkingDays), 4,
									java.math.RoundingMode.HALF_UP)
							: java.math.BigDecimal.ZERO;

			// Build per-day incentive map using the same question-type rules as top-employees-month
			Map<Integer, Map<String, Integer>> optionValueToIdCache = new HashMap<>();
			Map<LocalDate, Map<String, Object>> dailyIncentiveMap = new HashMap<>();
			java.math.BigDecimal totalEmployeeIncentive = java.math.BigDecimal.ZERO;
			double totalValidWeightage = 0.0;

			if (emp != null && totalEmpIncentive.compareTo(java.math.BigDecimal.ZERO) > 0) {
				java.time.ZoneId zoneId = java.time.ZoneId.systemDefault();
				java.time.OffsetDateTime startDateTime = start.atStartOfDay(zoneId).toOffsetDateTime();
				java.time.OffsetDateTime endDateTime = effectiveEnd.atStartOfDay(zoneId).toOffsetDateTime();

				List<Submission> submissions = submissionRepository.findActiveByEmployeeAndSubmittedAtRange(
						employeeId, startDateTime, endDateTime);

				if (submissions != null && !submissions.isEmpty()) {
					// Group by YearMonth of question_date — each month gets its own working-days rate
					Map<YearMonth, List<Submission>> byMonth = submissions.stream()
							.filter(s -> s.getQuestionDate() != null)
							.collect(java.util.stream.Collectors.groupingBy(
									s -> YearMonth.from(s.getQuestionDate())));

					Map<YearMonth, Integer> monthWorkingDaysCache = new HashMap<>();

					for (Map.Entry<YearMonth, List<Submission>> monthEntry : byMonth.entrySet()) {
						YearMonth mym = monthEntry.getKey();
						List<Submission> monthSubs = monthEntry.getValue();

						int mOrgWorkingDays = monthWorkingDaysCache.computeIfAbsent(mym, m -> {
							int totalDays = m.lengthOfMonth();
							if (orgId == null) return totalDays;
							try {
								LocalDate mStart = m.atDay(1);
								LocalDate mEnd = mStart.plusMonths(1);
								long wDays = calendarRepository.countWorkingDays(orgId, mStart, mEnd);
								return (int) Math.max(1, Math.min(wDays, totalDays));
							} catch (Exception ex) {
								return m.lengthOfMonth();
							}
						});

						java.math.BigDecimal perDayForMonth = (mOrgWorkingDays > 0
								&& totalEmpIncentive.compareTo(java.math.BigDecimal.ZERO) > 0)
										? totalEmpIncentive.divide(java.math.BigDecimal.valueOf(mOrgWorkingDays), 4,
												java.math.RoundingMode.HALF_UP)
										: java.math.BigDecimal.ZERO;

						// Group by question_date within this month
						Map<LocalDate, List<Submission>> byDay = monthSubs.stream()
								.collect(java.util.stream.Collectors.groupingBy(Submission::getQuestionDate));

						for (Map.Entry<LocalDate, List<Submission>> dayEntry : byDay.entrySet()) {
							LocalDate dayDate = dayEntry.getKey();
							List<Submission> daySubs = dayEntry.getValue();

							double dayTotalWeightage = 0.0;
							double dayValidWeightage = 0.0;

							for (Submission submission : daySubs) {
								if (submission == null || submission.getQuestionBank() == null) continue;
								com.spearhead.ufc.model.QuestionBank question = submission.getQuestionBank();
								String questionType = question.getQuestionType() != null
										? question.getQuestionType().trim().toLowerCase() : "";
								Double weightage = question.getWeightage();
								if (weightage == null || weightage <= 0) continue;

								if ("radio".equals(questionType) || "multiselect".equals(questionType)) {
									Set<Integer> validAnswerIds = parseIds(question.getValidAnswer());
									if (validAnswerIds.isEmpty()) {
										// NULL valid_answer → skip entirely (no total, no valid)
										continue;
									}
									dayTotalWeightage += weightage;
									Set<Integer> submittedAnswerIds = resolveAnswerIds(
											submission.getAnswer(),
											question.getQuestionId(),
											optionValueToIdCache);
									if (!java.util.Collections.disjoint(submittedAnswerIds, validAnswerIds)) {
										dayValidWeightage += weightage;
									}
								} else {
									// text/number/other: always in total; valid if answer non-empty
									dayTotalWeightage += weightage;
									String answer = submission.getAnswer();
									if (answer != null && !answer.isBlank()) {
										dayValidWeightage += weightage;
									}
								}
							}

							if (dayTotalWeightage > 0) {
								java.math.BigDecimal costPerWeightage = perDayForMonth.divide(
										java.math.BigDecimal.valueOf(dayTotalWeightage), 8,
										java.math.RoundingMode.HALF_UP);
								java.math.BigDecimal dayIncentive = costPerWeightage
										.multiply(java.math.BigDecimal.valueOf(dayValidWeightage));
								totalEmployeeIncentive = totalEmployeeIncentive.add(dayIncentive);
								totalValidWeightage += dayValidWeightage;

								Map<String, Object> dayResult = new HashMap<>();
								dayResult.put("incentiveAmount", dayIncentive);
								dayResult.put("validWeightage", dayValidWeightage);
								dayResult.put("totalWeightage", dayTotalWeightage);
								dailyIncentiveMap.put(dayDate, dayResult);
							}
						}
					}
				}
			}

			// Build row map from DB query results
			Map<LocalDate, Object[]> rowMap = new HashMap<>();
			if (rows != null) {
				for (Object[] r : rows) {
					if (r == null) continue;
					LocalDate questionDate = toLocalDateValue(r[2]);
					if (questionDate != null) {
						rowMap.put(questionDate, r);
					}
				}
			}

			List<Map<String, Object>> daily = new ArrayList<>();
			for (LocalDate d = start; d.isBefore(end); d = d.plusDays(1)) {
				Object[] r = rowMap.get(d);
				if (r == null) continue;
				LocalDateTime submittedDateTime = toLocalDateTimeValue(getColumnValue(r, 3, 2));
				if (submittedDateTime == null) continue;

				Map<String, Object> day = new HashMap<>();
				LocalDate questionDateValue = toLocalDateValue(getColumnValue(r, 2, 2));
				day.put("questionDate", questionDateValue != null ? questionDateValue : d);
				day.put("submittedDate", submittedDateTime.toLocalDate());
				day.put("submittedDateTime", submittedDateTime.format(DATE_TIME_FORMATTER));
				day.put("totalScore", toIntValue(getColumnValue(r, 4, 3)));
				day.put("answeredQuestions", toIntValue(getColumnValue(r, 5, 4)));
				day.put("totalQuestions", toIntValue(getColumnValue(r, 6, 5)));
				day.put("perDayIncentive",
						perDayIncentiveBD.setScale(2, java.math.RoundingMode.HALF_UP).doubleValue());

				Map<String, Object> dayIncentiveData = dailyIncentiveMap.get(d);
				if (dayIncentiveData != null) {
					java.math.BigDecimal dayIncentiveAmt = (java.math.BigDecimal) dayIncentiveData.get("incentiveAmount");
					day.put("incentiveAmount", (int) Math.round(dayIncentiveAmt.doubleValue()));
					day.put("validWeightage", java.math.BigDecimal.valueOf(
							(Double) dayIncentiveData.get("validWeightage"))
							.setScale(2, java.math.RoundingMode.HALF_UP).doubleValue());
					day.put("totalWeightage", java.math.BigDecimal.valueOf(
							(Double) dayIncentiveData.get("totalWeightage"))
							.setScale(2, java.math.RoundingMode.HALF_UP).doubleValue());
				} else {
					day.put("incentiveAmount", 0);
					day.put("validWeightage", 0.0);
					day.put("totalWeightage", 0.0);
				}
				daily.add(day);
			}

			Map<String, Object> data = new HashMap<>();
			data.put("employeeId", employeeId);
			data.put("employeeName", employeeName);
			data.put("month", monthStr);
			data.put("daily", daily);
			data.put("totalIncentive", (int) Math.round(totalEmployeeIncentive.doubleValue()));
			data.put("employeeWorkingDays", daily.size());
			data.put("totalWorkingDays", orgWorkingDays);
			data.put("perDayIncentive",
					perDayIncentiveBD.setScale(2, java.math.RoundingMode.HALF_UP).doubleValue());
			data.put("totalValidWeightage", java.math.BigDecimal.valueOf(totalValidWeightage)
					.setScale(2, java.math.RoundingMode.HALF_UP).doubleValue());

			resp.put("success", true);
			resp.put("message", "Employee month progress fetched successfully");
			resp.put("data", data);
			return ResponseEntity.ok(resp);
		} catch (Exception e) {
			log.error("Error in getEmployeeMonthProgress", e);
			resp.put("success", false);
			resp.put("message", "Unable to fetch employee monthly progress right now. Please try again.");
			return ResponseEntity.internalServerError().body(resp);
		} finally {
			log.info("Exiting getEmployeeMonthProgress");
		}
	}

	@PostMapping("/calculate-employee-incentive")
	public ResponseEntity<?> calculateEmployeeIncentive(
			@RequestHeader("Authorization") String authHeader,
			@RequestBody Map<String, Object> payload) {

		log.info("Entered calculateEmployeeIncentive endpoint");
		Map<String, Object> resp = new HashMap<>();

		try {
			// Validate auth token
			Employee user = authUtil.getEmployeeFromToken(authHeader);
			if (user == null) {
				resp.put("success", false);
				resp.put("message", "Invalid or expired token");
				return ResponseEntity.status(401).body(resp);
			}

			// Extract parameters from payload
			Object orgIdObj = payload.get("orgId");
			Object fromDateObj = payload.get("fromDate");
			Object toDateObj = payload.get("toDate");
			Object branchIdsObj = payload.get("branchIds");
			Object allBranchesObj = payload.get("allBranches");
			Object staffIdObj = payload.get("staffId");
			Object staffNameObj = payload.get("staffName");
			String reportType = payload.get("reportType") != null ? payload.get("reportType").toString() : "summary";

			// Validate reportType
			if (!reportType.equalsIgnoreCase("summary") && !reportType.equalsIgnoreCase("daily")) {
				resp.put("success", false);
				resp.put("message", "Invalid reportType. Valid values are 'summary' or 'daily'");
				return ResponseEntity.badRequest().body(resp);
			}

			if (orgIdObj == null) {
				resp.put("success", false);
				resp.put("message", "Organization ID (orgId) is required");
				return ResponseEntity.badRequest().body(resp);
			}

			if (fromDateObj == null || toDateObj == null) {
				resp.put("success", false);
				resp.put("message", "From date (fromDate) and to date (toDate) are required");
				return ResponseEntity.badRequest().body(resp);
			}

			Integer orgId = null;
			LocalDate fromDate = null;
			LocalDate toDate = null;
			List<Integer> branchIds = new ArrayList<>();
			boolean allBranches = false;
			Integer staffId = null;
			String staffName = null;

			try {
				orgId = Integer.parseInt(orgIdObj.toString());
				fromDate = LocalDate.parse(fromDateObj.toString());
				toDate = LocalDate.parse(toDateObj.toString());

				// Parse branchIds (can be array or comma-separated string)
				if (branchIdsObj != null) {
					if (branchIdsObj instanceof List) {
						@SuppressWarnings("unchecked")
						List<Object> branchIdList = (List<Object>) branchIdsObj;
						for (Object id : branchIdList) {
							if (id != null) {
								branchIds.add(Integer.parseInt(id.toString()));
							}
						}
					} else if (branchIdsObj instanceof String) {
						String[] parts = branchIdsObj.toString().split(",");
						for (String part : parts) {
							String trimmed = part.trim();
							if (!trimmed.isEmpty()) {
								branchIds.add(Integer.parseInt(trimmed));
							}
						}
					}
				}

				// Parse allBranches
				if (allBranchesObj != null) {
					if (allBranchesObj instanceof Boolean) {
						allBranches = (Boolean) allBranchesObj;
					} else {
						allBranches = Boolean.parseBoolean(allBranchesObj.toString());
					}
				}

				// Parse optional staff filter. Apply only when both staffId and staffName are provided.
				if (staffIdObj != null && staffNameObj != null) {
					String staffNameValue = staffNameObj.toString().trim();
					if (!staffNameValue.isEmpty()) {
						staffId = Integer.parseInt(staffIdObj.toString());
						staffName = staffNameValue;
					}
				}

				log.info(
						"Calculating incentive - orgId: {}, fromDate: {}, toDate: {}, branchIds: {}, allBranches: {}, staffId: {}, staffName: {}",
						orgId, fromDate, toDate, branchIds, allBranches, staffId, staffName);

			} catch (Exception ex) {
				log.error("Error parsing request parameters", ex);
				resp.put("success", false);
				resp.put("message",
						"Invalid parameter format. orgId (integer), fromDate and toDate (yyyy-MM-dd), branchIds (array of integers)");
				return ResponseEntity.badRequest().body(resp);
			}

			// Validate organization exists and is active
			java.util.Optional<com.spearhead.ufc.model.Org> orgOpt = orgRepository.findById(orgId);
			if (orgOpt.isEmpty()) {
				resp.put("success", false);
				resp.put("message", "Organization not found");
				return ResponseEntity.badRequest().body(resp);
			}
			com.spearhead.ufc.model.Org org = orgOpt.get();
			if (org.getIsActive() != null && !org.getIsActive()) {
				resp.put("success", false);
				resp.put("message", "Organization is inactive");
				return ResponseEntity.badRequest().body(resp);
			}

			// Generate Excel file based on report type
			String filePath;
			String reportName;
			if (reportType.equalsIgnoreCase("daily")) {
				// Daily progress report - each day's progress for all employees
				log.info("Generating daily progress report");
				reportName = "Monthly Progress Detail";
				filePath = dashboardSummaryService.generateDailyProgressReportExcel(
						orgId, fromDate, toDate, branchIds, allBranches, staffId, staffName);
			} else {
				// Summary report - month-based data per user (default)
				log.info("Generating summary report");
				reportName = "Monthly Progress Summary";
				filePath = dashboardSummaryService.generateIncentiveReportExcel(
						orgId, fromDate, toDate, branchIds, allBranches, staffId, staffName);
			}

			// Extract filename from path
			java.io.File file = new java.io.File(filePath);
			String filename = file.getName();

			log.info("Excel file saved to: {}", filePath);
			
			resp.put("success", true);
			resp.put("message", "Excel report generated successfully");
			resp.put("filePath", filePath);
			resp.put("filename", filename);
			resp.put("reportName", reportName);
			resp.put("downloadUrl", "/dashboardSummary/download-report/" + filename);
			return ResponseEntity.ok(resp);

		} catch (Exception e) {
			log.error("Error in calculateEmployeeIncentive", e);
			resp.put("success", false);
			resp.put("message", "Unable to generate employee incentive report right now. Please try again.");
			return ResponseEntity.internalServerError().body(resp);
		} finally {
			log.info("Exiting calculateEmployeeIncentive endpoint");
		}
	}

	@PostMapping("/download-incentive-report")
	public ResponseEntity<?> downloadIncentiveReport(
			@RequestHeader("Authorization") String authHeader,
			@RequestBody Map<String, Object> payload) {

		log.info("Entered downloadIncentiveReport endpoint");

		try {
			// Validate auth token
			Employee user = authUtil.getEmployeeFromToken(authHeader);
			if (user == null) {
				Map<String, Object> resp = new HashMap<>();
				resp.put("success", false);
				resp.put("message", "Invalid or expired token");
				return ResponseEntity.status(401).body(resp);
			}

			// Extract parameters from payload
			Object orgIdObj = payload.get("orgId");
			Object fromDateObj = payload.get("fromDate");
			Object toDateObj = payload.get("toDate");
			Object branchIdsObj = payload.get("branchIds");
			Object allBranchesObj = payload.get("allBranches");
			Object staffIdObj = payload.get("staffId");
			Object staffNameObj = payload.get("staffName");
			String reportType = (payload.get("reportType") != null) ? payload.get("reportType").toString() : "summary";

			if (orgIdObj == null || fromDateObj == null || toDateObj == null) {
				Map<String, Object> resp = new HashMap<>();
				resp.put("success", false);
				resp.put("message", "orgId, fromDate, and toDate are required");
				return ResponseEntity.badRequest().body(resp);
			}

			Integer orgId = Integer.parseInt(orgIdObj.toString());
			LocalDate fromDate = LocalDate.parse(fromDateObj.toString());
			LocalDate toDate = LocalDate.parse(toDateObj.toString());
			List<Integer> branchIds = new ArrayList<>();
			boolean allBranches = false;
			Integer staffId = null;
			String staffName = null;

			// Parse branchIds
			if (branchIdsObj != null) {
				if (branchIdsObj instanceof List) {
					@SuppressWarnings("unchecked")
					List<Object> branchIdList = (List<Object>) branchIdsObj;
					for (Object id : branchIdList) {
						if (id != null) {
							branchIds.add(Integer.parseInt(id.toString()));
						}
					}
				}
			}

			// Parse allBranches
			if (allBranchesObj != null) {
				if (allBranchesObj instanceof Boolean) {
					allBranches = (Boolean) allBranchesObj;
				} else {
					allBranches = Boolean.parseBoolean(allBranchesObj.toString());
				}
			}

			// Parse optional staff filter. Apply only when both staffId and staffName are provided.
			if (staffIdObj != null && staffNameObj != null) {
				String staffNameValue = staffNameObj.toString().trim();
				if (!staffNameValue.isEmpty()) {
					staffId = Integer.parseInt(staffIdObj.toString());
					staffName = staffNameValue;
				}
			}

			// Generate Excel file and save to disk
			String filePath;
			if ("daily".equalsIgnoreCase(reportType)) {
				filePath = dashboardSummaryService.generateDailyProgressReportExcel(
						orgId, fromDate, toDate, branchIds, allBranches, staffId, staffName);
			} else {
				filePath = dashboardSummaryService.generateIncentiveReportExcel(
						orgId, fromDate, toDate, branchIds, allBranches, staffId, staffName);
			}

			// Read the file into memory then delete it so disk space is freed immediately
			java.io.File file = new java.io.File(filePath);
			byte[] excelBytes = java.nio.file.Files.readAllBytes(file.toPath());
			try {
				java.nio.file.Files.deleteIfExists(file.toPath());
			} catch (Exception delEx) {
				log.warn("Could not delete report file after serving: {}", file.getAbsolutePath());
			}

			// Set headers for file download
			String filename = file.getName();
			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(
					MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
			headers.setContentDispositionFormData("attachment", filename);
			headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");

			log.info("Excel file ready for download and deleted from disk: {}", filename);
			return ResponseEntity.ok()
					.headers(headers)
					.body(excelBytes);

		} catch (Exception e) {
			log.error("Error in downloadIncentiveReport", e);
			Map<String, Object> resp = new HashMap<>();
			resp.put("success", false);
			resp.put("message", "Unable to generate incentive report right now. Please try again.");
			return ResponseEntity.internalServerError().body(resp);
		} finally {
			log.info("Exiting downloadIncentiveReport endpoint");
		}
	}

	@PostMapping("/generate-staff-details-report")
	public ResponseEntity<?> generateStaffDetailsReport(
			@RequestHeader("Authorization") String authHeader,
			@RequestBody Map<String, Object> payload) {

		log.info("Entered generateStaffDetailsReport endpoint");
		Map<String, Object> resp = new HashMap<>();

		try {
			Employee user = authUtil.getEmployeeFromToken(authHeader);
			if (user == null) {
				resp.put("success", false);
				resp.put("message", "Invalid or expired token");
				return ResponseEntity.status(401).body(resp);
			}

			Object orgIdObj = payload.get("orgId");
			Object roleIdObj = payload.get("roleId");
			Object fromDateObj = payload.get("fromDate");
			Object toDateObj = payload.get("toDate");
			Object branchIdsObj = payload.get("branchIds");
			Object allBranchesObj = payload.get("allBranches");

			if (orgIdObj == null) {
				resp.put("success", false);
				resp.put("message", "Organization ID (orgId) is required");
				return ResponseEntity.badRequest().body(resp);
			}

			Integer orgId;
			Integer roleId = null;
			LocalDate fromDate = null;
			LocalDate toDate = null;
			List<Integer> branchIds = new ArrayList<>();
			boolean allBranches = false;

			try {
				orgId = Integer.parseInt(orgIdObj.toString());
				if (roleIdObj != null && !roleIdObj.toString().trim().isEmpty()) {
					roleId = Integer.parseInt(roleIdObj.toString().trim());
				}
			
				if (fromDateObj != null && !fromDateObj.toString().trim().isEmpty()) {
					fromDate = LocalDate.parse(fromDateObj.toString().trim());
				}
				if (toDateObj != null && !toDateObj.toString().trim().isEmpty()) {
					toDate = LocalDate.parse(toDateObj.toString().trim());
				}
				if ((fromDate == null) != (toDate == null)) {
					resp.put("success", false);
					resp.put("message", "Both fromDate and toDate should be provided together");
					return ResponseEntity.badRequest().body(resp);
				}

				if (branchIdsObj != null) {
					if (branchIdsObj instanceof List) {
						@SuppressWarnings("unchecked")
						List<Object> branchIdList = (List<Object>) branchIdsObj;
						for (Object id : branchIdList) {
							if (id != null) {
								branchIds.add(Integer.parseInt(id.toString()));
							}
						}
					} else if (branchIdsObj instanceof String) {
						String[] parts = branchIdsObj.toString().split(",");
						for (String part : parts) {
							String trimmed = part.trim();
							if (!trimmed.isEmpty()) {
								branchIds.add(Integer.parseInt(trimmed));
							}
						}
					}
				}

				if (allBranchesObj != null) {
					if (allBranchesObj instanceof Boolean) {
						allBranches = (Boolean) allBranchesObj;
					} else {
						allBranches = Boolean.parseBoolean(allBranchesObj.toString());
					}
				}
			} catch (Exception ex) {
				log.error("Error parsing staff details report request", ex);
				resp.put("success", false);
				resp.put("message",
						"Invalid parameter format. orgId/roleId (integer), branchIds (array or comma-separated)");
				return ResponseEntity.badRequest().body(resp);
			}

			java.util.Optional<com.spearhead.ufc.model.Org> orgOpt = orgRepository.findById(orgId);
			if (orgOpt.isEmpty()) {
				resp.put("success", false);
				resp.put("message", "Organization not found");
				return ResponseEntity.badRequest().body(resp);
			}

			com.spearhead.ufc.model.Org org = orgOpt.get();
			if (org.getIsActive() != null && !org.getIsActive()) {
				resp.put("success", false);
				resp.put("message", "Organization is inactive");
				return ResponseEntity.badRequest().body(resp);
			}

			String filePath = dashboardSummaryService.generateStaffDetailsReportExcel(orgId, fromDate, toDate, branchIds,
					allBranches, roleId);

			java.io.File file = new java.io.File(filePath);
			String filename = file.getName();

			resp.put("success", true);
			resp.put("message", "Staff details report generated successfully");
			resp.put("filePath", filePath);
			resp.put("filename", filename);
			resp.put("reportName", "Staff details");
			resp.put("downloadUrl", "/dashboardSummary/download-report/" + filename);
			return ResponseEntity.ok(resp);

		} catch (Exception e) {
			log.error("Error in generateStaffDetailsReport", e);
			resp.put("success", false);
			resp.put("message", "Unable to generate staff details report right now. Please try again.");
			return ResponseEntity.internalServerError().body(resp);
		} finally {
			log.info("Exiting generateStaffDetailsReport endpoint");
		}
	}

	@PostMapping("/download-staff-details-report")
	public ResponseEntity<?> downloadStaffDetailsReport(
			@RequestHeader("Authorization") String authHeader,
			@RequestBody Map<String, Object> payload) {

		log.info("Entered downloadStaffDetailsReport endpoint");

		try {
			Employee user = authUtil.getEmployeeFromToken(authHeader);
			if (user == null) {
				Map<String, Object> resp = new HashMap<>();
				resp.put("success", false);
				resp.put("message", "Invalid or expired token");
				return ResponseEntity.status(401).body(resp);
			}

			Object filenameObj = payload.get("filename");
			Object filePathObj = payload.get("filePath");
			if ((filenameObj != null && !filenameObj.toString().trim().isEmpty())
					|| (filePathObj != null && !filePathObj.toString().trim().isEmpty())) {
				String filename = null;
				if (filenameObj != null && !filenameObj.toString().trim().isEmpty()) {
					filename = filenameObj.toString().trim();
				} else {
					filename = java.nio.file.Paths.get(filePathObj.toString().trim()).getFileName().toString();
				}

				if (filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
					Map<String, Object> resp = new HashMap<>();
					resp.put("success", false);
					resp.put("message", "Invalid filename");
					return ResponseEntity.badRequest().body(resp);
				}

				java.nio.file.Path safePath = java.nio.file.Paths.get("reports", filename);
				java.io.File existingFile = safePath.toFile();
				if (!existingFile.exists() || !existingFile.isFile()) {
					Map<String, Object> resp = new HashMap<>();
					resp.put("success", false);
					resp.put("message", "Requested report file not found");
					return ResponseEntity.status(404).body(resp);
				}

				byte[] excelBytes = java.nio.file.Files.readAllBytes(existingFile.toPath());
				try {
					java.nio.file.Files.deleteIfExists(existingFile.toPath());
				} catch (Exception delEx) {
					log.warn("Could not delete staff details report file after serving: {}", existingFile.getAbsolutePath());
				}
				HttpHeaders headers = new HttpHeaders();
				headers.setContentType(
						MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
				headers.setContentDispositionFormData("attachment", existingFile.getName());
				headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");

				log.info("Returning existing staff details report and deleted from disk: {}", existingFile.getName());
				return ResponseEntity.ok().headers(headers).body(excelBytes);
			}

			Object orgIdObj = payload.get("orgId");
			Object roleIdObj = payload.get("roleId");
			Object fromDateObj = payload.get("fromDate");
			Object toDateObj = payload.get("toDate");
			Object branchIdsObj = payload.get("branchIds");
			Object allBranchesObj = payload.get("allBranches");

			if (orgIdObj == null) {
				Map<String, Object> resp = new HashMap<>();
				resp.put("success", false);
				resp.put("message", "orgId is required");
				return ResponseEntity.badRequest().body(resp);
			}

			Integer orgId = Integer.parseInt(orgIdObj.toString());
			Integer roleId = null;
			LocalDate fromDate = null;
			LocalDate toDate = null;
			List<Integer> branchIds = new ArrayList<>();
			boolean allBranches = false;
			if (roleIdObj != null && !roleIdObj.toString().trim().isEmpty()) {
				roleId = Integer.parseInt(roleIdObj.toString().trim());
			}
			if (roleId == null) {
				Map<String, Object> resp = new HashMap<>();
				resp.put("success", false);
				resp.put("message", "roleId is required for regeneration. Pass filename/filePath to download existing report.");
				return ResponseEntity.badRequest().body(resp);
			}

			if (branchIdsObj != null) {
				if (branchIdsObj instanceof List) {
					@SuppressWarnings("unchecked")
					List<Object> branchIdList = (List<Object>) branchIdsObj;
					for (Object id : branchIdList) {
						if (id != null) {
							branchIds.add(Integer.parseInt(id.toString()));
						}
					}
				} else if (branchIdsObj instanceof String) {
					String[] parts = branchIdsObj.toString().split(",");
					for (String part : parts) {
						String trimmed = part.trim();
						if (!trimmed.isEmpty()) {
							branchIds.add(Integer.parseInt(trimmed));
						}
					}
				}
			}

			if (allBranchesObj != null) {
				if (allBranchesObj instanceof Boolean) {
					allBranches = (Boolean) allBranchesObj;
				} else {
					allBranches = Boolean.parseBoolean(allBranchesObj.toString());
				}
			}

			log.warn("download-staff-details-report called without filename/filePath; regenerating report. orgId={}, roleId={}, branchIds={}, allBranches={}",
					orgId, roleId, branchIds, allBranches);
			String filePath = dashboardSummaryService.generateStaffDetailsReportExcel(orgId, fromDate, toDate, branchIds,
					allBranches, roleId);

			java.io.File file = new java.io.File(filePath);
			byte[] excelBytes = java.nio.file.Files.readAllBytes(file.toPath());
			try {
				java.nio.file.Files.deleteIfExists(file.toPath());
			} catch (Exception delEx) {
				log.warn("Could not delete staff details report file after serving: {}", file.getAbsolutePath());
			}

			String filename = file.getName();
			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(
					MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
			headers.setContentDispositionFormData("attachment", filename);
			headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");

			log.info("Staff details file ready for download and deleted from disk: {}", filename);
			return ResponseEntity.ok().headers(headers).body(excelBytes);

		} catch (Exception e) {
			log.error("Error in downloadStaffDetailsReport", e);
			Map<String, Object> resp = new HashMap<>();
			resp.put("success", false);
			resp.put("message", "Unable to generate staff details report right now. Please try again.");
			return ResponseEntity.internalServerError().body(resp);
		} finally {
			log.info("Exiting downloadStaffDetailsReport endpoint");
		}
	}

	@PostMapping("/generate-staff-details-report-by-role")
	public ResponseEntity<?> generateStaffDetailsReportByRole(
			@RequestHeader("Authorization") String authHeader,
			@RequestBody Map<String, Object> payload) {

		log.info("Entered generateStaffDetailsReportByRole endpoint");
		Map<String, Object> resp = new HashMap<>();

		try {
			Employee user = authUtil.getEmployeeFromToken(authHeader);
			if (user == null) {
				resp.put("success", false);
				resp.put("message", "Invalid or expired token");
				return ResponseEntity.status(401).body(resp);
			}

			Object orgIdObj = payload.get("orgId");
			Object roleIdObj = payload.get("roleId");
			Object fromDateObj = payload.get("fromDate");
			Object toDateObj = payload.get("toDate");

			Object branchIdsObj = payload.get("branchIds");
			Object allBranchesObj = payload.get("allBranches");

			if (orgIdObj == null) {
				resp.put("success", false);
				resp.put("message", "Organization ID (orgId) is required");
				return ResponseEntity.badRequest().body(resp);
			}

			if (roleIdObj == null) {
				resp.put("success", false);
				resp.put("message", "roleId is required");
				return ResponseEntity.badRequest().body(resp);
			}

			Integer orgId;
			Integer roleId;
			LocalDate fromDate = null;
			LocalDate toDate = null;
			List<Integer> branchIds = new ArrayList<>();
			boolean allBranches = false;

			try {
				orgId = Integer.parseInt(orgIdObj.toString());
				roleId = Integer.parseInt(roleIdObj.toString());
				if (fromDateObj != null && !fromDateObj.toString().trim().isEmpty()) {
					fromDate = LocalDate.parse(fromDateObj.toString().trim());
				}
				if (toDateObj != null && !toDateObj.toString().trim().isEmpty()) {
					toDate = LocalDate.parse(toDateObj.toString().trim());
				}
				
				if ((fromDate == null) != (toDate == null)) {
					resp.put("success", false);
					resp.put("message", "Both fromDate and toDate should be provided together");
					return ResponseEntity.badRequest().body(resp);
				}

				if (branchIdsObj != null) {
					if (branchIdsObj instanceof List) {
						@SuppressWarnings("unchecked")
						List<Object> branchIdList = (List<Object>) branchIdsObj;
						for (Object id : branchIdList) {
							if (id != null) {
								branchIds.add(Integer.parseInt(id.toString()));
							}
						}
					} else if (branchIdsObj instanceof String) {
						String[] parts = branchIdsObj.toString().split(",");
						for (String part : parts) {
							String trimmed = part.trim();
							if (!trimmed.isEmpty()) {
								branchIds.add(Integer.parseInt(trimmed));
							}
						}
					}
				}

				if (allBranchesObj != null) {
					if (allBranchesObj instanceof Boolean) {
						allBranches = (Boolean) allBranchesObj;
					} else {
						allBranches = Boolean.parseBoolean(allBranchesObj.toString());
					}
				}
			} catch (Exception ex) {
				log.error("Error parsing role-based staff details report request", ex);
				resp.put("success", false);
				resp.put("message",
						"Invalid parameter format. orgId/roleId (integer), fromDate/toDate (yyyy-MM-dd), branchIds (array or comma-separated)");
				return ResponseEntity.badRequest().body(resp);
			}

			java.util.Optional<com.spearhead.ufc.model.Org> orgOpt = orgRepository.findById(orgId);
			if (orgOpt.isEmpty()) {
				resp.put("success", false);
				resp.put("message", "Organization not found");
				return ResponseEntity.badRequest().body(resp);
			}

			com.spearhead.ufc.model.Org org = orgOpt.get();
			if (org.getIsActive() != null && !org.getIsActive()) {
				resp.put("success", false);
				resp.put("message", "Organization is inactive");
				return ResponseEntity.badRequest().body(resp);
			}

			String filePath = dashboardSummaryService.generateStaffDetailsReportExcel(
					orgId, fromDate, toDate, branchIds, allBranches, roleId);

			java.io.File file = new java.io.File(filePath);
			String filename = file.getName();

			resp.put("success", true);
			resp.put("message", "Role-based staff details report generated successfully");
			resp.put("filePath", filePath);
			resp.put("filename", filename);
			resp.put("reportName", "Staff details");
			resp.put("downloadUrl", "/dashboardSummary/download-report/" + filename);
			return ResponseEntity.ok(resp);

		} catch (Exception e) {
			log.error("Error in generateStaffDetailsReportByRole", e);
			resp.put("success", false);
			resp.put("message", "Unable to generate role-based staff details report right now. Please try again.");
			return ResponseEntity.internalServerError().body(resp);
		} finally {
			log.info("Exiting generateStaffDetailsReportByRole endpoint");
		}
	}

	/**
	 * GET endpoint to download a report file from the reports folder.
	 * Chrome can download files directly from this endpoint.
	 */
	@GetMapping("/download-report/{filename}")
	public ResponseEntity<?> downloadReportFile(@PathVariable String filename) {
		log.info("Entered downloadReportFile endpoint for file: {}", filename);

		try {
			// Security check: prevent directory traversal attacks
			if (filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
				Map<String, Object> resp = new HashMap<>();
				resp.put("success", false);
				resp.put("message", "Invalid filename");
				return ResponseEntity.badRequest().body(resp);
			}

			// Build the file path
			java.nio.file.Path filePath = java.nio.file.Paths.get("reports", filename);
			java.io.File file = filePath.toFile();

			if (!file.exists() || !file.isFile()) {
				Map<String, Object> resp = new HashMap<>();
				resp.put("success", false);
				resp.put("message", "File not found");
				return ResponseEntity.status(404).body(resp);
			}

			// Read file bytes into memory then delete from disk to free space
			byte[] fileBytes = java.nio.file.Files.readAllBytes(filePath);
			try {
				java.nio.file.Files.deleteIfExists(filePath);
			} catch (Exception delEx) {
				log.warn("Could not delete report file after serving: {}", file.getAbsolutePath());
			}

			// Set headers for file download
			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(
					MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
			headers.setContentDispositionFormData("attachment", filename);
			headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");

			log.info("File served and deleted from disk: {}", filename);
			return ResponseEntity.ok()
					.headers(headers)
					.body(fileBytes);

		} catch (Exception e) {
			log.error("Error in downloadReportFile", e);
			Map<String, Object> resp = new HashMap<>();
			resp.put("success", false);
			resp.put("message", "Failed to download file: " + e.getMessage());
			return ResponseEntity.internalServerError().body(resp);
		} finally {
			log.info("Exiting downloadReportFile endpoint");
		}
	}

	/**
	 * Submit a report generation request to the JMS queue.
	 * Returns a requestId immediately; the report is generated asynchronously.
	 * Poll /report-status/{requestId} to check completion, then download via
	 * /download-report/{filename}.
	 */
	@PostMapping("/queue-report")
	public ResponseEntity<?> queueReport(
			@RequestHeader("Authorization") String authHeader,
			@RequestBody Map<String, Object> payload) {

		log.info("Entered queueReport endpoint");
		Map<String, Object> resp = new HashMap<>();

		try {
			Employee user = authUtil.getEmployeeFromToken(authHeader);
			if (user == null) {
				resp.put("success", false);
				resp.put("message", "Invalid or expired token");
				return ResponseEntity.status(401).body(resp);
			}

			Object orgIdObj = payload.get("orgId");
			Object fromDateObj = payload.get("fromDate");
			Object toDateObj = payload.get("toDate");
			Object branchIdsObj = payload.get("branchIds");
			Object allBranchesObj = payload.get("allBranches");
			Object staffIdObj = payload.get("staffId");
			Object staffNameObj = payload.get("staffName");
			String reportType = payload.get("reportType") != null ? payload.get("reportType").toString() : "summary";

			if (orgIdObj == null || fromDateObj == null || toDateObj == null) {
				resp.put("success", false);
				resp.put("message", "orgId, fromDate and toDate are required");
				return ResponseEntity.badRequest().body(resp);
			}

			Integer orgId = Integer.parseInt(orgIdObj.toString());
			LocalDate fromDate = LocalDate.parse(fromDateObj.toString());
			LocalDate toDate = LocalDate.parse(toDateObj.toString());
			List<Integer> branchIds = new ArrayList<>();
			boolean allBranches = false;
			Integer staffId = null;
			String staffName = null;

			if (branchIdsObj instanceof List) {
				@SuppressWarnings("unchecked")
				List<Object> raw = (List<Object>) branchIdsObj;
				for (Object id : raw) {
					if (id != null) branchIds.add(Integer.parseInt(id.toString()));
				}
			}
			if (allBranchesObj instanceof Boolean) {
				allBranches = (Boolean) allBranchesObj;
			} else if (allBranchesObj != null) {
				allBranches = Boolean.parseBoolean(allBranchesObj.toString());
			}
			if (staffIdObj != null && staffNameObj != null) {
				String sn = staffNameObj.toString().trim();
				if (!sn.isEmpty()) {
					staffId = Integer.parseInt(staffIdObj.toString());
					staffName = sn;
				}
			}

			ReportRequest request = new ReportRequest();
			request.setRequestId(UUID.randomUUID().toString());
			request.setOrgId(orgId);
			request.setFromDate(fromDate);
			request.setToDate(toDate);
			request.setBranchIds(branchIds);
			request.setAllBranches(allBranches);
			request.setStaffId(staffId);
			request.setStaffName(staffName);
			request.setReportType(reportType);
			request.setRequestedByEmployeeId(user.getEmployeeId());

			reportQueueProducer.enqueue(request);

			resp.put("success", true);
			resp.put("message", "Report queued for generation");
			resp.put("requestId", request.getRequestId());
			resp.put("statusUrl", "/dashboardSummary/report-status/" + request.getRequestId());
			return ResponseEntity.accepted().body(resp);

		} catch (Exception e) {
			log.error("Error in queueReport", e);
			resp.put("success", false);
			resp.put("message", "Failed to queue report: " + e.getMessage());
			return ResponseEntity.internalServerError().body(resp);
		} finally {
			log.info("Exiting queueReport endpoint");
		}
	}

	/**
	 * Poll the status of a previously queued report.
	 * Status values: PENDING | IN_PROGRESS | COMPLETED | FAILED
	 * When COMPLETED, the response includes 'filename' and 'downloadUrl'.
	 */
	@GetMapping("/report-status/{requestId}")
	public ResponseEntity<?> getReportStatus(
			@RequestHeader("Authorization") String authHeader,
			@PathVariable String requestId) {

		log.info("Entered getReportStatus endpoint - requestId={}", requestId);
		Map<String, Object> resp = new HashMap<>();

		try {
			Employee user = authUtil.getEmployeeFromToken(authHeader);
			if (user == null) {
				resp.put("success", false);
				resp.put("message", "Invalid or expired token");
				return ResponseEntity.status(401).body(resp);
			}

			ReportStatusStore.ReportResult result = reportStatusStore.get(requestId);
			if (result == null) {
				resp.put("success", false);
				resp.put("message", "Report request not found");
				return ResponseEntity.status(404).body(resp);
			}

			resp.put("success", true);
			resp.put("requestId", requestId);
			resp.put("status", result.getStatus().name());

			if (result.getStatus() == ReportStatusStore.Status.COMPLETED) {
				resp.put("filename", result.getFilename());
				resp.put("downloadUrl", result.getDownloadUrl());
			} else if (result.getStatus() == ReportStatusStore.Status.FAILED) {
				resp.put("error", result.getErrorMessage());
			}

			return ResponseEntity.ok(resp);

		} catch (Exception e) {
			log.error("Error in getReportStatus - requestId={}", requestId, e);
			resp.put("success", false);
			resp.put("message", "Failed to fetch report status: " + e.getMessage());
			return ResponseEntity.internalServerError().body(resp);
		} finally {
			log.info("Exiting getReportStatus endpoint - requestId={}", requestId);
		}
	}

	/**
	 * Queue an async Compliance Incentive (weighted-daily) report.
	 *
	 * Filters:
	 *   allBranches=true  → resolve all branches the token-user has access to
	 *   branchIds         → one or more specific branch IDs
	 *   employeeId        → single specific employee
	 *   fromDate / toDate → submitted_at range (yyyy-MM-dd)
	 *
	 * Returns a requestId immediately. Poll /report-status/{requestId} to check progress,
	 * then download via /download-report/{filename} once COMPLETED.
	 */
	@PostMapping("/queue-weighted-report")
	public ResponseEntity<?> queueWeightedReport(
			@RequestHeader("Authorization") String authHeader,
			@RequestBody Map<String, Object> payload) {

		log.info("Entered queueWeightedReport endpoint");
		Map<String, Object> resp = new HashMap<>();

		try {
			Employee user = authUtil.getEmployeeFromToken(authHeader);
			if (user == null) {
				resp.put("success", false);
				resp.put("message", "Invalid or expired token");
				return ResponseEntity.status(401).body(resp);
			}

			// Determine orgId: non-super-admin always uses their own org
			String roleName = user.getRoleId() != null ? user.getRoleId().getRoleName() : null;
			boolean isSuperAdmin = roleName != null && roleName.toUpperCase().contains("SUPER");

			Object orgIdObj = payload.get("orgId");
			Integer orgId;
			if (isSuperAdmin && orgIdObj != null) {
				orgId = Integer.parseInt(orgIdObj.toString());
			} else {
				orgId = user.getOrg() != null ? user.getOrg().getOrgId() : null;
			}
			if (orgId == null) {
				resp.put("success", false);
				resp.put("message", "Organization could not be determined");
				return ResponseEntity.badRequest().body(resp);
			}

			// Validate org
			java.util.Optional<com.spearhead.ufc.model.Org> orgOpt = orgRepository.findById(orgId);
			if (orgOpt.isEmpty()) {
				resp.put("success", false);
				resp.put("message", "Organization not found");
				return ResponseEntity.badRequest().body(resp);
			}
			if (Boolean.FALSE.equals(orgOpt.get().getIsActive())) {
				resp.put("success", false);
				resp.put("message", "Organization is inactive");
				return ResponseEntity.badRequest().body(resp);
			}

			// Parse mandatory date range
			Object fromDateObj = payload.get("fromDate");
			Object toDateObj = payload.get("toDate");
			if (fromDateObj == null || toDateObj == null) {
				resp.put("success", false);
				resp.put("message", "fromDate and toDate are required (yyyy-MM-dd)");
				return ResponseEntity.badRequest().body(resp);
			}
			LocalDate fromDate = LocalDate.parse(fromDateObj.toString());
			LocalDate toDate = LocalDate.parse(toDateObj.toString());
			if (fromDate.isAfter(toDate)) {
				resp.put("success", false);
				resp.put("message", "fromDate must not be after toDate");
				return ResponseEntity.badRequest().body(resp);
			}

			// Parse optional filters
			Object allBranchesObj = payload.get("allBranches");
			boolean allBranches = allBranchesObj instanceof Boolean ? (Boolean) allBranchesObj
					: allBranchesObj != null && Boolean.parseBoolean(allBranchesObj.toString());

			Object employeeIdObj = payload.get("employeeId");
			Integer staffId = (employeeIdObj != null && !employeeIdObj.toString().trim().isEmpty())
					? Integer.parseInt(employeeIdObj.toString().trim()) : null;

			Object branchIdsObj = payload.get("branchIds");
			List<Integer> branchIds = new ArrayList<>();
			if (branchIdsObj instanceof List) {
				@SuppressWarnings("unchecked")
				List<Object> raw = (List<Object>) branchIdsObj;
				for (Object id : raw) {
					if (id != null) branchIds.add(Integer.parseInt(id.toString()));
				}
			}

			// If allBranches=true, resolve every branch the token-user is authorised for.
			// This replaces whatever branchIds was passed.
			if (allBranches) {
				Set<Integer> authorizedBranchIds = new LinkedHashSet<>();
				if (user.getLocation() != null && user.getLocation().getLocationId() != null) {
					authorizedBranchIds.add(user.getLocation().getLocationId());
				}
				List<com.spearhead.ufc.model.EmployeeLocationAccess> accessList =
						employeeLocationAccessRepository.findByEmployeeEmployeeIdAndIsActiveTrue(user.getEmployeeId());
				if (accessList != null) {
					for (com.spearhead.ufc.model.EmployeeLocationAccess access : accessList) {
						if (access.getLocation() != null && access.getLocation().getLocationId() != null) {
							authorizedBranchIds.add(access.getLocation().getLocationId());
						}
					}
				}
				// Super-admin with allBranches=true and no access rows → treat as unrestricted
				if (!authorizedBranchIds.isEmpty()) {
					branchIds = new ArrayList<>(authorizedBranchIds);
					allBranches = false; // branchIds list is now explicit
				}
				log.info("Resolved authorized branches for user {}: {}", user.getEmployeeId(), branchIds);
			}

			// Build and enqueue the JMS request
			ReportRequest request = new ReportRequest();
			request.setRequestId(UUID.randomUUID().toString());
			request.setOrgId(orgId);
			request.setFromDate(fromDate);
			request.setToDate(toDate);
			request.setBranchIds(branchIds);
			request.setAllBranches(allBranches);
			request.setStaffId(staffId);
			request.setReportType("weighted-daily");
			request.setRequestedByEmployeeId(user.getEmployeeId());

			reportQueueProducer.enqueue(request);

			log.info("Compliance Incentive report queued - requestId={}, orgId={}, from={}, to={}, branches={}, staffId={}",
					request.getRequestId(), orgId, fromDate, toDate, branchIds, staffId);

			resp.put("success", true);
			resp.put("message", "Compliance Incentive report queued for generation");
			resp.put("requestId", request.getRequestId());
			resp.put("statusUrl", "/dashboardSummary/report-status/" + request.getRequestId());
			return ResponseEntity.accepted().body(resp);

		} catch (Exception e) {
			log.error("Error in queueWeightedReport", e);
			resp.put("success", false);
			resp.put("message", "Failed to queue report: " + e.getMessage());
			return ResponseEntity.internalServerError().body(resp);
		} finally {
			log.info("Exiting queueWeightedReport endpoint");
		}
	}
}
