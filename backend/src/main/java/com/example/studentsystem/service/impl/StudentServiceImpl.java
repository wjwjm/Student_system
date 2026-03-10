package com.example.studentsystem.service.impl;

import com.example.studentsystem.dto.EmploymentStatResponse;
import com.example.studentsystem.dto.StudentRequest;
import com.example.studentsystem.dto.StudentResponse;
import com.example.studentsystem.entity.College;
import com.example.studentsystem.entity.Major;
import com.example.studentsystem.entity.Student;
import com.example.studentsystem.repository.CollegeRepository;
import com.example.studentsystem.repository.MajorRepository;
import com.example.studentsystem.repository.StudentRepository;
import com.example.studentsystem.service.StudentService;
import jakarta.persistence.EntityNotFoundException;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
@Transactional
public class StudentServiceImpl implements StudentService {
    private final StudentRepository studentRepository;
    private final CollegeRepository collegeRepository;
    private final MajorRepository majorRepository;

    public StudentServiceImpl(StudentRepository studentRepository, CollegeRepository collegeRepository, MajorRepository majorRepository) {
        this.studentRepository = studentRepository;
        this.collegeRepository = collegeRepository;
        this.majorRepository = majorRepository;
    }

    @Override
    public StudentResponse create(StudentRequest request) {
        Student student = toEntity(new Student(), request);
        return toResponse(studentRepository.save(student));
    }

    @Override
    public StudentResponse update(Long id, StudentRequest request) {
        Student student = studentRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("student not found"));
        return toResponse(studentRepository.save(toEntity(student, request)));
    }

    @Override
    public void delete(Long id) {
        studentRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public StudentResponse getById(Long id) {
        return studentRepository.findById(id).map(this::toResponse)
                .orElseThrow(() -> new EntityNotFoundException("student not found"));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<StudentResponse> list(String keyword, Long collegeId, Long majorId, Integer year, int page, int size) {
        return studentRepository.search(keyword, collegeId, majorId, year, PageRequest.of(page, size)).map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportXlsx() throws IOException {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("students");
            writeHeader(sheet.createRow(0));
            int rowNum = 1;
            for (Student s : studentRepository.findAll()) {
                writeRow(sheet.createRow(rowNum++), s);
            }
            workbook.write(out);
            return out.toByteArray();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportCsv() throws IOException {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream();
             OutputStreamWriter writer = new OutputStreamWriter(out, StandardCharsets.UTF_8);
             CSVPrinter printer = new CSVPrinter(writer, CSVFormat.DEFAULT.withHeader("studentNo", "name", "enrollmentYear", "collegeId", "majorId", "employed"))) {
            for (Student s : studentRepository.findAll()) {
                printer.printRecord(s.getStudentNo(), s.getName(), s.getEnrollmentYear(), s.getCollege().getId(), s.getMajor().getId(), s.getEmployed());
            }
            printer.flush();
            return out.toByteArray();
        }
    }

    @Override
    public void importFile(MultipartFile file) throws IOException {
        String fileName = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase();
        if (fileName.endsWith(".xlsx")) {
            importXlsx(file.getInputStream());
        } else if (fileName.endsWith(".csv")) {
            importCsv(file.getInputStream());
        } else {
            throw new IllegalArgumentException("Only .xlsx and .csv are supported");
        }
    }

    @Override
    public byte[] downloadTemplate(String type) throws IOException {
        if ("xlsx".equalsIgnoreCase(type)) {
            try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                writeHeader(workbook.createSheet("template").createRow(0));
                workbook.write(out);
                return out.toByteArray();
            }
        }
        return "studentNo,name,enrollmentYear,collegeId,majorId,employed\n".getBytes(StandardCharsets.UTF_8);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EmploymentStatResponse> employmentStats() {
        return studentRepository.employmentStats().stream().map(arr -> {
            long total = ((Number) arr[3]).longValue();
            long employed = ((Number) arr[4]).longValue();
            double rate = total == 0 ? 0 : employed * 100.0 / total;
            return new EmploymentStatResponse((String) arr[0], (String) arr[1], (Integer) arr[2], total, employed, rate);
        }).toList();
    }

    private void importXlsx(InputStream inputStream) throws IOException {
        try (Workbook workbook = new XSSFWorkbook(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                StudentRequest request = new StudentRequest(
                        row.getCell(0).getStringCellValue(),
                        row.getCell(1).getStringCellValue(),
                        (int) row.getCell(2).getNumericCellValue(),
                        (long) row.getCell(3).getNumericCellValue(),
                        (long) row.getCell(4).getNumericCellValue(),
                        row.getCell(5).getBooleanCellValue()
                );
                create(request);
            }
        }
    }

    private void importCsv(InputStream inputStream) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
             CSVParser parser = CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).build().parse(reader)) {
            parser.forEach(record -> create(new StudentRequest(
                    record.get("studentNo"),
                    record.get("name"),
                    Integer.parseInt(record.get("enrollmentYear")),
                    Long.parseLong(record.get("collegeId")),
                    Long.parseLong(record.get("majorId")),
                    Boolean.parseBoolean(record.get("employed"))
            )));
        }
    }

    private Student toEntity(Student student, StudentRequest request) {
        College college = collegeRepository.findById(request.collegeId()).orElseThrow(() -> new EntityNotFoundException("college not found"));
        Major major = majorRepository.findById(request.majorId()).orElseThrow(() -> new EntityNotFoundException("major not found"));
        student.setStudentNo(request.studentNo());
        student.setName(request.name());
        student.setEnrollmentYear(request.enrollmentYear());
        student.setCollege(college);
        student.setMajor(major);
        student.setEmployed(request.employed());
        return student;
    }

    private StudentResponse toResponse(Student s) {
        return new StudentResponse(s.getId(), s.getStudentNo(), s.getName(), s.getEnrollmentYear(),
                s.getCollege().getId(), s.getCollege().getName(), s.getMajor().getId(), s.getMajor().getName(), s.getEmployed());
    }

    private void writeHeader(Row row) {
        row.createCell(0).setCellValue("studentNo");
        row.createCell(1).setCellValue("name");
        row.createCell(2).setCellValue("enrollmentYear");
        row.createCell(3).setCellValue("collegeId");
        row.createCell(4).setCellValue("majorId");
        row.createCell(5).setCellValue("employed");
    }

    private void writeRow(Row row, Student s) {
        row.createCell(0).setCellValue(s.getStudentNo());
        row.createCell(1).setCellValue(s.getName());
        row.createCell(2).setCellValue(s.getEnrollmentYear());
        row.createCell(3).setCellValue(s.getCollege().getId());
        row.createCell(4).setCellValue(s.getMajor().getId());
        row.createCell(5).setCellValue(Boolean.TRUE.equals(s.getEmployed()));
    }
}
