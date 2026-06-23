# Master Migration Plan

**Solution:** pom.xml (Maven single-module project)
**Solution Root:** `/home/ddicgegd/Projects/erp_springboot-experiment`
**Project Name:** ERP_SpringBoot-Experiment (com.anno)
**Created:** 2026-06-09T16:30:00+07:00
**Last Updated:** 2026-06-09T16:30:00+07:00

## Pre-Migration Fixes (Already Applied)

| # | Fix | Why | Files Changed |
|---|---|---|---|
| 1 | Oracle image: `11.2.0.2-slim` → `21-slim-faststart` | Hibernate 6+ (Spring Boot 3.5) **drops support for Oracle 11g**. Error: `HHH000511: The 11.2.0 version for [OracleDialect] is no longer supported`. Minimum supported version is 19c. | `src/docker/compose/oracle-db.yml` |
| 2 | Connection SID: `XE` → `XEPDB1` | Oracle 21c uses a pluggable database (PDB) named `XEPDB1`. Oracle 11g only has the CDB `XE`. | `.env` (root) |

## Solution Summary

| Metric | Count |
|--------|-------|
| Total modules in project | 1 (single Maven module) |
| Modules requiring migration | 1 |
| Modules already migrated | 0 |
| Modules skipped (no Oracle usage) | 0 |
| Test infrastructure (handled separately) | 1 (application-test.yml) |

## Project Inventory

| # | Module Name | Path | Classification | Notes |
|---|---|---|---|---|
| 1 | `ERP_SpringBoot-Experiment` | `pom.xml` | **MIGRATE** | Uses `ojdbc11` driver; Oracle JDBC URL in `application.yml`; `OraclePrivilegeInitializer.java` with raw JDBC for GRANT statements; `Oracle` compatibility mode in H2 test config; Docker infra runs Oracle XE container |

## Migration Order

1. **ERP_SpringBoot-Experiment** — Single Maven module; all Oracle dependencies, config, and code reside in this one project. No dependency ordering needed.

---

## Detailed Migration Scope

### 1. Maven Dependencies (`pom.xml`)

| Current (Oracle) | Target (PostgreSQL) |
|---|---|
| `com.oracle.database.jdbc:ojdbc11:21.9.0.0` | `org.postgresql:postgresql:42.x.x` |
| N/A | `org.postgresql:postgresql` (runtime scope) |

### 2. Application Configuration (`application.yml`)

| Setting | Oracle (current) | PostgreSQL (target) |
|---|---|---|
| `spring.datasource.url` | `jdbc:oracle:thin:@localhost:1521/XEPDB1` | `jdbc:postgresql://localhost:5432/erp_db` |
| `spring.datasource.driver-class-name` | `oracle.jdbc.OracleDriver` | `org.postgresql.Driver` |
| `spring.jpa.database-platform` / `spring.jpa.properties.hibernate.dialect` | (none — Hibernate 6 auto-detects `org.hibernate.dialect.OracleDialect` from `ojdbc11` driver) | `org.hibernate.dialect.PostgreSQLDialect` (explicitly set, or let Hibernate auto-detect from `postgresql` driver) |

### 3. Test Configuration (`application-test.yml`)

| Setting | Oracle (current) | PostgreSQL (target) |
|---|---|---|
| JDBC URL | `jdbc:h2:mem:testdb;MODE=Oracle;...` | `jdbc:h2:mem:testdb;MODE=PostgreSQL;...` + switch to Testcontainers with `postgresql:latest` (recommended) |

### 4. Oracle-Specific Code

| File | Issue | Action |
|---|---|---|
| `OraclePrivilegeInitializer.java` | Raw JDBC with Oracle `GRANT` syntax (`CREATE TABLE/VIEW/SEQUENCE`) | Rewrite to use PostgreSQL schema/privilege model or remove (PostgreSQL role-based privileges differ) |

### 5. JPA / Hibernate Entities (No direct Oracle dependencies found)

- All entities use JPA annotations (`@Entity`, `@Table`, `@Column`) — should be database-agnostic
- No Oracle-specific annotations (`@SequenceGenerator` with Oracle sequences) found in scan
- Verify any `@GeneratedValue(strategy = GenerationType.SEQUENCE)` — PostgreSQL supports sequences natively

### 6. Docker Infrastructure (`src/docker/`)

| Service | Oracle (current) | PostgreSQL (target) |
|---|---|---|
| Database | `gvenzl/oracle-xe:21-slim-faststart` (previously `11.2.0.2-slim` — upgraded to fix Hibernate 6+ compat) | `postgres:16-alpine` |
| Volume | `oracle-data:/opt/oracle/oradata` | `postgres-data:/var/lib/postgresql/data` |
| Port | `1521` | `5432` |
| Healthcheck | `tnsping XE 1` | `pg_isready -U $POSTGRES_USER` |

### 7. Spring App Env Overrides

| Variable | Oracle (current) | PostgreSQL (target) |
|---|---|---|
| `SPRING_DATASOURCE_URL` | `jdbc:oracle:thin:@oracle-db:1521/XEPDB1` | `jdbc:postgresql://postgres-db:5432/erp_db` |
| `SPRING_DATASOURCE_USERNAME` | `system` | `erp_user` |
| `SPRING_DATASOURCE_PASSWORD` | `${DB_PASSWORD}` | `${DB_PASSWORD}` |
| Remove `SPRING_JPA_HIBERNATE_DDL_AUTO: update` (no equivalent grant step needed) | — | Optional: keep or change to `validate` for production |
| Remove `OraclePrivilegeInitializer` component | — | Not needed for PostgreSQL |

### 8. Potential DDL / Migration Concerns

| Concern | Impact |
|---|---|
| `@Column(columnDefinition = "CLOB")` or `BLOB` | PostgreSQL uses `TEXT` / `BYTEA` — verify entity column definitions |
| `GenerationType.SEQUENCE` vs `GenerationType.IDENTITY` | PostgreSQL supports both — verify `@GeneratedValue` strategy |
| Oracle `VARCHAR2(255)` | Maps to PostgreSQL `VARCHAR(255)` — compatible |
| Oracle `NUMBER(19,2)` for monetary | Maps to PostgreSQL `NUMERIC(19,2)` — compatible |
| Soft delete `deletedAt` in `BaseEntity.java` | Uses `LocalDateTime` — compatible across both |
| Index names with trailing spaces (e.g., `idx_address_user `) | PostgreSQL trims identifiers — fix during migration |

### 9. Environment Variables (`.env` files)

| Variable | Current Use | Target |
|---|---|---|
| `DB_URL` | `jdbc:oracle:thin:@localhost:1521/XE` | `jdbc:postgresql://localhost:5432/erp_db` |
| `DB_USERNAME` | `Spring_app` | `erp_user` |
| `DB_PASSWORD` | `ddinhnn11` / `oracle@2025` | (update to new credentials) |
