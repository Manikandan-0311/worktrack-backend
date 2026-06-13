package com.spearhead.ufc.dto;

import java.time.LocalDate;

public class EmployeeProgressDTO {
    private Integer employeeId;
    private String employeeName;
    private LocalDate date;
    private int totalScore;
    private int totalQuestions;
    private int answeredQuestions;

    public EmployeeProgressDTO(Integer employeeId, String employeeName, LocalDate date, int totalScore, int totalQuestions, int answeredQuestions) {
        this.employeeId = employeeId;
        this.employeeName = employeeName;
        this.date = date;
        this.totalScore = totalScore;
        this.totalQuestions = totalQuestions;
        this.answeredQuestions = answeredQuestions;
    }

    public Integer getEmployeeId() { return employeeId; }
    public String getEmployeeName() { return employeeName; }
    public LocalDate getDate() { return date; }
    public int getTotalScore() { return totalScore; }
    public int getTotalQuestions() { return totalQuestions; }
    public int getAnsweredQuestions() { return answeredQuestions; }

    public void setEmployeeId(Integer employeeId) { this.employeeId = employeeId; }
    public void setEmployeeName(String employeeName) { this.employeeName = employeeName; }
    public void setDate(LocalDate date) { this.date = date; }
    public void setTotalScore(int totalScore) { this.totalScore = totalScore; }
    public void setTotalQuestions(int totalQuestions) { this.totalQuestions = totalQuestions; }
    public void setAnsweredQuestions(int answeredQuestions) { this.answeredQuestions = answeredQuestions; }
}
