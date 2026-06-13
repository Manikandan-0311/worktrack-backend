package com.spearhead.ufc.dto;

import java.time.LocalDate;
import java.util.List;

public class EmployeeQuestionHistoryDTO {
    private Integer employeeId;
    private String employeeName;
    private LocalDate selectedDate;
    private Integer questionId;
    private String questionText;
    private String questionType;
    private Double weightage;
    private LocalDate fromDate;
    private LocalDate toDate;
    private List<OptionDTO> optionDTOs;
    private String answer;
    private boolean submittedOnDate;
    private int marksAwarded;
    private String remarks;
    private double validWeightage;
    private boolean isValid;
    private String validAnswer;

    public EmployeeQuestionHistoryDTO() {}

    public EmployeeQuestionHistoryDTO(Integer employeeId, String employeeName, LocalDate selectedDate, Integer questionId, String questionText,
                                     String questionType, Double weightage, LocalDate fromDate, LocalDate toDate,
                                     List<OptionDTO> optionDTOs, String answer, boolean submittedOnDate, int marksAwarded,String remarks) {
        this.employeeId = employeeId;
        this.employeeName = employeeName;
        this.selectedDate = selectedDate;
        this.questionId = questionId;
        this.questionText = questionText;
        this.questionType = questionType;
        this.weightage = weightage;
        this.fromDate = fromDate;
        this.toDate = toDate;
        this.optionDTOs = optionDTOs;
        this.answer = answer;
        this.submittedOnDate = submittedOnDate;
        this.marksAwarded = marksAwarded;
        this.remarks = remarks;
    }

    public String getEmployeeName() {
        return employeeName;
    }
    public void setEmployeeName(String employeeName) {
        this.employeeName = employeeName;
    }

    public Integer getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(Integer employeeId) {
        this.employeeId = employeeId;
    }

    public LocalDate getSelectedDate() {
        return selectedDate;
    }

    public void setSelectedDate(LocalDate selectedDate) {
        this.selectedDate = selectedDate;
    }

    public Integer getQuestionId() {
        return questionId;
    }

    public void setQuestionId(Integer questionId) {
        this.questionId = questionId;
    }

    public String getQuestionText() {
        return questionText;
    }

    public void setQuestionText(String questionText) {
        this.questionText = questionText;
    }

    public String getQuestionType() {
        return questionType;
    }

    public void setQuestionType(String questionType) {
        this.questionType = questionType;
    }

    public Double getWeightage() {
        return weightage;
    }

    public void setWeightage(Double weightage) {
        this.weightage = weightage;
    }

    public LocalDate getFromDate() {
        return fromDate;
    }

    public void setFromDate(LocalDate fromDate) {
        this.fromDate = fromDate;
    }

    public LocalDate getToDate() {
        return toDate;
    }

    public void setToDate(LocalDate toDate) {
        this.toDate = toDate;
    }

    public List<OptionDTO> getOptionDTOs() {
        return optionDTOs;
    }

    public void setOptionDTOs(List<OptionDTO> optionDTOs) {
        this.optionDTOs = optionDTOs;
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }

    public boolean isSubmittedOnDate() {
        return submittedOnDate;
    }

    public void setSubmittedOnDate(boolean submittedOnDate) {
        this.submittedOnDate = submittedOnDate;
    }

    public int getMarksAwarded() {
        return marksAwarded;
    }

    public void setMarksAwarded(int marksAwarded) {
        this.marksAwarded = marksAwarded;
    }
    public String getRemarks() {
        return remarks;
    }
    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }

    public double getValidWeightage() {
        return validWeightage;
    }
    public void setValidWeightage(double validWeightage) {
        this.validWeightage = validWeightage;
    }

    public boolean isIsValid() {
        return isValid;
    }
    public void setIsValid(boolean isValid) {
        this.isValid = isValid;
    }

    public String getValidAnswer() {
        return validAnswer;
    }
    public void setValidAnswer(String validAnswer) {
        this.validAnswer = validAnswer;
    }
}
