-- 02_sample_data.sql
-- Inserir dados de exemplo para testes

ALTER SESSION SET CONTAINER = XEPDB1;
ALTER SESSION SET CURRENT_SCHEMA = hr_app;

-- Inserir 10 colaboradores de exemplo
INSERT INTO employees (employee_id, cpf, pis, full_name, birth_date, admission_date, job_title, department, salary, status)
VALUES ('EMP001', '12345678901', '10011223344', 'João da Silva Santos', TO_DATE('1985-03-15', 'YYYY-MM-DD'), TO_DATE('2020-01-10', 'YYYY-MM-DD'), 'Analista de Sistemas', 'TI', 5500.00, 'ACTIVE');

INSERT INTO employees (employee_id, cpf, pis, full_name, birth_date, admission_date, job_title, department, salary, status)
VALUES ('EMP002', '23456789012', '10022334455', 'Maria Oliveira Costa', TO_DATE('1990-07-22', 'YYYY-MM-DD'), TO_DATE('2019-05-15', 'YYYY-MM-DD'), 'Gerente de Projetos', 'TI', 8200.00, 'ACTIVE');

INSERT INTO employees (employee_id, cpf, pis, full_name, birth_date, admission_date, job_title, department, salary, status)
VALUES ('EMP003', '34567890123', '10033445566', 'Pedro Henrique Lima', TO_DATE('1988-11-08', 'YYYY-MM-DD'), TO_DATE('2018-03-20', 'YYYY-MM-DD'), 'Desenvolvedor Senior', 'TI', 7500.00, 'ACTIVE');

INSERT INTO employees (employee_id, cpf, pis, full_name, birth_date, admission_date, job_title, department, salary, status)
VALUES ('EMP004', '45678901234', '10044556677', 'Ana Paula Ferreira', TO_DATE('1992-02-14', 'YYYY-MM-DD'), TO_DATE('2021-06-01', 'YYYY-MM-DD'), 'Analista de RH', 'Recursos Humanos', 4800.00, 'ACTIVE');

INSERT INTO employees (employee_id, cpf, pis, full_name, birth_date, admission_date, job_title, department, salary, status)
VALUES ('EMP005', '56789012345', '10055667788', 'Carlos Eduardo Souza', TO_DATE('1987-09-30', 'YYYY-MM-DD'), TO_DATE('2017-11-10', 'YYYY-MM-DD'), 'Coordenador Financeiro', 'Financeiro', 9200.00, 'ACTIVE');

INSERT INTO employees (employee_id, cpf, pis, full_name, birth_date, admission_date, job_title, department, salary, status)
VALUES ('EMP006', '67890123456', '10066778899', 'Juliana Martins Alves', TO_DATE('1995-04-18', 'YYYY-MM-DD'), TO_DATE('2022-02-01', 'YYYY-MM-DD'), 'Assistente Administrativo', 'Administrativo', 3200.00, 'ACTIVE');

INSERT INTO employees (employee_id, cpf, pis, full_name, birth_date, admission_date, job_title, department, salary, status)
VALUES ('EMP007', '78901234567', '10077889900', 'Ricardo Mendes Rocha', TO_DATE('1983-12-25', 'YYYY-MM-DD'), TO_DATE('2016-08-15', 'YYYY-MM-DD'), 'Diretor de Operações', 'Diretoria', 15000.00, 'ACTIVE');

INSERT INTO employees (employee_id, cpf, pis, full_name, birth_date, admission_date, job_title, department, salary, status)
VALUES ('EMP008', '89012345678', '10088990011', 'Fernanda Silva Gomes', TO_DATE('1991-06-12', 'YYYY-MM-DD'), TO_DATE('2020-09-20', 'YYYY-MM-DD'), 'Analista Contábil', 'Financeiro', 5000.00, 'ACTIVE');

INSERT INTO employees (employee_id, cpf, pis, full_name, birth_date, admission_date, job_title, department, salary, status)
VALUES ('EMP009', '90123456789', '10099001122', 'Bruno Cesar Oliveira', TO_DATE('1989-01-07', 'YYYY-MM-DD'), TO_DATE('2019-12-01', 'YYYY-MM-DD'), 'Desenvolvedor Pleno', 'TI', 6500.00, 'ACTIVE');

INSERT INTO employees (employee_id, cpf, pis, full_name, birth_date, admission_date, job_title, department, salary, status)
VALUES ('EMP010', '01234567890', '10000112233', 'Patrícia Cardoso Dias', TO_DATE('1994-08-29', 'YYYY-MM-DD'), TO_DATE('2021-04-15', 'YYYY-MM-DD'), 'Designer UX/UI', 'TI', 5800.00, 'ACTIVE');

-- Inserir folhas de pagamento de exemplo
INSERT INTO payroll (payroll_id, employee_id, reference_month, gross_salary, net_salary, inss_amount, irrf_amount, fgts_amount)
VALUES (seq_payroll_id.NEXTVAL, 'EMP001', TO_DATE('2024-10-01', 'YYYY-MM-DD'), 5500.00, 4620.00, 550.00, 220.00, 440.00);

INSERT INTO payroll (payroll_id, employee_id, reference_month, gross_salary, net_salary, inss_amount, irrf_amount, fgts_amount)
VALUES (seq_payroll_id.NEXTVAL, 'EMP002', TO_DATE('2024-10-01', 'YYYY-MM-DD'), 8200.00, 6890.00, 820.00, 328.00, 656.00);

INSERT INTO payroll (payroll_id, employee_id, reference_month, gross_salary, net_salary, inss_amount, irrf_amount, fgts_amount)
VALUES (seq_payroll_id.NEXTVAL, 'EMP003', TO_DATE('2024-10-01', 'YYYY-MM-DD'), 7500.00, 6300.00, 750.00, 300.00, 600.00);

COMMIT;

-- Estatísticas
SELECT 
    'Dados de exemplo inseridos!' AS status,
    (SELECT COUNT(*) FROM employees) AS total_employees,
    (SELECT COUNT(*) FROM payroll) AS total_payroll_records
FROM DUAL;
