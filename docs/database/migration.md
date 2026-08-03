# Inicialización de AgendaFlow

## 1. Bootstrap del servidor

Conéctate desde DataGrip a:

- Base de datos: `postgres`
- Usuario: `postgres_admin`
- URL: `jdbc:postgresql://localhost:5432/postgres`

Ejecuta una sola vez:

`00_bootstrap_agendaflow.sql`

Este archivo crea:

- `agendaflow_user`
- `agendaflow_db`
- permisos básicos de conexión
- `search_path` predeterminado

## 2. Migración Flyway V1

Coloca este archivo en el backend Spring Boot:

`src/main/resources/db/migration/V1__initial_schema.sql`

Flyway se conectará a `agendaflow_db` con `agendaflow_user` y creará:

- esquema `agendaflow`
- esquema `audit`
- esquema `batch`
- tablas
- índices
- restricciones
- función y triggers de `updated_at`
- roles y permisos iniciales

## 3. Configuración Spring Boot

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/agendaflow_db
spring.datasource.username=agendaflow_user
spring.datasource.password=${AGENDAFLOW_DB_PASSWORD}

spring.jpa.hibernate.ddl-auto=validate
spring.jpa.properties.hibernate.default_schema=agendaflow

spring.flyway.enabled=true
spring.flyway.locations=classpath:db/migration
spring.flyway.default-schema=agendaflow
spring.flyway.schemas=agendaflow,audit,batch
spring.flyway.create-schemas=false
```

Variable local:

```text
AGENDAFLOW_DB_PASSWORD=agendaflow_local_password
```

No subas contraseñas reales al repositorio.
