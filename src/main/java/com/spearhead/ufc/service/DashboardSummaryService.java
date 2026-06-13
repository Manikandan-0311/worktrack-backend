package com.spearhead.ufc.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.spearhead.ufc.dto.DashboardDTO;
import com.spearhead.ufc.model.DashboardSummary;
import com.spearhead.ufc.model.Employee;
import com.spearhead.ufc.model.Org;
import com.spearhead.ufc.repository.DashboardSummaryRepository;
import com.spearhead.ufc.repository.EmployeeRepository;
import com.spearhead.ufc.repository.CalendarRepository;
import com.spearhead.ufc.repository.SubmissionRepository;
import com.spearhead.ufc.repository.OrgRepository;
import com.spearhead.ufc.repository.LocationRepository;
import com.spearhead.ufc.repository.EmployeeLocationAccessRepository;
import com.spearhead.ufc.model.Location;
import com.spearhead.ufc.model.EmployeeLocationAccess;
import com.spearhead.ufc.utils.DashboardSummaryProjection;
import com.spearhead.ufc.model.Submission;
import com.spearhead.ufc.model.Option;
import com.spearhead.ufc.repository.OptionRepository;
import java.util.stream.Collectors;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.DefaultIndexedColorMap;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.awt.Color;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.AbstractMap.SimpleEntry;

@Service
public class DashboardSummaryService {

	private static final Logger log = LoggerFactory.getLogger(DashboardSummaryService.class);

	/** Delete report files older than 10 minutes every 5 minutes. This cleans up
	 *  any files from generate-only endpoints that were never downloaded. */
	@Scheduled(fixedDelay = 5 * 60 * 1000)
	public void cleanupOldReportFiles() {
		Path reportsDir = Paths.get("reports");
		if (!Files.exists(reportsDir)) {
			return;
		}
		long tenMinutesAgo = System.currentTimeMillis() - (10 * 60 * 1000);
		try (var stream = Files.list(reportsDir)) {
			stream.filter(p -> p.toString().endsWith(".xlsx")).forEach(p -> {
				try {
					FileTime lastModified = Files.getLastModifiedTime(p);
					if (lastModified.toMillis() < tenMinutesAgo) {
						Files.deleteIfExists(p);
						log.info("Scheduled cleanup deleted old report file: {}", p.getFileName());
					}
				} catch (IOException ex) {
					log.warn("Scheduled cleanup could not delete file {}: {}", p.getFileName(), ex.getMessage());
				}
			});
		} catch (IOException ex) {
			log.warn("Scheduled cleanup error listing reports directory: {}", ex.getMessage());
		}
	}

	@Autowired
	private DashboardSummaryRepository dashboardSummaryRepository;

	@Autowired
	private EmployeeRepository employeeRepository;

	@Autowired
	private CalendarRepository calendarRepository;

	@Autowired
	private SubmissionRepository submissionRepository;

	@Autowired
	private OrgRepository orgRepository;

	@Autowired
	private LocationRepository locationRepository;

	@Autowired
	private EmployeeLocationAccessRepository employeeLocationAccessRepository;

	@Autowired
	private OptionRepository optionRepository;

	/**
	 * Fetch dashboard summary based on filters
	 */
	public List<DashboardSummary> getSummaryByFilter(Integer orgId, Optional<LocalDate> startDate,
			Optional<LocalDate> endDate, Optional<Integer> locationId, Optional<Integer> departmentId,
			Optional<Integer> employeeId) {
		log.info("Entered into getSummaryByFilter method - DashboardSummaryService");
		try {
			List<DashboardSummary> summaries = dashboardSummaryRepository.findByFilters(orgId, startDate.orElse(null),
					endDate.orElse(null), locationId.orElse(null), departmentId.orElse(null), employeeId.orElse(null));
			log.info("Fetched {} records from getSummaryByFilter", summaries.size());
			return summaries;
		} catch (Exception e) {
			log.error("Error occurred in getSummaryByFilter method - DashboardSummaryService", e);
			return Collections.emptyList();
		} finally {
			log.info("Exiting getSummaryByFilter method - DashboardSummaryService");
		}
	}

	/**
	 * Fetch filtered summaries (projection-based)
	 */
	public List<DashboardSummaryProjection> getFilteredSummaries(Integer orgId, LocalDate startDate, LocalDate endDate,
			Integer locationId, Integer departmentId, Integer employeeId) {
		log.info("Entered into getFilteredSummaries method - DashboardSummaryService");
		try {
			List<DashboardSummaryProjection> result = dashboardSummaryRepository.findByFiltersWithNames(orgId,
					startDate, endDate, locationId, departmentId, employeeId);
			log.info("Fetched {} records from getFilteredSummaries", result.size());
			return result;
		} catch (Exception e) {
			log.error("Error occurred in getFilteredSummaries method - DashboardSummaryService", e);
			return Collections.emptyList();
		} finally {
			log.info("Exiting getFilteredSummaries method - DashboardSummaryService");
		}
	}

	/**
	 * Build dashboard data (summary cards, top performers, daily aggregates,
	 * department-wise compliance)
	 */
	public DashboardDTO getDashboardData(Integer orgId, Integer locationId, Integer departmentId) {
		log.info("Entered into getDashboardData method - DashboardSummaryService for orgId: {}", orgId);
		DashboardDTO dto = new DashboardDTO();

		try {
			// 1. Summary Cards
			dto.setTodayCompliance(dashboardSummaryRepository.getTodayCompliance(orgId, locationId, departmentId));

			// 2. Top performers
			Map<Integer, Double> topMap = new LinkedHashMap<>();
			List<Object[]> topPerformersList = dashboardSummaryRepository.getTopPerformers(orgId, locationId,
					departmentId);

			List<SimpleEntry<Integer, Double>> filteredTopPerformers = topPerformersList.stream()
					.map(row -> new AbstractMap.SimpleEntry<>(((Number) row[0]).intValue(),
							row[1] != null ? ((Number) row[1]).doubleValue() : 0.0))
					.filter(e -> e.getValue() >= 85.0).sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
					.limit(3).toList();

			filteredTopPerformers.forEach(e -> topMap.put(e.getKey(), e.getValue()));
			dto.setTopPerformers(topMap);

			// 3. Daily Aggregates
			List<Object[]> dailyAggregates = dashboardSummaryRepository.getDailyAggregates(orgId, locationId,
					departmentId);
			List<LocalDate> dates = new ArrayList<>();
			List<Double> dateWiseCompliance = new ArrayList<>();
			List<Double> submissionPercentages = new ArrayList<>();
			List<Double> incentives = new ArrayList<>();
			List<Double> deductions = new ArrayList<>();
			double totalIncentive = 0;
			double totalDeduction = 0;

			for (Object[] row : dailyAggregates) {
				LocalDate date = (LocalDate) row[0];
				Double avgCompliance = row[1] != null ? ((Number) row[1]).doubleValue() : 0.0;
				Double submissionPerc = row[4] != null ? ((Number) row[4]).doubleValue() : 0.0;
				Double dayIncentive = row[5] != null ? ((Number) row[5]).doubleValue() : 0.0;
				Double dayDeduction = row[6] != null ? ((Number) row[6]).doubleValue() : 0.0;

				totalIncentive += dayIncentive;
				totalDeduction += dayDeduction;

				dates.add(date);
				dateWiseCompliance.add(avgCompliance);
				submissionPercentages.add(submissionPerc);
				incentives.add(dayIncentive);
				deductions.add(dayDeduction);
			}

			dto.setDates(dates);
			dto.setDateWiseCompliance(dateWiseCompliance);
			dto.setSubmissionPercentages(submissionPercentages);
			dto.setIncentives(incentives);
			dto.setDeductions(deductions);
			dto.setTotalIncentive(totalIncentive);
			dto.setTotalDeduction(totalDeduction);

			// 4. Department-wise compliance
			List<Object[]> depts = dashboardSummaryRepository.getDepartmentWiseCompliance(orgId, locationId);
			List<String> deptNames = new ArrayList<>();
			List<Double> deptComp = new ArrayList<>();
			for (Object[] row : depts) {
				deptNames.add((String) row[1]);
				Double avgComp = row[2] != null ? ((Number) row[2]).doubleValue() : 0.0;
				deptComp.add(avgComp);
			}
			dto.setDepartmentNames(deptNames);
			dto.setDepartmentCompliance(deptComp);

			log.info("Dashboard data successfully prepared for orgId: {}", orgId);
			return dto;

		} catch (Exception e) {
			log.error("Error occurred in getDashboardData method - DashboardSummaryService for orgId: {}", orgId, e);
			return new DashboardDTO();
		} finally {
			log.info("Exiting getDashboardData method - DashboardSummaryService for orgId: {}", orgId);
		}
	}

	private long countWorkingDaysInMonth(Integer orgId, YearMonth month) {
		LocalDate monthStart = month.atDay(1);
		LocalDate monthEnd = month.atEndOfMonth();

		// If month end is in the future, cap it at today
		LocalDate today = LocalDate.now();
		LocalDate effectiveMonthEnd = monthEnd.isAfter(today) ? today : monthEnd;

		// If month start is after effectiveMonthEnd (future month), return 0
		if (monthStart.isAfter(effectiveMonthEnd)) {
			log.debug("Working days in month {}: future month, returning 0", month);
			return 0;
		}

		// Calculate total days from monthStart to effectiveMonthEnd (inclusive)
		long totalDays = java.time.temporal.ChronoUnit.DAYS.between(monthStart, effectiveMonthEnd) + 1;

		// Get holidays count from calendar table (is_active = true means holiday)
		long holidaysCount = calendarRepository.countHolidaysInRange(orgId, monthStart, effectiveMonthEnd);

		// Working days = Total days - Holidays
		long workingDays = totalDays - holidaysCount;

		log.debug("Working days in month {}: totalDays={}, holidays={}, workingDays={}, effectiveEnd={}",
				month, totalDays, holidaysCount, workingDays, effectiveMonthEnd);

		return workingDays > 0 ? workingDays : 0;
	}

