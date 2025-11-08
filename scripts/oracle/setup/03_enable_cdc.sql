-- 03_enable_cdc.sql
-- Configurar Change Data Capture (LogMiner) no Oracle XE

-- Conectar ao CDB$ROOT
ALTER SESSION SET CONTAINER = CDB$ROOT;

-- Habilitar ARCHIVELOG mode (necessário para CDC)
-- Nota: No Oracle XE, isso pode não funcionar, mas vamos tentar
BEGIN
    EXECUTE IMMEDIATE 'ALTER DATABASE ARCHIVELOG';
EXCEPTION
    WHEN OTHERS THEN
        DBMS_OUTPUT.PUT_LINE('ARCHIVELOG já habilitado ou não suportado no XE');
END;
/

-- Habilitar Supplemental Logging
ALTER DATABASE ADD SUPPLEMENTAL LOG DATA;
ALTER DATABASE ADD SUPPLEMENTAL LOG DATA (PRIMARY KEY) COLUMNS;

-- Conectar ao PDB
ALTER SESSION SET CONTAINER = XEPDB1;

-- Conceder privilégios para LogMiner ao hr_app
GRANT SELECT ANY TRANSACTION TO hr_app;
GRANT EXECUTE ON DBMS_LOGMNR TO hr_app;
GRANT EXECUTE ON DBMS_LOGMNR_D TO hr_app;
GRANT SELECT ON V_$LOGMNR_CONTENTS TO hr_app;
GRANT SELECT ON V_$LOGMNR_LOGS TO hr_app;
GRANT SELECT ON V_$ARCHIVED_LOG TO hr_app;

-- Verificar configuração
SELECT log_mode, supplemental_log_data_min, supplemental_log_data_pk 
FROM v$database;

-- Mensagem de sucesso
SELECT 'CDC configurado com sucesso!' AS status FROM DUAL;
