-- Schema para testes
CREATE SCHEMA IF NOT EXISTS source;

-- Tabela employees no schema source
CREATE TABLE IF NOT EXISTS source.employees (
    employee_id VARCHAR(20) PRIMARY KEY,
    cpf VARCHAR(11) NOT NULL,
    pis VARCHAR(11),
    full_name VARCHAR(200) NOT NULL,
    birth_date DATE,
    admission_date DATE NOT NULL,
    termination_date DATE,
    job_title VARCHAR(100),
    department VARCHAR(100),
    salary NUMERIC(10,2),
    status VARCHAR(20) DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_employees_updated_at ON source.employees(updated_at);
CREATE INDEX idx_employees_created_at ON source.employees(created_at);
