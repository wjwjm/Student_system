package com.example.studentsystem.dto;

public record StudentResponse(
        Long id,
        String studentNo,
        String name,
        Integer enrollmentYear,
        Long collegeId,
        String collegeName,
        Long majorId,
        String majorName,
        Boolean employed
) {
}
