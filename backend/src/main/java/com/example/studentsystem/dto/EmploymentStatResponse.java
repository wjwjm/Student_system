package com.example.studentsystem.dto;

public record EmploymentStatResponse(
        String college,
        String major,
        Integer year,
        Long total,
        Long employed,
        Double employmentRate
) {
}
