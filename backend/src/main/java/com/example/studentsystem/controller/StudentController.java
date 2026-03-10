package com.example.studentsystem.controller;

import com.example.studentsystem.dto.EmploymentStatResponse;
import com.example.studentsystem.dto.StudentRequest;
import com.example.studentsystem.dto.StudentResponse;
import com.example.studentsystem.service.StudentService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/students")
public class StudentController {
    private final StudentService studentService;

    public StudentController(StudentService studentService) {
        this.studentService = studentService;
    }

    @PostMapping
    public StudentResponse create(@Valid @RequestBody StudentRequest request) {
        return studentService.create(request);
    }

    @PutMapping("/{id}")
    public StudentResponse update(@PathVariable Long id, @Valid @RequestBody StudentRequest request) {
        return studentService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        studentService.delete(id);
    }

    @GetMapping("/{id}")
    public StudentResponse get(@PathVariable Long id) {
        return studentService.getById(id);
    }

    @GetMapping
    public Page<StudentResponse> list(@RequestParam(required = false) String keyword,
                                      @RequestParam(required = false) Long collegeId,
                                      @RequestParam(required = false) Long majorId,
                                      @RequestParam(required = false) Integer year,
                                      @RequestParam(defaultValue = "0") int page,
                                      @RequestParam(defaultValue = "10") int size) {
        return studentService.list(keyword, collegeId, majorId, year, page, size);
    }

    @GetMapping("/stats/employment")
    public List<EmploymentStatResponse> stats() {
        return studentService.employmentStats();
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public void importFile(@RequestPart("file") MultipartFile file) throws IOException {
        studentService.importFile(file);
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> export(@RequestParam(defaultValue = "xlsx") String type) throws IOException {
        byte[] body = "csv".equalsIgnoreCase(type) ? studentService.exportCsv() : studentService.exportXlsx();
        String filename = "students." + ("csv".equalsIgnoreCase(type) ? "csv" : "xlsx");
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename).body(body);
    }

    @GetMapping("/template")
    public ResponseEntity<byte[]> template(@RequestParam(defaultValue = "xlsx") String type) throws IOException {
        byte[] body = studentService.downloadTemplate(type);
        String filename = "student-template." + ("csv".equalsIgnoreCase(type) ? "csv" : "xlsx");
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename).body(body);
    }
}
