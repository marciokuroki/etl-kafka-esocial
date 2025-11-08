-- 01_create_schema.sql
-- Criação do schema no PostgreSQL (destino - modelo eSocial)

-- Criar extensões
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- Criar schemas
CREATE SCHEMA IF NOT EXISTS public;
CREATE SCHEMA IF NOT EXISTS audit;
CREATE SCHEMA IF NOT EXISTS staging;

-- Comentários
COMMENT ON SCHEMA public IS 'Schema principal com dados do eSocial';
COMMENT ON SCHEMA audit IS 'Schema de auditoria e histórico';
COMMENT ON SCHEMA staging IS 'Schema temporário para processamento';

-- Criar usuário de aplicação (Consumer)
CREATE USER esocial_app WITH PASSWORD 'EsocialAppPassword123!';

-- Conceder permissões
GRANT CONNECT ON DATABASE esocial TO esocial_app;
GRANT USAGE ON SCHEMA public, audit, staging TO esocial_app;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO esocial_app;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO esocial_app;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA audit TO esocial_app;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA audit TO esocial_app;

-- Garantir privilégios em tabelas futuras
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO esocial_app;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO esocial_app;
ALTER DEFAULT PRIVILEGES IN SCHEMA audit GRANT ALL ON TABLES TO esocial_app;

-- Criar função para trigger de updated_at
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;
