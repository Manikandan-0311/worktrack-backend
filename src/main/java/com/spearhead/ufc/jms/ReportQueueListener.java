package com.spearhead.ufc.jms;

import com.spearhead.ufc.config.JmsConfig;
import com.spearhead.ufc.model.Employee;
import com.spearhead.ufc.model.RequestJobInfo;
import com.spearhead.ufc.repository.EmployeeRepository;
import com.spearhead.ufc.repository.RequestJobInfoRepository;
import com.spearhead.ufc.service.DashboardSummaryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

import java.io.File;
import java.time.OffsetDateTime;
import java.util.ArrayList;

/**
 * Consumes {@link ReportRequest} messages from the report generation queue and
 * delegates to {@link DashboardSummaryService} to produce the Excel file.
 * The resulting file path and status are written back to {@link ReportStatusStore}
 * and persisted to the {@code base.request_job_info} table.
 */
@Component
public class ReportQueueListener {

    private static final Logger log = LoggerFactory.getLogger(ReportQueueListener.class);

    @Autowired
    private DashboardSummaryService dashboardSummaryService;

    @Autowired
    private ReportStatusStore reportStatusStore;

    @Autowired
    private RequestJobInfoRepository requestJobInfoRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @JmsListener(destination = JmsConfig.REPORT_QUEUE,
                 containerFactory = "jmsListenerContainerFactory")
    public void onReportRequest(ReportRequest request) {
        String requestId = request.getRequestId();
        log.info("Picked up report request from queue - requestId={}, reportType={}", requestId, request.getReportType());

        reportStatusStore.setInProgress(requestId);

        // Persist a new job record with IN_PROGRESS status
        RequestJobInfo jobRecord = null;
        try {
            if (request.getRequestedByEmployeeId() != null) {
                // getReferenceById returns a proxy (FK reference only) — avoids detached entity issues
                Employee employeeRef = employeeRepository.getReferenceById(request.getRequestedByEmployeeId());
                OffsetDateTime now = OffsetDateTime.now();
                jobRecord = RequestJobInfo.builder()
                        .requestNo(requestId)
                        .jobType(request.getReportType())
                        .programName("Compliance Incentive Report")
                        .requestStatus("IN_PROGRESS")
                        .requestedBy(String.valueOf(request.getRequestedByEmployeeId()))
                        .requestDt(now)
                        .updatedDt(now)
                        .employee(employeeRef)
                        .build();
                jobRecord = requestJobInfoRepository.save(jobRecord);
                log.info("RequestJobInfo created - requestId={}, dbId={}", requestId, jobRecord.getRequestId());
            }
        } catch (Exception ex) {
            log.error("Could not persist RequestJobInfo on start - requestId={}: {}", requestId, ex.getMessage(), ex);
        }

        try {
            Integer orgId = request.getOrgId();
            java.time.LocalDate fromDate = request.getFromDate();
            java.time.LocalDate toDate = request.getToDate();
            java.util.List<Integer> branchIds = request.getBranchIds() != null ? request.getBranchIds() : new ArrayList<>();
            boolean allBranches = request.isAllBranches();
            Integer staffId = request.getStaffId();
            String staffName = request.getStaffName();
            String reportType = request.getReportType() != null ? request.getReportType().toLowerCase() : "summary";

            String filePath;
            if ("daily".equals(reportType)) {
                filePath = dashboardSummaryService.generateDailyProgressReportExcel(
                        orgId, fromDate, toDate, branchIds, allBranches, staffId, staffName);
            } else if ("staff".equals(reportType)) {
                filePath = dashboardSummaryService.generateStaffDetailsReportExcel(
                        orgId, fromDate, toDate, branchIds, allBranches);
            } else if ("weighted-daily".equals(reportType)) {
                filePath = dashboardSummaryService.generateWeightedDailyReportExcel(
                        orgId, fromDate, toDate, branchIds, allBranches, staffId);
            } else {
                // default: summary
                filePath = dashboardSummaryService.generateIncentiveReportExcel(
                        orgId, fromDate, toDate, branchIds, allBranches, staffId, staffName);
            }

            String filename = new File(filePath).getName();
            reportStatusStore.setCompleted(requestId, filePath, filename);
            log.info("Report generation completed - requestId={}, filename={}", requestId, filename);

            // Update job record to COMPLETED
            if (jobRecord != null) {
                OffsetDateTime now = OffsetDateTime.now();
                requestJobInfoRepository.updateStatusAndFilePath(
                        jobRecord.getRequestId(), "COMPLETED", filePath, now, now);
                log.info("RequestJobInfo updated to COMPLETED - dbId={}", jobRecord.getRequestId());
            }

        } catch (Exception e) {
            log.error("Report generation failed - requestId={}", requestId, e);
            reportStatusStore.setFailed(requestId, e.getMessage());

            // Update job record to FAILED
            if (jobRecord != null) {
                try {
                    String errorMsg = e.getMessage() != null
                            ? e.getMessage().substring(0, Math.min(e.getMessage().length(), 490))
                            : "Unknown error";
                    OffsetDateTime now = OffsetDateTime.now();
                    requestJobInfoRepository.updateStatusWithError(
                            jobRecord.getRequestId(), "FAILED", errorMsg, now, now);
                    log.info("RequestJobInfo updated to FAILED - dbId={}", jobRecord.getRequestId());
                } catch (Exception updateEx) {
                    log.warn("Could not update RequestJobInfo to FAILED - dbId={}: {}",
                            jobRecord.getRequestId(), updateEx.getMessage());
                }
            }
        }
    }
}
