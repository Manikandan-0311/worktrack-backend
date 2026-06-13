package com.spearhead.ufc.jms;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;

/**
 * JMS message payload for asynchronous report generation requests.
 */
public class ReportRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private String requestId;
    private Integer orgId;
    private LocalDate fromDate;
    private LocalDate toDate;
    private List<Integer> branchIds;
    private boolean allBranches;
    private Integer staffId;
    private String staffName;
    /** "summary" | "daily" | "staff" | "weighted-daily" */
    private String reportType;
    /** employeeId of the user who triggered this request (for RequestJobInfo persistence) */
    private Integer requestedByEmployeeId;

    public ReportRequest() {}

    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }

    public Integer getOrgId() { return orgId; }
    public void setOrgId(Integer orgId) { this.orgId = orgId; }

    public LocalDate getFromDate() { return fromDate; }
    public void setFromDate(LocalDate fromDate) { this.fromDate = fromDate; }

    public LocalDate getToDate() { return toDate; }
    public void setToDate(LocalDate toDate) { this.toDate = toDate; }

    public List<Integer> getBranchIds() { return branchIds; }
    public void setBranchIds(List<Integer> branchIds) { this.branchIds = branchIds; }

    public boolean isAllBranches() { return allBranches; }
    public void setAllBranches(boolean allBranches) { this.allBranches = allBranches; }

    public Integer getStaffId() { return staffId; }
    public void setStaffId(Integer staffId) { this.staffId = staffId; }

    public String getStaffName() { return staffName; }
    public void setStaffName(String staffName) { this.staffName = staffName; }

    public String getReportType() { return reportType; }
    public void setReportType(String reportType) { this.reportType = reportType; }

    public Integer getRequestedByEmployeeId() { return requestedByEmployeeId; }
    public void setRequestedByEmployeeId(Integer requestedByEmployeeId) { this.requestedByEmployeeId = requestedByEmployeeId; }
}
