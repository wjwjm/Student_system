package com.example.studentsystem;

import com.example.studentsystem.entity.College;
import com.example.studentsystem.entity.Major;
import com.example.studentsystem.repository.CollegeRepository;
import com.example.studentsystem.repository.MajorRepository;
import com.example.studentsystem.repository.StudentRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class StudentSystemApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private CollegeRepository collegeRepository;

    @Autowired
    private MajorRepository majorRepository;

    private Long collegeId;
    private Long majorId;

    @BeforeEach
    void setUp() {
        studentRepository.deleteAll();
        majorRepository.deleteAll();
        collegeRepository.deleteAll();

        College college = new College();
        college.setName("计算机学院");
        college = collegeRepository.save(college);
        collegeId = college.getId();

        Major major = new Major();
        major.setName("软件工程");
        major.setCollege(college);
        major = majorRepository.save(major);
        majorId = major.getId();
    }

    @Test
    void authLoginShouldReturnToken() throws Exception {
        String content = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "admin",
                                  "password": "admin123"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isString())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode jsonNode = objectMapper.readTree(content);
        assertThat(jsonNode.get("token").asText()).isNotBlank();
    }

    @Test
    void studentCrudShouldWork() throws Exception {
        String token = loginAndGetToken();

        String created = mockMvc.perform(post("/api/students")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(studentJson("S1001", "张三", 2024, false)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.studentNo").value("S1001"))
                .andExpect(jsonPath("$.name").value("张三"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long studentId = objectMapper.readTree(created).get("id").asLong();

        mockMvc.perform(get("/api/students/{id}", studentId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(studentId));

        mockMvc.perform(put("/api/students/{id}", studentId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(studentJson("S1001", "李四", 2024, true)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("李四"))
                .andExpect(jsonPath("$.employed").value(true));

        mockMvc.perform(get("/api/students")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .param("keyword", "李四"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].studentNo").value("S1001"));

        mockMvc.perform(delete("/api/students/{id}", studentId)
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk());

        assertThat(studentRepository.findById(studentId)).isEmpty();
    }

    @Test
    void importExportAndStatsShouldWork() throws Exception {
        String token = loginAndGetToken();

        String csv = "studentNo,name,enrollmentYear,collegeId,majorId,employed\n"
                + "S2001,王五,2023," + collegeId + "," + majorId + ",true\n"
                + "S2002,赵六,2023," + collegeId + "," + majorId + ",false\n";

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "students.csv",
                "text/csv",
                csv.getBytes(StandardCharsets.UTF_8)
        );

        mockMvc.perform(multipart("/api/students/import")
                        .file(file)
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk());

        assertThat(studentRepository.findAll()).hasSize(2);

        mockMvc.perform(get("/api/students/export")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .param("type", "csv"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=students.csv"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("S2001")));

        mockMvc.perform(get("/api/students/stats/employment")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].year").value(2023))
                .andExpect(jsonPath("$[0].total").value(2))
                .andExpect(jsonPath("$[0].employed").value(1));
    }

    private String loginAndGetToken() throws Exception {
        String content = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "admin",
                                  "password": "admin123"
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(content).get("token").asText();
    }

    private String studentJson(String studentNo, String name, int year, boolean employed) {
        return String.format("""
                {
                  "studentNo": "%s",
                  "name": "%s",
                  "enrollmentYear": %d,
                  "collegeId": %d,
                  "majorId": %d,
                  "employed": %s
                }
                """, studentNo, name, year, collegeId, majorId, employed);
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
