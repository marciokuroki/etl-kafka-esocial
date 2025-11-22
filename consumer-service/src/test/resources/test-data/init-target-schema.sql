-- Schema de destino para testes
CREATE TABLE public.employees (
    id BIGSERIAL PRIMARY KEY,
    source_id VARCHAR(20) UNIQUE NOT NULL,
    cpf VARCHAR(11) UNIQUE NOT NULL,
    pis VARCHAR(11),
    full_name VARCHAR(200) NOT NULL,
    birth_date DATE,
    admission_date DATE NOT NULL,
    termination_date DATE,
    job_title VARCHAR(100),
    department VARCHAR(100),
    salary NUMERIC(10,2),
    status VARCHAR(20),
    esocial_status VARCHAR(20) DEFAULT 'PENDING',
    version INTEGER DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE SCHEMA audit;

CREATE TABLE audit.employees_history (
    history_id BIGSERIAL PRIMARY KEY,
    employee_id BIGINT,
    source_id VARCHAR(20),
    operation VARCHAR(10),
    version INTEGER,
    changed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    cpf VARCHAR(11),
    full_name VARCHAR(200),
    salary NUMERIC(10,2)
);

CREATE TABLE public.validation_errors (
    id BIGSERIAL PRIMARY KEY,
    event_id VARCHAR(50),
    source_id VARCHAR(50),
    validation_rule VARCHAR(100),
    error_message TEXT,
    severity VARCHAR(20),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE public.dlq_events (
    id BIGSERIAL PRIMARY KEY,
    event_id VARCHAR(50),
    event_type VARCHAR(50),
    event_payload JSONB,
    error_message TEXT,
    retry_count INTEGER DEFAULT 0,
    status VARCHAR(20) DEFAULT 'PENDING',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
