# Migraciones de base de datos

Flyway es la única herramienta autorizada para versionar y aplicar cambios al esquema PostgreSQL
de AgendaFlow. Las migraciones se encuentran en `src/main/resources/db/migration` y se ejecutan en
orden de versión.

## Reglas de evolución

- Una migración aplicada es inmutable: no se edita, renombra ni reordena.
- Los cambios futuros deben agregarse en un archivo nuevo con la convención
  `V<versión>__<descripción>.sql`, usando una versión mayor y una descripción legible con guiones
  bajos.
- No se debe utilizar `flyway clean` en bases compartidas, locales con información útil ni
  ambientes desplegados. Tampoco deben usarse `repair` o `baseline` para ocultar errores.
- Las claves primarias y foráneas utilizan PostgreSQL `BIGINT`; las futuras entidades Java las
  representarán mediante `Long`.

## Validación con Testcontainers

La clase `FlywayMigrationIT` levanta exclusivamente `postgres:18.4`, inyecta dinámicamente URL,
usuario y contraseña, inicia Spring con el perfil `integration-test` y permite que Flyway construya
el esquema desde cero. Después comprueba esquemas, tablas, historial, tipos `BIGINT` y ausencia de
migraciones fallidas o baseline. Failsafe deshabilita el contenedor auxiliar Ryuk para que esta
validación ejecute únicamente la imagen PostgreSQL permitida; JUnit detiene el contenedor al cerrar
la clase de prueba.

```cmd
mvnw.cmd clean test
mvnw.cmd clean verify
```

`clean test` ejecuta las pruebas rápidas sin Docker. `clean verify` añade la prueba de integración
mediante Maven Failsafe y requiere que Docker esté activo. Para ejecutar solo la integración:

```cmd
mvnw.cmd -Dit.test=FlywayMigrationIT verify
```

La base creada por Testcontainers es desechable, usa un puerto aleatorio y se elimina al terminar.
No comparte URL ni credenciales con la instancia PostgreSQL local del desarrollador y nunca debe
sustituirse por ella dentro de esta prueba.
