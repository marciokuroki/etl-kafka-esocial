-- 02_create_tables.sql
-- Tabelas principais do modelo eSocial

-- Tabela de colaboradores (modelo eSocial)
CREATE TABLE IF NOT EXISTS public.employees (
    id BIGSERIAL PRIMARY KEY,
    source_id VARCHAR(20) NOT NULL UNIQUE, -- ID do sistema origem (Oracle)
    cpf VARCHAR(11) NOT NULL UNIQUE,
    pis VARCHAR(11),
    ctps VARCHAR(20) UNIQUE,
    matricula VARCHAR(20) NOT NULL,
    full_name VARCHAR(200) NOT NULL,
    birth_date DATE,
    sex VARCHAR(1),
    nationality VARCHAR(1),
    marital_status VARCHAR(1),
    race VARCHAR(2),
    education_level VARCHAR(2),
    disability VARCHAR(2),
    email VARCHAR(150),
    phone VARCHAR(15),
    zip_code VARCHAR(8),
    uf VARCHAR(2),
    admission_date DATE NOT NULL,
    termination_date DATE,
    job_title VARCHAR(100),
    department VARCHAR(100),
    category VARCHAR(3),
    contract_type VARCHAR(3),
    cbo VARCHAR(6),
    salary NUMERIC(12, 2),
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

-- Índices para Performance
CREATE INDEX idx_employees_cpf ON employees(cpf);
CREATE INDEX idx_employees_pis ON employees(pis);
CREATE INDEX idx_employees_source_id ON employees(source_id);
CREATE INDEX idx_employees_status ON employees(status);
CREATE INDEX idx_employees_esocial_status ON employees(esocial_status);
CREATE INDEX idx_employees_ctps ON employees(ctps);
CREATE INDEX idx_employees_matricula ON employees(matricula);
CREATE INDEX idx_employees_created_at ON employees(created_at);

-- Trigger para atualizar updated_at
CREATE TRIGGER employees_updated_at
    BEFORE UPDATE ON public.employees
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- Criar tabela employee_events
CREATE TABLE IF NOT EXISTS public.employee_events (
    id BIGSERIAL PRIMARY KEY,
    
    -- --- Identificação do Evento
    source_id VARCHAR(20) NOT NULL UNIQUE,
    event_id VARCHAR(50) NOT NULL UNIQUE,
    correlation_id UUID NOT NULL,
    
    -- --- Tipo e Status do Evento
    event_type VARCHAR(10) NOT NULL,
    status VARCHAR(20) NOT NULL,
    
    -- --- Mensagens de Erro
    error_message TEXT,
    error_details TEXT,
    
    -- --- Dados Kafka
    kafka_topic VARCHAR(100) NOT NULL,
    kafka_partition INTEGER NOT NULL,
    kafka_offset BIGINT NOT NULL,
    event_payload TEXT,
    
    -- --- Validação
    validation_status VARCHAR(20),
    validation_errors TEXT,
    validation_warnings TEXT,
    validation_executed_at TIMESTAMP,
    
    -- --- Processamento
    processing_status VARCHAR(20),
    processing_started_at TIMESTAMP,
    processing_finished_at TIMESTAMP,
    processing_duration_ms BIGINT,
    
    -- --- eSocial Integration
    esocial_sent BOOLEAN DEFAULT FALSE,
    esocial_sent_at TIMESTAMP,
    esocial_protocol VARCHAR(100),
    esocial_status VARCHAR(20),
    esocial_response TEXT,
    
    -- --- Retry
    retry_count INTEGER DEFAULT 0,
    last_retry_at TIMESTAMP,
    next_retry_at TIMESTAMP,
    
    -- --- Auditoria
    created_by VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_by VARCHAR(50),
    updated_at TIMESTAMP,
    version INTEGER,
    
    -- --- Relacionamento com Employee
    employee_id BIGINT,
    
    -- --- Constraints
    CONSTRAINT fk_employee_events_employee 
        FOREIGN KEY (employee_id) 
        REFERENCES public.employees(id) ON DELETE SET NULL
);

-- ============================================================================
-- ÍNDICES PARA PERFORMANCE
-- ============================================================================

-- Índices de Identificação
CREATE UNIQUE INDEX IF NOT EXISTS idx_employee_events_source_id 
    ON public.employee_events(source_id);

CREATE UNIQUE INDEX IF NOT EXISTS idx_employee_events_event_id 
    ON public.employee_events(event_id);

CREATE INDEX IF NOT EXISTS idx_employee_events_correlation_id 
    ON public.employee_events(correlation_id);

-- Índices de Tipo e Status
CREATE INDEX IF NOT EXISTS idx_employee_events_event_type 
    ON public.employee_events(event_type);

CREATE INDEX IF NOT EXISTS idx_employee_events_status 
    ON public.employee_events(status);

-- Índices de Kafka
CREATE INDEX IF NOT EXISTS idx_employee_events_kafka_offset 
    ON public.employee_events(kafka_offset);

CREATE INDEX IF NOT EXISTS idx_employee_events_kafka_topic 
    ON public.employee_events(kafka_topic);

CREATE INDEX IF NOT EXISTS idx_employee_events_kafka_partition 
    ON public.employee_events(kafka_partition);

-- Índices de Validação
CREATE INDEX IF NOT EXISTS idx_employee_events_validation_status 
    ON public.employee_events(validation_status);

CREATE INDEX IF NOT EXISTS idx_employee_events_validation_executed_at 
    ON public.employee_events(validation_executed_at);

-- Índices de Processamento
CREATE INDEX IF NOT EXISTS idx_employee_events_processing_status 
    ON public.employee_events(processing_status);

CREATE INDEX IF NOT EXISTS idx_employee_events_processing_started_at 
    ON public.employee_events(processing_started_at);

-- Índices de eSocial
CREATE INDEX IF NOT EXISTS idx_employee_events_esocial_status 
    ON public.employee_events(esocial_status);

CREATE INDEX IF NOT EXISTS idx_employee_events_esocial_sent 
    ON public.employee_events(esocial_sent);

-- Índices de Auditoria
CREATE INDEX IF NOT EXISTS idx_employee_events_created_at 
    ON public.employee_events(created_at);

CREATE INDEX IF NOT EXISTS idx_employee_events_created_by 
    ON public.employee_events(created_by);

CREATE INDEX IF NOT EXISTS idx_employee_events_updated_at 
    ON public.employee_events(updated_at);

-- Índice para Retry
CREATE INDEX IF NOT EXISTS idx_employee_events_next_retry_at 
    ON public.employee_events(next_retry_at);

CREATE INDEX IF NOT EXISTS idx_employee_events_retry_count 
    ON public.employee_events(retry_count);

-- Índice para Relacionamento
CREATE INDEX IF NOT EXISTS idx_employee_events_employee_id 
    ON public.employee_events(employee_id);

-- ============================================================================
-- ÍNDICES COMPOSTOS (PARA QUERIES FREQUENTES)
-- ============================================================================

-- Buscar eventos por tipo e status
CREATE INDEX IF NOT EXISTS idx_employee_events_type_status 
    ON public.employee_events(event_type, status);

-- Buscar eventos de retry por status
CREATE INDEX IF NOT EXISTS idx_employee_events_retry_status 
    ON public.employee_events(retry_count, status, next_retry_at);

-- Buscar eventos por tipo e data de criação
CREATE INDEX IF NOT EXISTS idx_employee_events_type_created 
    ON public.employee_events(event_type, created_at DESC);

-- Buscar eventos de eSocial por status e data
CREATE INDEX IF NOT EXISTS idx_employee_events_esocial_date 
    ON public.employee_events(esocial_status, esocial_sent_at DESC);

-- Buscar eventos não processados
CREATE INDEX IF NOT EXISTS idx_employee_events_unprocessed 
    ON public.employee_events(status, created_at ASC) 
    WHERE status NOT IN ('ARCHIVED', 'ESOCIAL_PROCESSED');

-- ============================================================================
-- COMENTÁRIOS DESCRITIVOS
-- ============================================================================

COMMENT ON TABLE public.employee_events IS 
'Tabela de rastreamento de eventos de colaboradores. Armazena todo o ciclo de vida de um evento desde o recebimento até a conclusão no eSocial.';

COMMENT ON COLUMN public.employee_events.source_id IS 
'Identificador único do sistema origem. Chave de negócio.';

COMMENT ON COLUMN public.employee_events.event_id IS 
'ID do evento no Kafka para rastreabilidade.';

COMMENT ON COLUMN public.employee_events.correlation_id IS 
'UUID para correlacionar eventos entre sistemas.';

COMMENT ON COLUMN public.employee_events.event_type IS 
'Tipo do evento eSocial: S-2200, S-2300, S-2400, etc.';

COMMENT ON COLUMN public.employee_events.status IS 
'Estado atual do evento: RECEIVED, VALIDATING, VALIDATION_FAILED, PROCESSING, SUCCESS, ERROR, etc.';

COMMENT ON COLUMN public.employee_events.validation_status IS 
'Status da validação: PENDING, PASSED, FAILED.';

COMMENT ON COLUMN public.employee_events.processing_status IS 
'Status do processamento: PENDING, PROCESSING, COMPLETED, FAILED.';

COMMENT ON COLUMN public.employee_events.esocial_status IS 
'Status no eSocial: PENDING, SENT, RECEIVED, PROCESSED, REJECTED.';

COMMENT ON COLUMN public.employee_events.retry_count IS 
'Quantidade de tentativas de reprocessamento.';

COMMENT ON COLUMN public.employee_events.created_at IS 
'Data e hora de criação do registro (quando o evento foi recebido).';

COMMENT ON COLUMN public.employee_events.updated_at IS 
'Data e hora da última atualização.';

COMMENT ON COLUMN public.employee_events.version IS 
'Versão do registro para controle de concorrência otimista.';

-- ============================================================================
-- GRANTS (Ajuste conforme suas políticas de segurança)
-- ============================================================================

-- Garantir permissões ao usuário da aplicação
-- GRANT SELECT, INSERT, UPDATE ON public.employee_events TO app_user;
-- GRANT USAGE, SELECT ON SEQUENCE public.employee_events_id_seq TO app_user;


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
