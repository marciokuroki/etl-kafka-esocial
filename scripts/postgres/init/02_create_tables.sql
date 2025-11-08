-- 02_create_tables.sql
-- Tabelas principais do modelo eSocial

-- Tabela de colaboradores (modelo eSocial)
CREATE TABLE IF NOT EXISTS public.employees (
    id BIGSERIAL PRIMARY KEY,
    source_id VARCHAR(20) NOT NULL UNIQUE, -- ID do sistema origem (Oracle)
    cpf VARCHAR(11) NOT NULL UNIQUE,
    pis VARCHAR(11),
    full_name VARCHAR(200) NOT NULL,
    birth_date DATE,
    admission_date DATE NOT NULL,
    termination_date DATE,
    job_title VARCHAR(100),
    department VARCHAR(100),
    salary NUMERIC(10,2),
    status VARCHAR(20) DEFAULT 'ACTIVE',
    
    -- Metadados eSocial
    esocial_event_type VARCHAR(10), -- S-2190, S-2200, S-2300, etc
    esocial_sent_at TIMESTAMP,
    esocial_protocol VARCHAR(100),
    esocial_status VARCHAR(20) DEFAULT 'PENDING', -- PENDING, SENT, ACCEPTED, REJECTED
    
    -- Auditoria
    version INTEGER DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(50) DEFAULT 'system',
    updated_by VARCHAR(50) DEFAULT 'system',
    
    -- Kafka metadata
    kafka_offset BIGINT,
    kafka_partition INTEGER,
    kafka_topic VARCHAR(100),
    correlation_id UUID DEFAULT uuid_generate_v4()
);

-- Trigger para atualizar updated_at
CREATE TRIGGER employees_updated_at
    BEFORE UPDATE ON public.employees
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- Tabela de histórico (versionamento)
CREATE TABLE IF NOT EXISTS audit.employees_history (
    history_id BIGSERIAL PRIMARY KEY,
    employee_id BIGINT NOT NULL,
    source_id VARCHAR(20) NOT NULL,
    cpf VARCHAR(11) NOT NULL,
    pis VARCHAR(11),
    full_name VARCHAR(200),
    birth_date DATE,
    admission_date DATE,
    termination_date DATE,
    job_title VARCHAR(100),
    department VARCHAR(100),
    salary NUMERIC(10,2),
    status VARCHAR(20),
    
    -- Metadados de versionamento
    version INTEGER NOT NULL,
    operation VARCHAR(10) NOT NULL, -- INSERT, UPDATE, DELETE
    changed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    changed_by VARCHAR(50),
    
    -- Kafka metadata
    kafka_offset BIGINT,
    correlation_id UUID
);

CREATE INDEX idx_employees_history_employee_id ON audit.employees_history(employee_id);
CREATE INDEX idx_employees_history_source_id ON audit.employees_history(source_id);
CREATE INDEX idx_employees_history_changed_at ON audit.employees_history(changed_at);

-- Tabela de erros de validação
CREATE TABLE IF NOT EXISTS public.validation_errors (
    id BIGSERIAL PRIMARY KEY,
    event_id VARCHAR(50),
    source_table VARCHAR(50),
    source_id VARCHAR(50),
    validation_rule VARCHAR(100) NOT NULL,
    error_message TEXT NOT NULL,
    severity VARCHAR(20) NOT NULL, -- ERROR, WARNING, INFO
    field_name VARCHAR(100),
    field_value TEXT,
    event_payload JSONB,
    
    -- Metadados
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    resolved_at TIMESTAMP,
    resolved_by VARCHAR(50),
    
    -- Kafka metadata
    kafka_offset BIGINT,
    kafka_partition INTEGER,
    kafka_topic VARCHAR(100),
    correlation_id UUID
);

CREATE INDEX idx_validation_errors_event_id ON public.validation_errors(event_id);
CREATE INDEX idx_validation_errors_rule ON public.validation_errors(validation_rule);
CREATE INDEX idx_validation_errors_severity ON public.validation_errors(severity);
CREATE INDEX idx_validation_errors_created_at ON public.validation_errors(created_at);

-- Tabela de Dead Letter Queue
CREATE TABLE IF NOT EXISTS public.dlq_events (
    id BIGSERIAL PRIMARY KEY,
    event_id VARCHAR(50) NOT NULL,
    event_type VARCHAR(50) NOT NULL,
    source_table VARCHAR(50),
    source_id VARCHAR(50),
    event_payload JSONB NOT NULL,
    error_message TEXT,
    stack_trace TEXT,
    retry_count INTEGER DEFAULT 0,
    max_retries INTEGER DEFAULT 3,
    status VARCHAR(20) DEFAULT 'PENDING', -- PENDING, REPROCESSING, RESOLVED, DISCARDED
    
    -- Metadados
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_retry_at TIMESTAMP,
    resolved_at TIMESTAMP,
    resolved_by VARCHAR(50),
    
    -- Kafka metadata
    kafka_offset BIGINT,
    kafka_partition INTEGER,
    kafka_topic VARCHAR(100),
    correlation_id UUID
);

CREATE INDEX idx_dlq_events_event_id ON public.dlq_events(event_id);
CREATE INDEX idx_dlq_events_status ON public.dlq_events(status);
CREATE INDEX idx_dlq_events_created_at ON public.dlq_events(created_at);

-- Tabela de métricas de processamento
CREATE TABLE IF NOT EXISTS public.processing_metrics (
    id BIGSERIAL PRIMARY KEY,
    event_type VARCHAR(50) NOT NULL,
    processing_start TIMESTAMP NOT NULL,
    processing_end TIMESTAMP NOT NULL,
    duration_ms BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL, -- SUCCESS, ERROR
    error_message TEXT,
    
    -- Metadados
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    correlation_id UUID
);

CREATE INDEX idx_processing_metrics_event_type ON public.processing_metrics(event_type);
CREATE INDEX idx_processing_metrics_created_at ON public.processing_metrics(created_at);
CREATE INDEX idx_processing_metrics_status ON public.processing_metrics(status);

-- Comentários nas tabelas
COMMENT ON TABLE public.employees IS 'Colaboradores no modelo eSocial';
COMMENT ON TABLE audit.employees_history IS 'Histórico completo de todas as mudanças em employees';
COMMENT ON TABLE public.validation_errors IS 'Erros de validação identificados no processamento';
COMMENT ON TABLE public.dlq_events IS 'Dead Letter Queue - eventos que falharam no processamento';
COMMENT ON TABLE public.processing_metrics IS 'Métricas de performance do processamento';
