package com.spearhead.ufc.dto;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Data
public class DashboardDTO {

    // Summary cards
    private Double todayCompliance;
    private Double totalIncentive;
    private Double totalDeduction;

    // Top performers: employeeId → avgCompliance
    private Map<Integer, Double> topPerformers;

    // Date-wise charts
    private List<LocalDate> dates;
    private List<Double> dateWiseCompliance;
    private List<Double> submissionPercentages;
    private List<Double> incentives;
    private List<Double> deductions;

    // Department-wise compliance
    private List<String> departmentNames;
    private List<Double> departmentCompliance;
}
