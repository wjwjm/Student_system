package com.example.studentsystem.repository;

import com.example.studentsystem.entity.Student;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface StudentRepository extends JpaRepository<Student, Long> {
    @Query("""
        SELECT s FROM Student s
        WHERE (:keyword IS NULL OR s.name LIKE %:keyword% OR s.studentNo LIKE %:keyword%)
          AND (:collegeId IS NULL OR s.college.id = :collegeId)
          AND (:majorId IS NULL OR s.major.id = :majorId)
          AND (:year IS NULL OR s.enrollmentYear = :year)
    """)
    Page<Student> search(@Param("keyword") String keyword,
                         @Param("collegeId") Long collegeId,
                         @Param("majorId") Long majorId,
                         @Param("year") Integer year,
                         Pageable pageable);

    @Query("""
        SELECT s.college.name, s.major.name, s.enrollmentYear,
               COUNT(s.id), SUM(CASE WHEN s.employed = true THEN 1 ELSE 0 END)
        FROM Student s
        GROUP BY s.college.name, s.major.name, s.enrollmentYear
        ORDER BY s.enrollmentYear DESC
    """)
    List<Object[]> employmentStats();
}
