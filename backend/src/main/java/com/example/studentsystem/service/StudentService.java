package com.example.studentsystem.service;

import com.example.studentsystem.dto.EmploymentStatResponse;
import com.example.studentsystem.dto.StudentRequest;
import com.example.studentsystem.dto.StudentResponse;
import org.springframework.data.domain.Page;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface StudentService {
    StudentResponse create(StudentRequest request);
    StudentResponse update(Long id, StudentRequest request);
    void delete(Long id);
    StudentResponse getById(Long id);
    Page<StudentResponse> list(String keyword, Long collegeId, Long majorId, Integer year, int page, int size);
    byte[] exportXlsx() throws IOException;
    byte[] exportCsv() throws IOException;
    void importFile(MultipartFile file) throws IOException;
    byte[] downloadTemplate(String type) throws IOException;
    List<EmploymentStatResponse> employmentStats();
}