	private long countWorkingDaysInFullMonth(Integer orgId, YearMonth month) {
		LocalDate monthStart = month.atDay(1);
		LocalDate monthEnd = month.atEndOfMonth();
		long totalDays = java.time.temporal.ChronoUnit.DAYS.between(monthStart, monthEnd) + 1;
		long holidaysCount = 0;

		if (orgId != null) {
			holidaysCount = calendarRepository.countHolidaysInRange(orgId, monthStart, monthEnd);
		}

		long workingDays = totalDays - holidaysCount;
		log.debug("Full-month working days for month {} and org {}: totalDays={}, holidays={}, workingDays={}",
				month, orgId, totalDays, holidaysCount, workingDays);

		return workingDays > 0 ? workingDays : 0;
	}

	private long countEmployeeWorkingDaysInMonth(Integer employeeId, YearMonth month) {
		LocalDate monthStart = month.atDay(1);
		LocalDate monthEnd = month.atEndOfMonth().plusDays(1);
		return submissionRepository.countEmployeeWorkingDays(employeeId, monthStart, monthEnd);
	}

	private long countWorkingDaysInRange(Integer orgId, LocalDate fromDate, LocalDate toDate) {
		// If toDate is in the future, cap it at today
		LocalDate today = LocalDate.now();
		LocalDate effectiveToDate = toDate.isAfter(today) ? today : toDate;

		// If fromDate is after effectiveToDate, no working days
		if (fromDate.isAfter(effectiveToDate)) {
			log.info("Working days calculation: fromDate={} is after effectiveToDate={}, returning 0", fromDate,
					effectiveToDate);
			return 0;
		}

		// Calculate total days in range (inclusive of both start and end dates)
		long totalDaysInRange = java.time.temporal.ChronoUnit.DAYS.between(fromDate, effectiveToDate) + 1;

		// Get holidays count from calendar table (is_active = true means holiday)
		long holidaysCount = calendarRepository.countHolidaysInRange(orgId, fromDate, effectiveToDate);

		// Working days = Total days - Holidays
		long workingDays = totalDaysInRange - holidaysCount;

		log.info(
				"Working days calculation: fromDate={}, toDate={}, effectiveToDate={}, totalDays={}, holidays={}, workingDays={}",
				fromDate, toDate, effectiveToDate, totalDaysInRange, holidaysCount, workingDays);

		return workingDays > 0 ? workingDays : 0;
	}

	/**
	 * Calculate employee incentive summary for a specific month.
	 * Returns total working days in month, employee's working days, and total
	 * incentive earned.
	 */
	public Map<String, Object> getEmployeeMonthIncentiveSummary(Integer employeeId, Integer orgId, YearMonth month) {
		Map<String, Object> summary = new HashMap<>();

		// Get total working days in full month (month days - active holidays)
		long totalWorkingDays = countWorkingDaysInFullMonth(orgId, month);

		// Get employee's working days (days with submissions)
		long employeeWorkingDays = countEmployeeWorkingDaysInMonth(employeeId, month);

		// Get employee's base incentive
		Optional<Employee> empOpt = employeeRepository.findById(employeeId);
		BigDecimal employeeIncentive = BigDecimal.ZERO;
		if (empOpt.isPresent() && empOpt.get().getIncentive() != null) {
			employeeIncentive = empOpt.get().getIncentive();
		}

		// Calculate total incentive earned
		BigDecimal totalIncentiveEarned = BigDecimal.ZERO;
		long payableWorkingDays = 0;
		BigDecimal perDayIncentive = BigDecimal.ZERO;
		if (totalWorkingDays > 0 && employeeIncentive.compareTo(BigDecimal.ZERO) > 0) {
			perDayIncentive = employeeIncentive.divide(BigDecimal.valueOf(totalWorkingDays), 4, RoundingMode.HALF_UP);
			if (employeeWorkingDays > 0) {
				// Pay by actual worked/submitted days, even if it exceeds company working days.
				payableWorkingDays = employeeWorkingDays;
				totalIncentiveEarned = perDayIncentive.multiply(BigDecimal.valueOf(payableWorkingDays))
						.setScale(2, RoundingMode.HALF_UP);
			}
		}

		summary.put("totalWorkingDays", totalWorkingDays);
		summary.put("employeeWorkingDays", employeeWorkingDays);
		summary.put("payableWorkingDays", payableWorkingDays);
		summary.put("totalIncentive", totalIncentiveEarned.doubleValue());
		summary.put("perDayIncentive", perDayIncentive.setScale(2, RoundingMode.HALF_UP).doubleValue());

		return summary;
	}

	private String buildEmployeeName(Employee emp) {
		String firstName = emp.getFirstName() != null ? emp.getFirstName().trim() : "";
		String lastName = emp.getLastName() != null ? emp.getLastName().trim() : "";
		return (firstName + " " + lastName).trim();
	}

	private String normalizeName(String name) {
		return name == null ? "" : name.trim().replaceAll("\\s+", " ").toLowerCase();
	}

	private List<Employee> filterByStaff(List<Employee> employees, Integer staffId, String staffName) {
		if (employees == null) {
			return new ArrayList<>();
		}
		if (staffId == null || staffName == null || staffName.trim().isEmpty()) {
			if (staffId != null) {
				return employees.stream()
						.filter(emp -> emp.getEmployeeId() != null && emp.getEmployeeId().equals(staffId))
						.toList();
			}
			if (staffName != null && !staffName.trim().isEmpty()) {
				String normalizedRequestedName = normalizeName(staffName);
				return employees.stream()
						.filter(emp -> normalizeName(buildEmployeeName(emp)).equals(normalizedRequestedName))
						.toList();
			}
			return employees;
		}

		// When both values are present, staffId is authoritative and staffName is treated as optional confirmation.
		return employees.stream()
				.filter(emp -> emp.getEmployeeId() != null && emp.getEmployeeId().equals(staffId))
				.toList();
	}

	private String resolveBranchDisplay(List<Integer> branchIds, boolean allBranches, Integer staffId) {
		if (staffId != null) {
			Optional<Employee> empOpt = employeeRepository.findById(staffId);
			if (empOpt.isPresent() && empOpt.get().getLocation() != null) {
				String locationName = empOpt.get().getLocation().getLocationName();
				if (locationName != null && !locationName.trim().isEmpty()) {
					return locationName.trim();
				}
			}
		}

		if (allBranches) {
			return "All Branches";
		}
		if (branchIds != null && !branchIds.isEmpty()) {
			List<Location> locations = locationRepository.findAllById(branchIds);
			List<String> branchNames = locations.stream()
					.map(Location::getLocationName)
					.filter(name -> name != null)
					.toList();
			if (!branchNames.isEmpty()) {
				return String.join(", ", branchNames);
			}
		}
		return "All Branches";
	}

	private void applyBlueBackground(CellStyle style) {
		if (style instanceof XSSFCellStyle xssfStyle) {
			XSSFColor blue = new XSSFColor(new Color(0x25, 0x63, 0xEB), new DefaultIndexedColorMap());
			xssfStyle.setFillForegroundColor(blue);
			xssfStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
		}
	}

