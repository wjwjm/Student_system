CREATE DATABASE IF NOT EXISTS student_system DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE student_system;

CREATE TABLE IF NOT EXISTS college (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(128) NOT NULL UNIQUE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS major (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(128) NOT NULL,
  college_id BIGINT NOT NULL,
  CONSTRAINT fk_major_college FOREIGN KEY (college_id) REFERENCES college(id)
    ON UPDATE CASCADE ON DELETE RESTRICT,
  UNIQUE KEY uk_major_name_college (name, college_id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS student (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  student_no VARCHAR(32) NOT NULL UNIQUE,
  name VARCHAR(64) NOT NULL,
  enrollment_year INT NOT NULL,
  college_id BIGINT NOT NULL,
  major_id BIGINT NOT NULL,
  employed TINYINT(1) NOT NULL DEFAULT 0,
  CONSTRAINT fk_student_college FOREIGN KEY (college_id) REFERENCES college(id)
    ON UPDATE CASCADE ON DELETE RESTRICT,
  CONSTRAINT fk_student_major FOREIGN KEY (major_id) REFERENCES major(id)
    ON UPDATE CASCADE ON DELETE RESTRICT,
  KEY idx_student_stat (college_id, major_id, enrollment_year)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS user_account (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  username VARCHAR(64) NOT NULL UNIQUE,
  password VARCHAR(255) NOT NULL,
  role VARCHAR(32) NOT NULL
) ENGINE=InnoDB;

INSERT INTO college(name) VALUES ('计算机学院'), ('经济管理学院')
ON DUPLICATE KEY UPDATE name = VALUES(name);

INSERT INTO major(name, college_id)
SELECT '软件工程', c.id FROM college c WHERE c.name = '计算机学院'
UNION ALL
SELECT '信息管理', c.id FROM college c WHERE c.name = '经济管理学院'
ON DUPLICATE KEY UPDATE name = VALUES(name);
