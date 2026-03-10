package com.example.studentsystem.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record StudentRequest(
        @NotBlank String studentNo,
        @NotBlank String name,
        @NotNull Integer enrollmentYear,
        @NotNull Long collegeId,
        @NotNull Long majorId,
        @NotNull Boolean employed
) {
}
