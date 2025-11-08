-- 01_create_hr_schema.sql
-- Criação do schema de RH no Oracle XE (origem)

-- Habilitar Supplemental Logging no CDB$ROOT primeiro
ALTER SESSION SET CONTAINER = CDB$ROOT;
ALTER DATABASE ADD SUPPLEMENTAL LOG DATA;
ALTER DATABASE ADD SUPPLEMENTAL LOG DATA (PRIMARY KEY) COLUMNS;

-- Conectar ao PDB e configurar sessão
ALTER SESSION SET CONTAINER = XEPDB1;
ALTER SESSION SET "_ORACLE_SCRIPT" = TRUE;

-- Criar usuário de aplicação
CREATE USER hr_app IDENTIFIED BY HrAppPassword123
DEFAULT TABLESPACE USERS
TEMPORARY TABLESPACE TEMP
QUOTA UNLIMITED ON USERS;

-- Conceder privilégios básicos
GRANT CONNECT, RESOURCE TO hr_app;
GRANT CREATE SESSION TO hr_app;
GRANT CREATE TABLE TO hr_app;
GRANT CREATE VIEW TO hr_app;
GRANT CREATE SEQUENCE TO hr_app;
GRANT UNLIMITED TABLESPACE TO hr_app;

-- Criar tabela de colaboradores no schema hr_app
CREATE TABLE hr_app.employees (
    employee_id VARCHAR2(20) PRIMARY KEY,
    cpf VARCHAR2(11) NOT NULL UNIQUE,
    pis VARCHAR2(11),
    full_name VARCHAR2(200) NOT NULL,
    birth_date DATE,
    admission_date DATE NOT NULL,
    termination_date DATE,
    job_title VARCHAR2(100),
    department VARCHAR2(100),
    salary NUMBER(10,2),
    status VARCHAR2(20) DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR2(50) DEFAULT USER,
    updated_by VARCHAR2(50) DEFAULT USER
);

-- Criar índices (apenas os que não são automáticos)
-- CPF já tem índice (UNIQUE constraint)
-- employee_id já tem índice (PRIMARY KEY constraint)
CREATE INDEX hr_app.idx_employees_pis ON hr_app.employees(pis);
CREATE INDEX hr_app.idx_employees_status ON hr_app.employees(status);
CREATE INDEX hr_app.idx_employees_admission ON hr_app.employees(admission_date);
CREATE INDEX hr_app.idx_employees_department ON hr_app.employees(department);

-- Criar trigger para atualizar updated_at
CREATE OR REPLACE TRIGGER hr_app.employees_updated_at
BEFORE UPDATE ON hr_app.employees
FOR EACH ROW
BEGIN
    :NEW.updated_at := CURRENT_TIMESTAMP;
    :NEW.updated_by := USER;
END;
/

-- Criar sequência para employee_id
CREATE SEQUENCE hr_app.seq_employee_id START WITH 1 INCREMENT BY 1;

-- Criar tabela de histórico de folha de pagamento
CREATE TABLE hr_app.payroll (
    payroll_id NUMBER PRIMARY KEY,
    employee_id VARCHAR2(20) NOT NULL,
    reference_month DATE NOT NULL,
    gross_salary NUMBER(10,2),
    net_salary NUMBER(10,2),
    inss_amount NUMBER(10,2),
    irrf_amount NUMBER(10,2),
    fgts_amount NUMBER(10,2),
    processed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_payroll_employee FOREIGN KEY (employee_id) 
        REFERENCES hr_app.employees(employee_id)
);

-- Criar sequência para payroll_id
CREATE SEQUENCE hr_app.seq_payroll_id START WITH 1 INCREMENT BY 1;

-- Habilitar Supplemental Logging nas tabelas (para CDC)
-- Agora funcionará porque já habilitamos no CDB$ROOT
ALTER TABLE hr_app.employees ADD SUPPLEMENTAL LOG DATA (ALL) COLUMNS;
ALTER TABLE hr_app.payroll ADD SUPPLEMENTAL LOG DATA (ALL) COLUMNS;

COMMIT;

-- Mensagem de sucesso
SELECT 'Schema HR criado com sucesso!' AS status FROM DUAL;
