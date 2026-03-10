package com.example.studentsystem.repository;

import com.example.studentsystem.entity.College;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CollegeRepository extends JpaRepository<College, Long> {
}