	private Set<Integer> parseIds(String csvIds) {
		Set<Integer> ids = new HashSet<>();
		if (csvIds == null || csvIds.isBlank()) {
			return ids;
		}
		for (String part : csvIds.split(",")) {
			if (part == null) continue;
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
			List<Option> options = optionRepository.findByQuestion_QuestionId(qId);
			if (options != null) {
				for (Option option : options) {
					if (option != null && option.getOptionValue() != null && !option.getOptionValue().isBlank()) {
						lookup.put(option.getOptionValue().trim().toLowerCase(), option.getOptionId());
					}
				}
			}
			return lookup;
		});
		String normalized = rawAnswer.replace("[", "").replace("]", "").replace("\"", "").trim();
		if (normalized.isEmpty()) {
			return ids;
		}
		for (String part : normalized.split(",")) {
			if (part == null) continue;
			String token = part.trim();
			if (token.isEmpty()) continue;
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

	public Map<String, Object> calculateEmployeeIncentive(Integer orgId, LocalDate fromDate, LocalDate toDate,
			List<Integer> branchIds, boolean allBranches) {
		return calculateEmployeeIncentive(orgId, fromDate, toDate, branchIds, allBranches, null, null);
	}

	public Map<String, Object> calculateEmployeeIncentive(Integer orgId, LocalDate fromDate, LocalDate toDate,
			List<Integer> branchIds, boolean allBranches, Integer staffId, String staffName) {
		log.info(
				"Entered calculateEmployeeIncentive - orgId={}, fromDate={}, toDate={}, branchIds={}, allBranches={}, staffId={}, staffName={}",
				orgId, fromDate, toDate, branchIds, allBranches, staffId, staffName);
		Map<String, Object> response = new HashMap<>();

		try {
			if (fromDate == null || toDate == null) {
				response.put("success", false);
				response.put("message", "From date and to date are required");
				return response;
			}

			if (fromDate.isAfter(toDate)) {
				response.put("success", false);
				response.put("message", "From date must be before to date");
				return response;
			}

			List<Employee> employees = employeeRepository.findEmployeesByFilters(orgId, null);
			if (employees == null || employees.isEmpty()) {
				response.put("success", true);
				response.put("message", "No employees found for the organization");
				response.put("data", new ArrayList<>());
				response.put("totalIncentiveCost", 0.0);
				return response;
			}

			if (!allBranches && branchIds != null && !branchIds.isEmpty()) {
				log.info("Filtering employees by branchIds: {}", branchIds);
				employees = employees.stream()
						.filter(emp -> emp.getLocation() != null
								&& emp.getLocation().getLocationId() != null
								&& branchIds.contains(emp.getLocation().getLocationId()))
						.toList();
				log.info("Employees after branch filter: {}", employees.size());
			}

			employees = filterByStaff(employees, staffId, staffName);
			if (staffId != null && staffName != null && !staffName.trim().isEmpty()) {
				log.info("Employees after staff filter (staffId={}, staffName={}): {}", staffId, staffName,
						employees.size());
				if (employees.isEmpty()) {
					response.put("success", true);
					response.put("message", "No matching staff found for the provided staff details");
					response.put("data", new ArrayList<>());
					response.put("totalIncentiveCost", 0.0);
					response.put("totalEmployees", 0);
					response.put("from", fromDate);
					response.put("to", toDate);
					response.put("staffId", staffId);
					response.put("staffName", staffName);
					response.put("branchIds", branchIds != null ? branchIds : new ArrayList<>());
					response.put("allBranches", allBranches);
					return response;
				}
			}

			log.info("Total employees fetched from organization {}: {}", orgId, employees.size());
			employees.forEach(e -> log.debug("Employee: ID={}, Name={}, Active={}, Incentive={}",
					e.getEmployeeId(), e.getFirstName(), e.getIsActive(), e.getIncentive()));

			log.info("Processing incentive for {} employees, range={} to {}", employees.size(), fromDate, toDate);

			List<Map<String, Object>> employeeIncentiveList = new ArrayList<>();
			BigDecimal totalIncentiveCost = BigDecimal.ZERO;

			Optional<Org> orgOpt = orgRepository.findById(orgId);
			String orgName = "";
			String commonMail = "";

			// Shared option-value → option-ID cache (populated on first lookup per question)
			Map<Integer, Map<String, Integer>> optionValueToIdCache = new HashMap<>();

			for (Employee emp : employees) {
				if (!emp.getIsActive()) {
					log.warn("Skipping employee ID={}, Name={} - NOT ACTIVE", emp.getEmployeeId(), emp.getFirstName());
					continue;
				}

				BigDecimal totalEmployeeIncentiveEarned = BigDecimal.ZERO;
				long totalEmployeeWorkingDays = 0;
				double totalEmployeeValidWeightage = 0.0;
				BigDecimal totalEmpIncentive = (emp.getIncentive() != null) ? emp.getIncentive() : BigDecimal.ZERO;

				// Step 1: Fetch active submissions by question_date range
				List<Submission> submissions = submissionRepository
						.findByEmployee_EmployeeIdAndQuestionDateGreaterThanEqualAndQuestionDateLessThanAndIsActiveTrue(
								emp.getEmployeeId(), fromDate, toDate.plusDays(1));

				if (submissions != null && !submissions.isEmpty()) {
					// Step 2: Group by YearMonth of question_date — each month uses its own full-month working days
					Map<YearMonth, List<Submission>> byMonth = submissions.stream()
							.filter(s -> s.getQuestionDate() != null)
							.collect(Collectors.groupingBy(s -> YearMonth.from(s.getQuestionDate())));

					for (Map.Entry<YearMonth, List<Submission>> monthEntry : byMonth.entrySet()) {
						YearMonth ym = monthEntry.getKey();
						List<Submission> monthSubs = monthEntry.getValue();

						// Step 3: Full-month working days for per-day incentive rate
						long orgWorkingDays = countWorkingDaysInFullMonth(orgId, ym);
						if (orgWorkingDays <= 0) continue;

						BigDecimal perDayIncentive = (totalEmpIncentive.compareTo(BigDecimal.ZERO) > 0)
								? totalEmpIncentive.divide(BigDecimal.valueOf(orgWorkingDays), 4, RoundingMode.HALF_UP)
								: BigDecimal.ZERO;

						// Step 4: Group by question_date within this month
						Map<LocalDate, List<Submission>> byDay = monthSubs.stream()
								.collect(Collectors.groupingBy(Submission::getQuestionDate));
						totalEmployeeWorkingDays += byDay.size();

						for (Map.Entry<LocalDate, List<Submission>> dayEntry : byDay.entrySet()) {
							List<Submission> daySubs = dayEntry.getValue();
							if (daySubs == null || daySubs.isEmpty()) continue;

							double dayTotalWeightage = 0.0;
							double dayValidWeightage = 0.0;

							// Step 5: Per-question weightage scoring
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
										// valid_answer is NULL — skip entirely
										continue;
									}
									dayTotalWeightage += weightage;
									Set<Integer> submittedIds = resolveAnswerIds(
											submission.getAnswer(), question.getQuestionId(), optionValueToIdCache);
									if (!java.util.Collections.disjoint(submittedIds, validAnswerIds)) {
										dayValidWeightage += weightage;
									}
								} else {
									// text/number/other: always count; valid if answer is non-empty
									dayTotalWeightage += weightage;
									String answer = submission.getAnswer();
									if (answer != null && !answer.isBlank()) {
										dayValidWeightage += weightage;
									}
								}
							}

							// Step 6: dayIncentive = perDayIncentive × (dayValidWeightage / dayTotalWeightage)
							if (dayTotalWeightage > 0) {
								BigDecimal costPerWeightage = perDayIncentive.divide(
										BigDecimal.valueOf(dayTotalWeightage), 8, RoundingMode.HALF_UP);
								BigDecimal dayIncentive = costPerWeightage
										.multiply(BigDecimal.valueOf(dayValidWeightage));
								totalEmployeeIncentiveEarned = totalEmployeeIncentiveEarned.add(dayIncentive);
								totalEmployeeValidWeightage += dayValidWeightage;
							}
						}
					}
				}

				totalEmployeeIncentiveEarned = totalEmployeeIncentiveEarned.setScale(2, RoundingMode.HALF_UP);
				Map<String, Object> empData = new HashMap<>();
				empData.put("employeeId", emp.getEmployeeId());
				empData.put("employeeCode", emp.getEmployeeCode() != null ? emp.getEmployeeCode() : "");
				empData.put("employeeName", (emp.getFirstName() != null ? emp.getFirstName() : "")
						+ (emp.getLastName() != null ? " " + emp.getLastName() : ""));
				empData.put("employeeRole", emp.getRoleId() != null ? emp.getRoleId().getRoleName() : "");
				empData.put("incentive", totalEmployeeIncentiveEarned.doubleValue());
				empData.put("workingDays", totalEmployeeWorkingDays);
				empData.put("validWeightage", BigDecimal.valueOf(totalEmployeeValidWeightage)
						.setScale(2, RoundingMode.HALF_UP).doubleValue());

				employeeIncentiveList.add(empData);
				totalIncentiveCost = totalIncentiveCost.add(totalEmployeeIncentiveEarned);
			}

			// Sort by incentive descending
			employeeIncentiveList.sort((a, b) -> Double.compare(
					(Double) b.get("incentive"),
					(Double) a.get("incentive")));

			if (orgOpt.isPresent()) {
				Org org = orgOpt.get();
				orgName = org.getOrgName() != null ? org.getOrgName() : "";
				commonMail = org.getCommonMail() != null ? org.getCommonMail() : "";
				log.info("Org details - Name: {}, Email: {}", orgName, commonMail);
			}

			// Calculate total working days for the specific date range (not entire months)
			long totalWorkingDaysInRange = countWorkingDaysInRange(orgId, fromDate, toDate);
			log.info("Total working days in range ({} to {}): {}", fromDate, toDate, totalWorkingDaysInRange);

			response.put("success", true);
			response.put("message", "Employee incentive calculated successfully");
			response.put("data", employeeIncentiveList);
			response.put("totalIncentiveCost", totalIncentiveCost.doubleValue());
			response.put("totalEmployees", employeeIncentiveList.size());
			response.put("from", fromDate);
			response.put("to", toDate);
			response.put("totalWorkingDays", totalWorkingDaysInRange);
			response.put("orgName", orgName);
			response.put("commonMail", commonMail);
			response.put("branchIds", branchIds != null ? branchIds : new ArrayList<>());
			response.put("allBranches", allBranches);
			response.put("staffId", staffId);
			response.put("staffName", staffName);

