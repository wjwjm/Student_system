package com.example.studentsystem.repository;

import com.example.studentsystem.entity.Major;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MajorRepository extends JpaRepository<Major, Long> {
}
