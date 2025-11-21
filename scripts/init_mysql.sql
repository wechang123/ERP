-- ERP Employee Service Database Initialization
-- 데이터베이스 생성
CREATE DATABASE IF NOT EXISTS erp_db;
USE erp_db;

-- 직원 테이블 생성
CREATE TABLE IF NOT EXISTS employees (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    department VARCHAR(100) NOT NULL,
    position VARCHAR(100) NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- 샘플 데이터 삽입 (테스트용)
INSERT INTO employees (name, department, position) VALUES
('Kim', 'HR', 'Manager'),
('Lee', 'Finance', 'Director'),
('Park', 'IT', 'Developer'),
('Choi', 'HR', 'Staff'),
('Jung', 'Finance', 'Manager'),
('Kang', 'IT', 'Senior Developer'),
('Yoon', 'Sales', 'Manager');