			log.info("Employee incentive calculated successfully for org {}", orgId);
			return response;

		} catch (Exception e) {
			log.error("Error in calculateEmployeeIncentive - orgId={}", orgId, e);
			response.put("success", false);
			response.put("message", "Failed to calculate employee incentive: " + e.getMessage());
			return response;
		} finally {
			log.info("Exiting calculateEmployeeIncentive");
		}
	}

	/**
	 * Generate Excel report for employee incentive data and save to disk
	 * 
	 * @return the absolute path of the saved file
	 */
	public String generateIncentiveReportExcel(Integer orgId, LocalDate fromDate, LocalDate toDate,
			List<Integer> branchIds, boolean allBranches) throws Exception {
		return generateIncentiveReportExcel(orgId, fromDate, toDate, branchIds, allBranches, null, null);
	}

	public String generateIncentiveReportExcel(Integer orgId, LocalDate fromDate, LocalDate toDate,
			List<Integer> branchIds, boolean allBranches, Integer staffId, String staffName) throws Exception {
		log.info("Generating Excel report for orgId={}, fromDate={}, toDate={}", orgId, fromDate, toDate);

		// Get the incentive data
		Map<String, Object> incentiveData = calculateEmployeeIncentive(orgId, fromDate, toDate, branchIds, allBranches,
				staffId, staffName);

		if (incentiveData == null || !(Boolean) incentiveData.getOrDefault("success", false)) {
			throw new RuntimeException("Failed to calculate incentive data");
		}

		try (Workbook workbook = new XSSFWorkbook()) {
			Sheet sheet = workbook.createSheet("Incentive Report");

			CellStyle headerStyle = workbook.createCellStyle();
			Font headerFont = workbook.createFont();
			headerFont.setBold(true);
			headerFont.setColor(IndexedColors.WHITE.getIndex());
			headerStyle.setFont(headerFont);
			headerStyle.setBorderBottom(BorderStyle.THIN);
			headerStyle.setBorderTop(BorderStyle.THIN);
			headerStyle.setBorderLeft(BorderStyle.THIN);
			headerStyle.setBorderRight(BorderStyle.THIN);
			applyBlueBackground(headerStyle);

			CellStyle cellStyle = workbook.createCellStyle();
			cellStyle.setBorderBottom(BorderStyle.THIN);
			cellStyle.setBorderTop(BorderStyle.THIN);
			cellStyle.setBorderLeft(BorderStyle.THIN);
			cellStyle.setBorderRight(BorderStyle.THIN);

			CellStyle totalStyle = workbook.createCellStyle();
			Font totalFont = workbook.createFont();
			totalFont.setBold(true);
			totalStyle.setFont(totalFont);
			totalStyle.setBorderBottom(BorderStyle.THIN);
			totalStyle.setBorderTop(BorderStyle.THIN);
			totalStyle.setBorderLeft(BorderStyle.THIN);
			totalStyle.setBorderRight(BorderStyle.THIN);

			// Create centered style for header info
			CellStyle centeredStyle = workbook.createCellStyle();
			Font centeredFont = workbook.createFont();
			centeredFont.setBold(true);
			centeredFont.setFontHeightInPoints((short) 12);
			centeredStyle.setFont(centeredFont);
			centeredStyle.setAlignment(HorizontalAlignment.CENTER);

			int rowNum = 0;

			// Number of columns in the data table
			int numColumns = 6;

			// Organization header - merged across all columns
			String orgName = (String) incentiveData.getOrDefault("orgName", "");
			Row orgRow = sheet.createRow(rowNum);
			orgRow.setHeightInPoints(28f);
			Cell orgCell = orgRow.createCell(0);
			orgCell.setCellValue(orgName);
			orgCell.setCellStyle(centeredStyle);
			sheet.addMergedRegion(new CellRangeAddress(rowNum, rowNum, 0, numColumns - 1));
			rowNum++;

			// Branch name row - merged across all columns
			Row branchRow = sheet.createRow(rowNum);
			String branchDisplay = resolveBranchDisplay(branchIds, allBranches, staffId);
			Cell branchCell = branchRow.createCell(0);
			branchCell.setCellValue("Branch: " + branchDisplay);
			branchCell.setCellStyle(centeredStyle);
			sheet.addMergedRegion(new CellRangeAddress(rowNum, rowNum, 0, numColumns - 1));
			rowNum++;

			// Date range - merged across all columns
			Row dateRow = sheet.createRow(rowNum);
			Cell dateCell = dateRow.createCell(0);
			DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
			dateCell.setCellValue(
					"From: " + fromDate.format(dateFormatter) + "    To: " + toDate.format(dateFormatter));
			dateCell.setCellStyle(centeredStyle);
			sheet.addMergedRegion(new CellRangeAddress(rowNum, rowNum, 0, numColumns - 1));
			rowNum++;

			// Table headers
			Row headerRow = sheet.createRow(rowNum++);
			String[] headers = { "S.No", "Staff Code", "Staff Name", "Role Name", "Incentive (INR)",
					"Working Days" };
			for (int i = 0; i < headers.length; i++) {
				Cell cell = headerRow.createCell(i);
				cell.setCellValue(headers[i]);
				cell.setCellStyle(headerStyle);
			}

			// Data rows
			@SuppressWarnings("unchecked")
			List<Map<String, Object>> employeeList = (List<Map<String, Object>>) incentiveData.get("data");
			long totalIncentive = 0L;
			long totalWorkingDays = 0;
			int serialNo = 1;

			if (employeeList != null) {
				for (Map<String, Object> emp : employeeList) {
					// Skip employees where both incentive amount is 0 AND working days count is 0
					Object incentiveObj = emp.get("incentive");
					Object daysObj = emp.get("workingDays");
					double incentive = incentiveObj != null ? Double.parseDouble(incentiveObj.toString()) : 0.0;
					long roundedIncentive = Math.round(incentive);
					long days = daysObj != null ? Long.parseLong(daysObj.toString()) : 0;
					
					if (incentive == 0 && days == 0) {
						log.debug("Skipping employee with no incentive and no working days: {}", emp.get("employeeName"));
						continue;
					}

					Row dataRow = sheet.createRow(rowNum++);

					Cell snoCell = dataRow.createCell(0);
					snoCell.setCellValue(serialNo++);
					snoCell.setCellStyle(cellStyle);

					Cell empCodeCell = dataRow.createCell(1);
					Object empCodeObj = emp.get("employeeCode");
					empCodeCell.setCellValue(empCodeObj != null ? empCodeObj.toString() : "");
					empCodeCell.setCellStyle(cellStyle);

					Cell nameCell = dataRow.createCell(2);
					Object nameObj = emp.get("employeeName");
					nameCell.setCellValue(nameObj != null ? nameObj.toString() : "");
					nameCell.setCellStyle(cellStyle);

					Cell roleCell = dataRow.createCell(3);
					Object roleObj = emp.get("employeeRole");
					roleCell.setCellValue(roleObj != null ? roleObj.toString() : "");
					roleCell.setCellStyle(cellStyle);

					Cell incentiveCell = dataRow.createCell(4);
					incentiveCell.setCellValue(roundedIncentive);
					incentiveCell.setCellStyle(cellStyle);
					totalIncentive += roundedIncentive;

					Cell daysCell = dataRow.createCell(5);
					daysCell.setCellValue(days);
					daysCell.setCellStyle(cellStyle);
					totalWorkingDays += days;
				}
			}

			// Total Incentive row
			Row totalIncentiveRow = sheet.createRow(rowNum++);
			Cell totalLabelCell = totalIncentiveRow.createCell(3);
			totalLabelCell.setCellValue("Total");
			totalLabelCell.setCellStyle(totalStyle);
			Cell totalValueCell = totalIncentiveRow.createCell(4);
			totalValueCell.setCellValue(totalIncentive);
			totalValueCell.setCellStyle(totalStyle);

			Cell totalDaysCell = totalIncentiveRow.createCell(5);
			// Total row shows employee worked days in selected date range.
			totalDaysCell.setCellValue(totalWorkingDays);
			totalDaysCell.setCellStyle(totalStyle);

			for (int i = 0; i < headers.length; i++) {
				sheet.autoSizeColumn(i);
			}

			String reportsDir = "reports";
			Path reportsDirPath = Paths.get(reportsDir);
			if (!Files.exists(reportsDirPath)) {
				Files.createDirectories(reportsDirPath);
				log.info("Created reports directory: {}", reportsDirPath.toAbsolutePath());
			}

			// Generate unique filename and save to disk
			String filename = "Monthly_Progress_Summary_" + fromDate + "_to_" + toDate + "_" + System.currentTimeMillis()
					+ ".xlsx";
			Path filePath = reportsDirPath.resolve(filename);

			try (FileOutputStream fileOut = new FileOutputStream(filePath.toFile())) {
				workbook.write(fileOut);
			}

			String absolutePath = filePath.toAbsolutePath().toString();
			log.info("Excel report saved to: {}", absolutePath);
			return absolutePath;
		}
	}

	/**
	 * Generate Daily Progress Excel report - each day's progress for all employees
	 * Shows Organization, Branch, Date Range, then for each employee: daily
	 * incentive breakdown
	 * 
	 * @return the absolute path of the saved file
	 */
	public String generateDailyProgressReportExcel(Integer orgId, LocalDate fromDate, LocalDate toDate,
			List<Integer> branchIds, boolean allBranches) throws Exception {
		return generateDailyProgressReportExcel(orgId, fromDate, toDate, branchIds, allBranches, null, null);
	}

	public String generateDailyProgressReportExcel(Integer orgId, LocalDate fromDate, LocalDate toDate,
			List<Integer> branchIds, boolean allBranches, Integer staffId, String staffName) throws Exception {
		log.info("Generating Daily Progress Excel report for orgId={}, fromDate={}, toDate={}", orgId, fromDate,
				toDate);

		try (Workbook workbook = new XSSFWorkbook()) {
			Sheet sheet = workbook.createSheet("Daily Progress Report");

			CellStyle headerStyle = workbook.createCellStyle();
			Font headerFont = workbook.createFont();
			headerFont.setBold(true);
			headerFont.setColor(IndexedColors.WHITE.getIndex());
			headerStyle.setFont(headerFont);
			headerStyle.setBorderBottom(BorderStyle.THIN);
			headerStyle.setBorderTop(BorderStyle.THIN);
			headerStyle.setBorderLeft(BorderStyle.THIN);
			headerStyle.setBorderRight(BorderStyle.THIN);
			applyBlueBackground(headerStyle);

			CellStyle titleStyle = workbook.createCellStyle();
			Font titleFont = workbook.createFont();
			titleFont.setBold(true);
			titleFont.setFontHeightInPoints((short) 14);
			titleStyle.setFont(titleFont);

			CellStyle cellStyle = workbook.createCellStyle();
			cellStyle.setBorderBottom(BorderStyle.THIN);
			cellStyle.setBorderTop(BorderStyle.THIN);
			cellStyle.setBorderLeft(BorderStyle.THIN);
			cellStyle.setBorderRight(BorderStyle.THIN);

			CellStyle totalStyle = workbook.createCellStyle();
			Font totalFont = workbook.createFont();
			totalFont.setBold(true);
			totalStyle.setFont(totalFont);
			totalStyle.setBorderBottom(BorderStyle.THIN);
			totalStyle.setBorderTop(BorderStyle.THIN);
			totalStyle.setBorderLeft(BorderStyle.THIN);
			totalStyle.setBorderRight(BorderStyle.THIN);

			CellStyle centeredStyle = workbook.createCellStyle();
			Font centeredFont = workbook.createFont();
			centeredFont.setBold(true);
			centeredFont.setFontHeightInPoints((short) 12);
			centeredStyle.setFont(centeredFont);
			centeredStyle.setAlignment(HorizontalAlignment.CENTER);

			int rowNum = 0;
			int numColumns = 6;

			Optional<Org> orgOpt = orgRepository.findById(orgId);
			String orgName = orgOpt.map(Org::getOrgName).orElse("Unknown Organization");

			Row orgRow = sheet.createRow(rowNum);
			Cell orgCell = orgRow.createCell(0);
			orgCell.setCellValue(orgName);
			orgCell.setCellStyle(centeredStyle);
			sheet.addMergedRegion(new CellRangeAddress(rowNum, rowNum, 0, numColumns - 1));
			rowNum++;

			Row branchRow = sheet.createRow(rowNum);
			String branchDisplay = resolveBranchDisplay(branchIds, allBranches, staffId);
			Cell branchCell = branchRow.createCell(0);
			branchCell.setCellValue("Branch: " + branchDisplay);
			branchCell.setCellStyle(centeredStyle);
			sheet.addMergedRegion(new CellRangeAddress(rowNum, rowNum, 0, numColumns - 1));
			rowNum++;

			Row dateRow = sheet.createRow(rowNum);
			Cell dateRangeCell = dateRow.createCell(0);
			DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
			dateRangeCell.setCellValue(
					"From: " + fromDate.format(dateFormatter) + "    To: " + toDate.format(dateFormatter));
			dateRangeCell.setCellStyle(centeredStyle);
			sheet.addMergedRegion(new CellRangeAddress(rowNum, rowNum, 0, numColumns - 1));
			rowNum++;

			Row tableHeaderRow = sheet.createRow(rowNum++);
			String[] headers = { "S.No", "Staff Code", "Staff Name", "Role Name", "Compliance Date", "Incentive(INR)" };
			for (int i = 0; i < headers.length; i++) {
				Cell cell = tableHeaderRow.createCell(i);
				cell.setCellValue(headers[i]);
				cell.setCellStyle(headerStyle);
			}

			List<Employee> employees = employeeRepository.findEmployeesByFilters(orgId,  null);
			if (employees == null) {
				employees = new ArrayList<>();
			}

			if (!allBranches && branchIds != null && !branchIds.isEmpty()) {
				employees = employees.stream()
						.filter(emp -> emp.getLocation() != null
								&& emp.getLocation().getLocationId() != null
								&& branchIds.contains(emp.getLocation().getLocationId()))
						.toList();
			}

			employees = filterByStaff(employees, staffId, staffName);
			employees = employees.stream()
					.filter(emp -> emp.getIsActive() != null && emp.getIsActive())
					.toList();

			log.info("Processing {} employees for daily progress report", employees.size());

			long grandTotalIncentive = 0L;
			int serialNo = 1;

			// Shared option-value → option-ID cache (populated on first lookup per question)
			Map<Integer, Map<String, Integer>> optionValueToIdCache = new HashMap<>();

			for (Employee emp : employees) {
				if (emp.getEmployeeId() == null)
					continue;

				String employeeName = (emp.getFirstName() != null ? emp.getFirstName() : "")
						+ (emp.getLastName() != null ? " " + emp.getLastName() : "");
				String employeeRole = emp.getRoleId() != null ? emp.getRoleId().getRoleName() : "";
				BigDecimal totalEmpIncentive = (emp.getIncentive() != null) ? emp.getIncentive() : BigDecimal.ZERO;

				// Step 1: Fetch active submissions by question_date range
				List<Submission> submissions = submissionRepository
						.findByEmployee_EmployeeIdAndQuestionDateGreaterThanEqualAndQuestionDateLessThanAndIsActiveTrue(
								emp.getEmployeeId(), fromDate, toDate.plusDays(1));

				if (submissions == null || submissions.isEmpty()) {
					continue;
				}

				// Step 2: Group by YearMonth → then by question_date
				Map<YearMonth, Map<LocalDate, List<Submission>>> byMonthThenDay = submissions.stream()
						.filter(s -> s.getQuestionDate() != null)
						.collect(Collectors.groupingBy(
								s -> YearMonth.from(s.getQuestionDate()),
								Collectors.groupingBy(Submission::getQuestionDate)));

				// Collect all dates in sorted order so rows are written chronologically
				List<LocalDate> sortedDates = submissions.stream()
						.filter(s -> s.getQuestionDate() != null)
						.map(Submission::getQuestionDate)
						.distinct()
						.sorted()
						.collect(Collectors.toList());

				if (sortedDates.isEmpty()) {
					continue;
				}

				for (LocalDate date : sortedDates) {
					YearMonth ym = YearMonth.from(date);
					Map<LocalDate, List<Submission>> dayMap = byMonthThenDay.getOrDefault(ym, new HashMap<>());
					List<Submission> daySubs = dayMap.getOrDefault(date, new ArrayList<>());
					if (daySubs.isEmpty()) continue;

					// Step 3: Full-month working days for per-day incentive rate
					long orgWorkingDays = countWorkingDaysInFullMonth(orgId, ym);
					BigDecimal perDayIncentive = (orgWorkingDays > 0
							&& totalEmpIncentive.compareTo(BigDecimal.ZERO) > 0)
									? totalEmpIncentive.divide(BigDecimal.valueOf(orgWorkingDays), 4, RoundingMode.HALF_UP)
									: BigDecimal.ZERO;

					// Step 4 & 5: Per-question weightage scoring
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
								// valid_answer is NULL — skip entirely
								continue;
							}
							dayTotalWeightage += weightage;
							Set<Integer> submittedIds = resolveAnswerIds(
									submission.getAnswer(), question.getQuestionId(), optionValueToIdCache);
							if (!java.util.Collections.disjoint(submittedIds, validAnswerIds)) {
								dayValidWeightage += weightage;
							}
						} else {
							// text/number/other: always count; valid if answer is non-empty
							dayTotalWeightage += weightage;
							String answer = submission.getAnswer();
							if (answer != null && !answer.isBlank()) {
								dayValidWeightage += weightage;
							}
						}
					}

					// Step 6: dayIncentive = perDayIncentive × (dayValidWeightage / dayTotalWeightage)
					BigDecimal dayIncentiveBD = BigDecimal.ZERO;
					if (dayTotalWeightage > 0 && perDayIncentive.compareTo(BigDecimal.ZERO) > 0) {
						BigDecimal costPerWeightage = perDayIncentive.divide(
								BigDecimal.valueOf(dayTotalWeightage), 8, RoundingMode.HALF_UP);
						dayIncentiveBD = costPerWeightage.multiply(BigDecimal.valueOf(dayValidWeightage));
					}
					long roundedDailyIncentive = Math.round(dayIncentiveBD.doubleValue());

					Row dataRow = sheet.createRow(rowNum++);

					Cell snoCell = dataRow.createCell(0);
					snoCell.setCellValue(serialNo++);
					snoCell.setCellStyle(cellStyle);

					Cell staffCodeCell = dataRow.createCell(1);
					staffCodeCell.setCellValue(emp.getEmployeeCode() != null ? emp.getEmployeeCode() : "");
					staffCodeCell.setCellStyle(cellStyle);

					Cell staffNameCell = dataRow.createCell(2);
					staffNameCell.setCellValue(employeeName.trim());
					staffNameCell.setCellStyle(cellStyle);

					Cell roleCell = dataRow.createCell(3);
					roleCell.setCellValue(employeeRole != null ? employeeRole : "");
					roleCell.setCellStyle(cellStyle);

					Cell complianceDateCell = dataRow.createCell(4);
					complianceDateCell.setCellValue(date.format(dateFormatter));
					complianceDateCell.setCellStyle(cellStyle);

					Cell incentiveCell = dataRow.createCell(5);
					incentiveCell.setCellValue(roundedDailyIncentive);
					incentiveCell.setCellStyle(cellStyle);

					grandTotalIncentive += roundedDailyIncentive;
				}
			}

			Row grandTotalIncentiveRow = sheet.createRow(rowNum++);
			Cell totalLabelCell = grandTotalIncentiveRow.createCell(4);
			totalLabelCell.setCellValue("Total");
			totalLabelCell.setCellStyle(totalStyle);
			Cell gtiCell = grandTotalIncentiveRow.createCell(5);
			gtiCell.setCellValue(grandTotalIncentive);
			gtiCell.setCellStyle(totalStyle);
			for (int i = 0; i <= 3; i++) {
				Cell cell = grandTotalIncentiveRow.createCell(i);
				cell.setCellStyle(totalStyle);
			}

			for (int i = 0; i < headers.length; i++) {
				sheet.autoSizeColumn(i);
			}

			String reportsDir = "reports";
			Path reportsDirPath = Paths.get(reportsDir);
			if (!Files.exists(reportsDirPath)) {
				Files.createDirectories(reportsDirPath);
				log.info("Created reports directory: {}", reportsDirPath.toAbsolutePath());
			}

			String filename = "Monthly_Progress_Detail_" + fromDate + "_to_" + toDate + "_" + System.currentTimeMillis()
					+ ".xlsx";
			Path filePath = reportsDirPath.resolve(filename);

			try (FileOutputStream fileOut = new FileOutputStream(filePath.toFile())) {
				workbook.write(fileOut);
			}

			String absolutePath = filePath.toAbsolutePath().toString();
			log.info("Daily Progress Excel report saved to: {}", absolutePath);
			return absolutePath;
		}
	}

	private boolean isEmployeeWithinDateRange(Employee emp, LocalDate fromDate, LocalDate toDate) {
		if (fromDate == null && toDate == null) {
			return true;
		}

		LocalDate employeeDate = null;
		if (emp.getCreatedDt() != null) {
			employeeDate = emp.getCreatedDt().toLocalDate();
		} else if (emp.getJoinDate() != null) {
			employeeDate = emp.getJoinDate();
		}

		if (employeeDate == null) {
			return false;
		}
		if (fromDate != null && employeeDate.isBefore(fromDate)) {
			return false;
		}
		if (toDate != null && employeeDate.isAfter(toDate)) {
			return false;
		}
		return true;
	}

	private String resolveManagerName(Integer managerId, Map<Integer, String> cache) {
		if (managerId == null) {
			return "";
		}
		if (cache.containsKey(managerId)) {
			return cache.get(managerId);
		}

		String name = "";
		Optional<Employee> managerOpt = employeeRepository.findById(managerId);
		if (managerOpt.isPresent()) {
			name = buildEmployeeName(managerOpt.get());
		}
		cache.put(managerId, name);
		return name;
	}

	public String generateStaffDetailsReportExcel(Integer orgId, LocalDate fromDate, LocalDate toDate,
			List<Integer> branchIds) throws Exception {
		return generateStaffDetailsReportExcel(orgId, fromDate, toDate, branchIds, false);
	}

	public String generateStaffDetailsReportExcel(Integer orgId, LocalDate fromDate, LocalDate toDate,
			List<Integer> branchIds, boolean allBranches) throws Exception {
		return generateStaffDetailsReportExcel(orgId, fromDate, toDate, branchIds, allBranches, null);
	}

	public String generateStaffDetailsReportExcel(Integer orgId, LocalDate fromDate, LocalDate toDate,
			List<Integer> branchIds, boolean allBranches, Integer roleId) throws Exception {
		log.info("Generating Staff Details report for orgId={}, fromDate={}, toDate={}, branchIds={}, roleId={}",
				orgId, fromDate, toDate, branchIds, roleId);

		LocalDate effectiveFromDate = fromDate;
		LocalDate effectiveToDate = toDate;
		

		List<Employee> employees = employeeRepository.findEmployeesByFilters(orgId, null);
		if (employees == null) {
			employees = new ArrayList<>();
		}

		boolean hasBranchFilter = !allBranches && branchIds != null && !branchIds.isEmpty();
		Set<Integer> requestedBranchIds = hasBranchFilter ? new HashSet<>(branchIds) : Collections.emptySet();

		employees = employees.stream()
				.filter(emp -> {
					if (emp.getIsActive() == null || !emp.getIsActive()) {
						return false;
					}

					if (roleId != null) {
						if (emp.getRoleId() == null || emp.getRoleId().getRoleId() == null
								|| !roleId.equals(emp.getRoleId().getRoleId())) {
							return false;
						}
					}

					// If branch filter is selected, employee must belong to one of the selected
					// primary branches.
					if (hasBranchFilter) {
						if (emp.getLocation() != null && emp.getLocation().getLocationId() != null
								&& requestedBranchIds.contains(emp.getLocation().getLocationId())) {
							return isEmployeeWithinDateRange(emp, effectiveFromDate, effectiveToDate);
						}
						return false;
					}

					// No branch filter selected: role/date-only in selected org.
					return isEmployeeWithinDateRange(emp, effectiveFromDate, effectiveToDate);
				})
				.toList();

		log.info("Staff report filter applied: orgId={}, roleId={}, allBranches={}, branchIds={}, matchedEmployees={}",
				orgId, roleId, allBranches, branchIds, employees.size());

		employees = employees.stream()
				.sorted(Comparator.comparing(Employee::getEmployeeId, Comparator.nullsLast(Integer::compareTo)))
				.toList();

		try (Workbook workbook = new XSSFWorkbook()) {
			Sheet sheet = workbook.createSheet("Staff details");

			CellStyle headerStyle = workbook.createCellStyle();
			Font headerFont = workbook.createFont();
			headerFont.setBold(true);
			headerFont.setColor(IndexedColors.WHITE.getIndex());
			headerStyle.setFont(headerFont);
			headerStyle.setBorderBottom(BorderStyle.THIN);
			headerStyle.setBorderTop(BorderStyle.THIN);
			headerStyle.setBorderLeft(BorderStyle.THIN);
			headerStyle.setBorderRight(BorderStyle.THIN);
			applyBlueBackground(headerStyle);

			CellStyle cellStyle = workbook.createCellStyle();
			cellStyle.setBorderBottom(BorderStyle.THIN);
			cellStyle.setBorderTop(BorderStyle.THIN);
			cellStyle.setBorderLeft(BorderStyle.THIN);
			cellStyle.setBorderRight(BorderStyle.THIN);

			CellStyle centeredStyle = workbook.createCellStyle();
			Font centeredFont = workbook.createFont();
			centeredFont.setBold(true);
			centeredFont.setFontHeightInPoints((short) 12);
			centeredStyle.setFont(centeredFont);
			centeredStyle.setAlignment(HorizontalAlignment.CENTER);

			int rowNum = 0;
			int numColumns = 15;

			Optional<Org> orgOpt = orgRepository.findById(orgId);
			String orgName = orgOpt.map(Org::getOrgName).orElse("Unknown Organization");

			Row orgRow = sheet.createRow(rowNum);
			Cell orgCell = orgRow.createCell(0);
			orgCell.setCellValue(orgName);
			orgCell.setCellStyle(centeredStyle);
			sheet.addMergedRegion(new CellRangeAddress(rowNum, rowNum, 0, numColumns - 1));
			rowNum++;

			Row branchRow = sheet.createRow(rowNum);
			branchRow.setHeightInPoints(24f);
			Cell branchCell = branchRow.createCell(0);
			String branchDisplay = resolveBranchDisplay(branchIds, allBranches, null);
			branchCell.setCellValue("Branch: " + branchDisplay);
			branchCell.setCellStyle(centeredStyle);
			sheet.addMergedRegion(new CellRangeAddress(rowNum, rowNum, 0, numColumns - 1));
			rowNum++;

			DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");

			Row headerRow = sheet.createRow(rowNum++);
			String[] headers = {
					"S.No", "Staff Code", "First Name", "Last Name", "Email", "Phone #",
					"Active", "Date of Joining", "Relieving Date", "Branch Name", "Branch Access",
					"Role Name", "Reports To", "Monthly Salary (INR)", "Monthly Incentive Allocation (INR)"
			};

			for (int i = 0; i < headers.length; i++) {
				Cell cell = headerRow.createCell(i);
				cell.setCellValue(headers[i]);
				cell.setCellStyle(headerStyle);
			}

			Map<Integer, String> managerNameCache = new HashMap<>();
			int serialNo = 1;
			for (Employee emp : employees) {
				// Defensive check to avoid mixed-role rows in role-based exports.
				if (roleId != null) {
					if (emp.getRoleId() == null || emp.getRoleId().getRoleId() == null
							|| !roleId.equals(emp.getRoleId().getRoleId())) {
						continue;
					}
				}

				Row dataRow = sheet.createRow(rowNum++);
				int col = 0;

				Cell c0 = dataRow.createCell(col++);
				c0.setCellValue(serialNo++);
				c0.setCellStyle(cellStyle);

				Cell c1 = dataRow.createCell(col++);
				c1.setCellValue(emp.getEmployeeCode() != null ? emp.getEmployeeCode() : "");
				c1.setCellStyle(cellStyle);

				Cell c2 = dataRow.createCell(col++);
				c2.setCellValue(emp.getFirstName() != null ? emp.getFirstName() : "");
				c2.setCellStyle(cellStyle);

				Cell c3 = dataRow.createCell(col++);
				c3.setCellValue(emp.getLastName() != null ? emp.getLastName() : "");
				c3.setCellStyle(cellStyle);

				Cell c4 = dataRow.createCell(col++);
				c4.setCellValue(emp.getEmailId() != null ? emp.getEmailId() : "");
				c4.setCellStyle(cellStyle);

				Cell c5 = dataRow.createCell(col++);
				c5.setCellValue(emp.getPhoneNumber() != null ? emp.getPhoneNumber() : "");
				c5.setCellStyle(cellStyle);

				Cell c6 = dataRow.createCell(col++);
				c6.setCellValue(emp.getIsActive() != null && emp.getIsActive() ? "Yes" : "No");
				c6.setCellStyle(cellStyle);

				Cell c7 = dataRow.createCell(col++);
				c7.setCellValue(emp.getJoinDate() != null ? emp.getJoinDate().format(dateFormatter) : "");
				c7.setCellStyle(cellStyle);

				Cell c8 = dataRow.createCell(col++);
				c8.setCellValue(emp.getRelieveDate() != null ? emp.getRelieveDate().format(dateFormatter) : "");
				c8.setCellStyle(cellStyle);

				Cell c9 = dataRow.createCell(col++);
				c9.setCellValue(emp.getLocation() != null && emp.getLocation().getLocationName() != null
						? emp.getLocation().getLocationName() : "");
				c9.setCellStyle(cellStyle);

				Cell c10 = dataRow.createCell(col++);
				LinkedHashSet<String> branchAccessNames = new LinkedHashSet<>();
				if (emp.getLocation() != null && emp.getLocation().getLocationName() != null
						&& !emp.getLocation().getLocationName().isBlank()) {
					branchAccessNames.add(emp.getLocation().getLocationName().trim());
				}
				if (emp.getEmployeeId() != null) {
					List<EmployeeLocationAccess> accessList = employeeLocationAccessRepository
							.findByEmployeeEmployeeIdAndIsActiveTrue(emp.getEmployeeId());
					if (accessList != null) {
						for (EmployeeLocationAccess access : accessList) {
							if (access.getLocation() != null && access.getLocation().getLocationName() != null
									&& !access.getLocation().getLocationName().isBlank()) {
								branchAccessNames.add(access.getLocation().getLocationName().trim());
							}
						}
					}
				}
				c10.setCellValue(String.join(", ", branchAccessNames));
				c10.setCellStyle(cellStyle);

				Cell c11 = dataRow.createCell(col++);
				c11.setCellValue(emp.getRoleId() != null && emp.getRoleId().getRoleName() != null
						? emp.getRoleId().getRoleName() : "");
				c11.setCellStyle(cellStyle);

				Cell c12 = dataRow.createCell(col++);
				c12.setCellValue(resolveManagerName(emp.getReportingTo(), managerNameCache));
				c12.setCellStyle(cellStyle);

				Cell c13 = dataRow.createCell(col++);
				long monthlySalary = emp.getSalary() != null
						? emp.getSalary().setScale(0, RoundingMode.DOWN).longValue()
						: 0L;
				c13.setCellValue(monthlySalary);
				c13.setCellStyle(cellStyle);

				Cell c14 = dataRow.createCell(col++);
				long monthlyIncentiveAllocation = emp.getIncentive() != null
						? emp.getIncentive().setScale(0, RoundingMode.DOWN).longValue()
						: 0L;
				c14.setCellValue(monthlyIncentiveAllocation);
				c14.setCellStyle(cellStyle);
			}

			for (int i = 0; i < headers.length; i++) {
				sheet.autoSizeColumn(i);
			}

			String reportsDir = "reports";
			Path reportsDirPath = Paths.get(reportsDir);
			if (!Files.exists(reportsDirPath)) {
				Files.createDirectories(reportsDirPath);
			}

			String fromLabel = effectiveFromDate != null ? effectiveFromDate.toString() : "all";
			String toLabel = effectiveToDate != null ? effectiveToDate.toString() : "all";
			String filename = "Staff_Details_" + fromLabel + "_to_" + toLabel + "_" + System.currentTimeMillis()
					+ ".xlsx";
			Path filePath = reportsDirPath.resolve(filename);

			try (FileOutputStream fileOut = new FileOutputStream(filePath.toFile())) {
				workbook.write(fileOut);
			}

			String absolutePath = filePath.toAbsolutePath().toString();
			log.info("Staff details report saved to: {}", absolutePath);
			return absolutePath;
		}
	}

	/**
	 * Generate Compliance Incentive Report (weighted-daily).
	 * Fetches submissions by submitted_at range, calculates per-day incentive
	 * using question-weightage scoring (same logic as /employee-month-progress),
	 * and writes one row per employee-day into the Excel.
	 *
	 * @return absolute path of the saved file
	 */
	@Transactional(readOnly = true)
	public String generateWeightedDailyReportExcel(Integer orgId, LocalDate fromDate, LocalDate toDate,
			List<Integer> branchIds, boolean allBranches, Integer staffId) throws Exception {
		log.info("Generating Compliance Incentive (weighted-daily) report - orgId={}, from={}, to={}, staffId={}",
				orgId, fromDate, toDate, staffId);

		// --- 1. Resolve employee list ---
		List<Employee> employees = employeeRepository.findEmployeesByFilters(orgId, null);
		if (employees == null) {
			employees = new ArrayList<>();
		}

		if (!allBranches && branchIds != null && !branchIds.isEmpty()) {
			final List<Integer> filteredBranchIds = branchIds;
			employees = employees.stream()
					.filter(emp -> emp.getLocation() != null
							&& emp.getLocation().getLocationId() != null
							&& filteredBranchIds.contains(emp.getLocation().getLocationId()))
					.toList();
		}

		if (staffId != null) {
			employees = employees.stream()
					.filter(emp -> staffId.equals(emp.getEmployeeId()))
					.toList();
		}

		employees = employees.stream()
				.filter(emp -> Boolean.TRUE.equals(emp.getIsActive()))
				.sorted(Comparator.comparing(Employee::getEmployeeId, Comparator.nullsLast(Integer::compareTo)))
				.toList();

		// --- 2. Build workbook ---
		try (Workbook workbook = new XSSFWorkbook()) {
			Sheet sheet = workbook.createSheet("Compliance Incentive Report");

			// Cell styles
			CellStyle headerStyle = workbook.createCellStyle();
			Font headerFont = workbook.createFont();
			headerFont.setBold(true);
			headerFont.setColor(IndexedColors.WHITE.getIndex());
			headerStyle.setFont(headerFont);
			headerStyle.setBorderBottom(BorderStyle.THIN);
			headerStyle.setBorderTop(BorderStyle.THIN);
			headerStyle.setBorderLeft(BorderStyle.THIN);
			headerStyle.setBorderRight(BorderStyle.THIN);
			applyBlueBackground(headerStyle);

			CellStyle cellStyle = workbook.createCellStyle();
			cellStyle.setBorderBottom(BorderStyle.THIN);
			cellStyle.setBorderTop(BorderStyle.THIN);
			cellStyle.setBorderLeft(BorderStyle.THIN);
			cellStyle.setBorderRight(BorderStyle.THIN);

			CellStyle totalStyle = workbook.createCellStyle();
			Font totalFont = workbook.createFont();
			totalFont.setBold(true);
			totalStyle.setFont(totalFont);
			totalStyle.setBorderBottom(BorderStyle.THIN);
			totalStyle.setBorderTop(BorderStyle.THIN);
			totalStyle.setBorderLeft(BorderStyle.THIN);
			totalStyle.setBorderRight(BorderStyle.THIN);

			CellStyle centeredStyle = workbook.createCellStyle();
			Font centeredFont = workbook.createFont();
			centeredFont.setBold(true);
			centeredFont.setFontHeightInPoints((short) 12);
			centeredStyle.setFont(centeredFont);
			centeredStyle.setAlignment(HorizontalAlignment.CENTER);

			int rowNum = 0;
			final int numColumns = 9;
			DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");

			// Org name header
			Optional<Org> orgOpt = orgRepository.findById(orgId);
			String orgName = orgOpt.map(Org::getOrgName).orElse("Unknown Organization");

			Row orgRow = sheet.createRow(rowNum);
			Cell orgCell = orgRow.createCell(0);
			orgCell.setCellValue(orgName);
			orgCell.setCellStyle(centeredStyle);
			sheet.addMergedRegion(new CellRangeAddress(rowNum, rowNum, 0, numColumns - 1));
			rowNum++;

			// Branch header
			Row branchRow = sheet.createRow(rowNum);
			String branchDisplay = resolveBranchDisplay(branchIds, allBranches, staffId);
			Cell branchCell = branchRow.createCell(0);
			branchCell.setCellValue("Branch: " + branchDisplay);
			branchCell.setCellStyle(centeredStyle);
			sheet.addMergedRegion(new CellRangeAddress(rowNum, rowNum, 0, numColumns - 1));
			rowNum++;

			// Date range header
			Row dateRow = sheet.createRow(rowNum);
			Cell dateCell = dateRow.createCell(0);
			dateCell.setCellValue("From: " + fromDate.format(dateFormatter) + "    To: " + toDate.format(dateFormatter));
			dateCell.setCellStyle(centeredStyle);
			sheet.addMergedRegion(new CellRangeAddress(rowNum, rowNum, 0, numColumns - 1));
			rowNum++;

			// Column headers
			String[] headers = {
					"S.No", "Staff Code", "Staff Name", "Role Name",
					"Compliance Date", "Question", "Answer", "Reason", "Weightage"
			};
			Row headerRow = sheet.createRow(rowNum++);
			for (int i = 0; i < headers.length; i++) {
				Cell cell = headerRow.createCell(i);
				cell.setCellValue(headers[i]);
				cell.setCellStyle(headerStyle);
			}

			// --- 3. Build submitted_at date range for repository query ---
			java.time.ZoneId zoneId = java.time.ZoneId.systemDefault();
			java.time.OffsetDateTime startDateTime = fromDate.atStartOfDay(zoneId).toOffsetDateTime();
			java.time.OffsetDateTime endDateTime = toDate.plusDays(1).atStartOfDay(zoneId).toOffsetDateTime();

			// Shared option-value → option-ID cache
			Map<Integer, Map<String, Integer>> optionValueToIdCache = new HashMap<>();
			int serialNo = 1;

			// --- 4. Per-employee processing ---
			for (Employee emp : employees) {
				if (emp.getEmployeeId() == null) continue;

				String employeeName = ((emp.getFirstName() != null ? emp.getFirstName() : "")
						+ (emp.getLastName() != null ? " " + emp.getLastName() : "")).trim();
				String employeeCode = emp.getEmployeeCode() != null ? emp.getEmployeeCode() : "";
				String employeeRole = emp.getRoleId() != null ? emp.getRoleId().getRoleName() : "";
				BigDecimal totalEmpIncentive = emp.getIncentive() != null ? emp.getIncentive() : BigDecimal.ZERO;

				// Fetch active submissions by submitted_at range
				List<Submission> submissions = submissionRepository.findActiveByEmployeeAndSubmittedAtRange(
						emp.getEmployeeId(), startDateTime, endDateTime);

				if (submissions == null || submissions.isEmpty()) continue;

				// Group by YearMonth of question_date so each month uses its own working-days rate
				Map<YearMonth, List<Submission>> byMonth = submissions.stream()
						.filter(s -> s.getQuestionDate() != null)
						.collect(Collectors.groupingBy(s -> YearMonth.from(s.getQuestionDate())));

				// Per-month working-days cache to avoid redundant DB calls
				Map<YearMonth, Long> monthWorkingDaysCache = new HashMap<>();

				// Sorted unique question_dates for chronological row ordering
				List<LocalDate> sortedDates = submissions.stream()
						.filter(s -> s.getQuestionDate() != null)
						.map(Submission::getQuestionDate)
						.distinct()
						.sorted()
						.collect(Collectors.toList());

				if (sortedDates.isEmpty()) continue;

				// Track first row of this employee block for cross-date merging
				int firstRowForEmployee = rowNum;
				boolean firstDateWritten = false;

				for (LocalDate date : sortedDates) {
					YearMonth ym = YearMonth.from(date);
					List<Submission> monthSubs = byMonth.getOrDefault(ym, new ArrayList<>());

					// Submissions for this specific day
					List<Submission> daySubs = monthSubs.stream()
							.filter(s -> date.equals(s.getQuestionDate()))
							.collect(Collectors.toList());
					if (daySubs.isEmpty()) continue;

					// Full-month working days for per-day incentive rate
					long orgWorkingDays = monthWorkingDaysCache.computeIfAbsent(ym,
							m -> countWorkingDaysInFullMonth(orgId, m));
					if (orgWorkingDays <= 0) continue;

					BigDecimal perDayIncentive = (totalEmpIncentive.compareTo(BigDecimal.ZERO) > 0)
							? totalEmpIncentive.divide(BigDecimal.valueOf(orgWorkingDays), 4, RoundingMode.HALF_UP)
							: BigDecimal.ZERO;

					// Per-question weightage scoring (same rules as /employee-month-progress)
					double dayTotalWeightage = 0.0;
					double dayValidWeightage = 0.0;

					// Collect per-question audit data first, then write rows
					List<String[]> questionRows = new ArrayList<>(); // [questionText, answerGiven, validAnswer, isValid, weightage]

					for (Submission submission : daySubs) {
						if (submission == null || submission.getQuestionBank() == null) continue;
						com.spearhead.ufc.model.QuestionBank question = submission.getQuestionBank();
						Double weightage = question.getWeightage();
						String questionText = question.getQuestionText() != null ? question.getQuestionText() : "";
						String answerGiven = submission.getAnswer() != null ? submission.getAnswer() : "";
						// [questionText, answerGiven, weightage]
						String remarksValue = submission.getRemarks() != null ? submission.getRemarks() : "";
					questionRows.add(new String[]{questionText, answerGiven, remarksValue, weightage != null ? String.valueOf(weightage) : "0"});
					}

					if (questionRows.isEmpty()) continue;

					// Write one row per question; date written once per date block; employee info once per employee block
					int firstRowForDay = rowNum;
					for (int qi = 0; qi < questionRows.size(); qi++) {
						String[] qr = questionRows.get(qi);
						Row dataRow = sheet.createRow(rowNum++);
						boolean isFirstQuestion = (qi == 0);
						boolean isVeryFirstRow = (!firstDateWritten && isFirstQuestion);

						// S.No — only on first row of the entire employee block
						Cell snoCell = dataRow.createCell(0);
						snoCell.setCellValue(isVeryFirstRow ? serialNo : 0);
						snoCell.setCellStyle(cellStyle);

						Cell codeCell = dataRow.createCell(1);
						codeCell.setCellValue(isVeryFirstRow ? employeeCode : "");
						codeCell.setCellStyle(cellStyle);

						Cell nameCell = dataRow.createCell(2);
						nameCell.setCellValue(isVeryFirstRow ? employeeName : "");
						nameCell.setCellStyle(cellStyle);

						Cell roleCell = dataRow.createCell(3);
						roleCell.setCellValue(isVeryFirstRow ? employeeRole : "");
						roleCell.setCellStyle(cellStyle);

						// Date — only on first question row of each date block
						Cell dateCell2 = dataRow.createCell(4);
						dateCell2.setCellValue(isFirstQuestion ? date.format(dateFormatter) : "");
						dateCell2.setCellStyle(cellStyle);

						// Question text
						Cell questionCell = dataRow.createCell(5);
						questionCell.setCellValue(qr[0]);
						questionCell.setCellStyle(cellStyle);

						// Answer
						Cell answerCell = dataRow.createCell(6);
						answerCell.setCellValue(qr[1]);
						answerCell.setCellStyle(cellStyle);

						// Remarks
						Cell remarksCell = dataRow.createCell(7);
						remarksCell.setCellValue(qr[2]);
						remarksCell.setCellStyle(cellStyle);

						// Weightage of this question
						Cell wgtCell = dataRow.createCell(8);
						try { wgtCell.setCellValue(Double.parseDouble(qr[3])); } catch (Exception ignore) { wgtCell.setCellValue(qr[3]); }
						wgtCell.setCellStyle(cellStyle);
					}

					// Merge Date across question rows for this date block
					if (questionRows.size() > 1) {
						int lastRow = rowNum - 1;
						sheet.addMergedRegion(new CellRangeAddress(firstRowForDay, lastRow, 4, 4)); // Date
					}

					firstDateWritten = true;
				}

				// Merge S.No, Staff Code, Staff Name, Role across ALL date blocks for this employee
				if (rowNum > firstRowForEmployee) {
					int lastEmpRow = rowNum - 1;
					if (lastEmpRow > firstRowForEmployee) {
						sheet.addMergedRegion(new CellRangeAddress(firstRowForEmployee, lastEmpRow, 0, 0)); // S.No
						sheet.addMergedRegion(new CellRangeAddress(firstRowForEmployee, lastEmpRow, 1, 1)); // Staff Code
						sheet.addMergedRegion(new CellRangeAddress(firstRowForEmployee, lastEmpRow, 2, 2)); // Staff Name
						sheet.addMergedRegion(new CellRangeAddress(firstRowForEmployee, lastEmpRow, 3, 3)); // Role
					}
				}
				serialNo++;
			}

			// Set column widths — explicit minimums
			int[] minWidths = {1500, 3500, 7000, 6000, 3500, 14000, 9000, 9000, 2500};
			for (int i = 0; i < numColumns; i++) {
				sheet.autoSizeColumn(i);
				if (sheet.getColumnWidth(i) < minWidths[i]) {
					sheet.setColumnWidth(i, minWidths[i]);
				}
			}

			// Save file to reports directory
			Path reportsDirPath = Paths.get("reports");
			if (!Files.exists(reportsDirPath)) {
				Files.createDirectories(reportsDirPath);
				log.info("Created reports directory: {}", reportsDirPath.toAbsolutePath());
			}

			String filename = "Compliance_Incentive_Report_" + fromDate + "_to_" + toDate
					+ "_" + System.currentTimeMillis() + ".xlsx";
			Path filePath = reportsDirPath.resolve(filename);

			try (FileOutputStream fileOut = new FileOutputStream(filePath.toFile())) {
				workbook.write(fileOut);
			}

			String absolutePath = filePath.toAbsolutePath().toString();
			log.info("Compliance Incentive report saved to: {}", absolutePath);
			return absolutePath;
		}
	}


}
