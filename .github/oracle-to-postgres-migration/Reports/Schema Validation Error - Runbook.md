# Schema Validation Error — Diagnostic Runbook

**Error:** `Schema-validation: missing table [orders]`
**Observed:** 2026-06-09
**Frequency:** Expected on fresh databases after container recreation

---

## 1. Root Cause

```
spring.jpa.hibernate.ddl-auto: validate
```

`validate` checks that **all JPA entities match existing database tables**. If the database is empty (new container, volume wiped, cloned environment), it fails immediately — Hibernate won't create anything.

---

## 2. Quick Fix (Development)

1. **In `application.yml`:**
   ```yaml
   spring:
     jpa:
       hibernate:
         ddl-auto: update   # was: validate
   ```

2. **Restart the app.** Hibernate logs every `CREATE TABLE` statement (with `show-sql: true`).

3. **After tables are created**, switch back:
   ```yaml
   ddl-auto: validate
   ```
   to catch schema drift in future runs.

---

## 3. Detection Flowchart

```
App fails to start?
    │
    ├── Log shows "missing table [xxx]"?
    │       └── YES → Schema-validation error
    │
    ├── Log shows "HHH000511"?
    │       └── YES → Oracle version too old (11g). Fix: upgrade image.
    │
    ├── Log shows "IO Error: The Network Adapter"?
    │       └── YES → Connection refused. Fix: check container + port mapping.
    │
    └── Log shows "Connection is not available, request timed out"?
            └── YES → DB not ready yet. Fix: wait for healthcheck.
```

---

## 4. Preventative Patterns

### 4.1 Profile-based ddl-auto (Recommended)

Keep `validate` as the safe default, but let Docker / CI override it:

**`application.yml` (default):**
```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: validate    # fail fast if schema drifts
```

**`application-docker.yml` or compose env:**
```yaml
SPRING_JPA_HIBERNATE_DDL_AUTO: update    # auto-create for fresh DBs
```

### 4.2 Flyway / Liquibase (Production)

For production, never use `ddl-auto: update`. Use migration tools:

| Tool | Pros | Cons |
|---|---|---|
| **Flyway** | Versioned SQL scripts, Oracle + PostgreSQL support | Must write DDL per DB |
| **Liquibase** | DB-agnostic changelogs (XML/YAML/JSON) | Slightly steeper setup |

### 4.3 Init Scripts on First Run

For Docker Compose, mount SQL init scripts:

```yaml
services:
  oracle-db:
    volumes:
      - ./init-scripts:/docker-entrypoint-initdb.d   # Not supported by gvenzl/oracle-xe
```

Oracle XE doesn't auto-run init scripts like PostgreSQL. Instead, use `ddl-auto: update` or a `@PostConstruct` seeder.

---

## 5. Common Scenarios

| Scenario | Symptom | Fix |
|---|---|---|
| **Fresh Docker Compose** | `missing table [orders]` | Use `ddl-auto: update` once |
| **Volume wiped** | Same | Same — Hibernate recreates from entities |
| **New developer clone** | Same | Same — no DB dump exists |
| **Schema drift (entity changed)** | `missing column [xxx]` | `update` will add column; or write Flyway migration |
| **Wrong Oracle image** | `HHH000511` + validation fail | Upgrade Oracle image to 21c+ |

---

## 6. Commands Cheat Sheet

```bash
# Check current ddl-auto
grep 'ddl-auto' src/main/resources/application.yml

# Quick switch to update (one-time fix)
sed -i 's/ddl-auto: validate/ddl-auto: update/' src/main/resources/application.yml

# Switch back after tables created
sed -i 's/ddl-auto: update/ddl-auto: validate/' src/main/resources/application.yml

# Verify tables exist
docker exec oracle-db sqlplus -s system/oracle@2025@//localhost:1521/XEPDB1 <<< "
  SELECT table_name FROM user_tables ORDER BY table_name;
"

# Count tables
docker exec oracle-db sqlplus -s system/oracle@2025@//localhost:1521/XEPDB1 <<< "
  SELECT COUNT(*) AS table_count FROM user_tables;
"
```

---

## 7. Permanent Fix (Long-term)

When migrating to PostgreSQL, the same principle applies:

| Environment | `ddl-auto` strategy |
|---|---|
| Local dev | `update` (fast iteration) |
| CI / test | `create-drop` (clean state each run) |
| Staging | `validate` + Flyway |
| Production | Flyway / Liquibase only |
