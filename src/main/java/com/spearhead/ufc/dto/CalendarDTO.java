package com.spearhead.ufc.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CalendarDTO {
    private LocalDate date;
    private Boolean isActive;
    private String remarks;
}
