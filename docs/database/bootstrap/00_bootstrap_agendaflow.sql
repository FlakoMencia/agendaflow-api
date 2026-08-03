-- =====================================================================
-- 00_bootstrap_agendaflow.sql
-- Aprovisionamiento inicial de AgendaFlow en PostgreSQL
--
-- EJECUTAR UNA SOLA VEZ conectado a la base administrativa "postgres"
-- con un usuario administrador, por ejemplo: postgres_admin
--
-- NO colocar este archivo dentro de db/migration.
-- Flyway se conecta después a agendaflow_db y ejecuta V1__initial_schema.sql.
--
-- IMPORTANTE:
-- CREATE DATABASE no puede ejecutarse dentro de BEGIN/COMMIT.
-- Este script no es repetible: si el rol o la base ya existen, PostgreSQL
-- informará que ya existen. En ese caso, no vuelvas a ejecutar esa sentencia.
-- =====================================================================

-- 1. Usuario propietario de la aplicación
CREATE ROLE agendaflow_user
WITH
    LOGIN
    PASSWORD 'agendaflow_local_password'
    NOSUPERUSER
    NOCREATEDB
    NOCREATEROLE
    NOINHERIT;

-- 2. Base de datos independiente para AgendaFlow
CREATE DATABASE agendaflow_db
WITH
    OWNER = agendaflow_user
    ENCODING = 'UTF8'
    TEMPLATE = template0;

-- 3. Restringir y conceder acceso explícito
REVOKE ALL ON DATABASE agendaflow_db FROM PUBLIC;
GRANT CONNECT, TEMPORARY ON DATABASE agendaflow_db TO agendaflow_user;

-- 4. Configuración predeterminada del usuario dentro de esta base
ALTER ROLE agendaflow_user
IN DATABASE agendaflow_db
SET search_path TO agendaflow, public;

-- Después de ejecutar este archivo:
-- URL: jdbc:postgresql://localhost:5432/agendaflow_db
-- User: agendaflow_user
-- Password: agendaflow_local_password
