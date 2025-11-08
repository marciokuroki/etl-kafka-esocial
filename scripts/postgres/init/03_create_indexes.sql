-- 03_create_indexes.sql
-- Índices otimizados para queries

-- Índices na tabela employees
CREATE INDEX IF NOT EXISTS idx_employees_cpf ON public.employees(cpf);
CREATE INDEX IF NOT EXISTS idx_employees_pis ON public.employees(pis);
CREATE INDEX IF NOT EXISTS idx_employees_status ON public.employees(status);
CREATE INDEX IF NOT EXISTS idx_employees_admission_date ON public.employees(admission_date);
CREATE INDEX IF NOT EXISTS idx_employees_esocial_status ON public.employees(esocial_status);
CREATE INDEX IF NOT EXISTS idx_employees_correlation_id ON public.employees(correlation_id);
CREATE INDEX IF NOT EXISTS idx_employees_updated_at ON public.employees(updated_at);

-- Índice para busca full-text no nome
CREATE INDEX IF NOT EXISTS idx_employees_full_name_gin 
ON public.employees USING gin(to_tsvector('portuguese', full_name));

-- Índices compostos
CREATE INDEX IF NOT EXISTS idx_employees_status_admission 
ON public.employees(status, admission_date);

-- Estatísticas
ANALYZE public.employees;
ANALYZE audit.employees_history;
ANALYZE public.validation_errors;
ANALYZE public.dlq_events;
ANALYZE public.processing_metrics;

-- Mensagem de sucesso
DO $$
BEGIN
    RAISE NOTICE 'Índices criados com sucesso!';
    RAISE NOTICE 'Total de tabelas: %', (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'public');
    RAISE NOTICE 'Total de índices: %', (SELECT COUNT(*) FROM pg_indexes WHERE schemaname = 'public');
END $$;
