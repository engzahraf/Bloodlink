# BloodLink – Database Module

**Role:** Member 03 – Database Developer  
**Module:** SQLite Database, JDBC, Models, DAOs, Security, and Docker Infrastructure  
**Target:** BloodLink – Real-Time Blood Donor and Recipient Communication System

---

## 1. Module Overview
This module houses the complete SQLite persistence layer for BloodLink. It provides:
- **Relational Schema:** 6 normalized 3NF tables (`users`, `donor_profiles`, `blood_requests`, `donations`, `notifications`, `messages`) with foreign keys, constraints, and 14 indexes.
- **Connection Management:** Connection vending with automatic WAL mode, 5000ms busy timeout, and enforced foreign keys.
- **Security:** OWASP-compliant salted PBKDF2 password hashing with HMAC-SHA256.
- **Data Access:** Full DAO layer using parameterized `PreparedStatement` queries to prevent SQL injection.
- **Docker Support:** Standalone containerization for schema initialization, automated testing, and volume-mounted persistent SQLite storage.

---

## 2. Docker Architecture

The Docker container isolates the SQLite database module within an official OpenJDK runtime environment:

```text
[ Host Machine ]
       │
       ▼ (Volume Mount: -v bloodlink-data:/app/data)
┌────────────────────────────────────────────────────────┐
│  Docker Container (bloodlink-database)                 │
│                                                        │
│  /app/                                                 │
│  ├── bin/                 (Compiled Java Bytecode)     │
│  ├── lib/                 (SQLite JDBC Driver)         │
│  ├── data/  ◄────────────► (Mounted SQLite Storage)    │
│  │   └── bloodlink.db                                  │
│  └── entrypoint.sh        (CLI Controller)             │
│        ├── 'init'  ──► DatabaseInitializer             │
│        └── 'test'  ──► DatabaseTestSuite               │
└────────────────────────────────────────────────────────┘
```

- **Base Image:** `eclipse-temurin:21-jdk` (Build) / `eclipse-temurin:21-jre` (Runtime multi-stage).
- **Driver:** Bundled `sqlite-jdbc-3.51.3.0.jar`.
- **Database Engine:** Embedded SQLite (no separate database server process required).
- **Data Persistence:** SQLite database file is persisted via volume mounted to `/app/data`.

---

## 3. Prerequisites
- Docker Engine 20.10+ / Docker Desktop.
- Java JDK 17+ (if building/running locally without Docker).

---

## 4. Docker Build Command

Build the Docker image using the database module context:

```bash
# From the project root:
docker build -t bloodlink-database database/

# OR from inside the database/ directory:
cd database
docker build -t bloodlink-database .
```

---

## 5. Docker Run Commands

### 5.1 Initialize Database with Persistent Volume Mount
To initialize the SQLite database schema, create tables, create indexes, and seed the default admin account:

#### Using Named Docker Volume (Recommended)
```bash
docker run --rm -v bloodlink-data:/app/data bloodlink-database init
```

#### Using Local Directory Bind Mount (Windows PowerShell)
```powershell
docker run --rm -v "${PWD}/data:/app/data" bloodlink-database init
```

#### Using Local Directory Bind Mount (Linux / macOS)
```bash
docker run --rm -v "$(pwd)/data:/app/data" bloodlink-database init
```

The container will:
1. Detect or create `bloodlink.db` inside `/app/data`.
2. Execute all schema DDL and indexes safely (`IF NOT EXISTS`).
3. Seed the default administrator (`admin@bloodlink.com`).
4. Exit cleanly with status code `0`.

---

### 5.2 Run the Automated Test Suite in Docker
Execute the full 22-test automated verification suite inside the container:

```bash
docker run --rm bloodlink-database test
```

Expected output:
```text
========================================================================
     BLOODLINK DATABASE COMPREHENSIVE AUTOMATED TEST SUITE             
========================================================================
[SETUP] Configuring isolated test database at: data/test_bloodlink.db
[DatabaseInitializer] Database schema and indexes initialized successfully.
  [PASS] 1. Database connection                   - Successfully obtained valid SQLite connection
  [PASS] 2. Database initialization               - Idempotent schema initialization executed without error
  [PASS] 3. Table creation                        - All 6 required relational tables verified in SQLite catalog
  ...
  [PASS] 22. Account blocking                     - Account successfully blocked and excluded from active donor queries
========================================================================
TEST SUMMARY: Total: 22 | Passed: 22 | Failed: 0
========================================================================
```

---

### 5.3 Run a Custom Java Class inside Container
You can run any specific class by passing its fully qualified name:

```bash
docker run --rm bloodlink-database com.bloodlink.database.DatabaseInitializer
```

---

## 6. Inspecting Container Logs
If running the container in detached mode (`-d`):

```bash
# Run container in background
docker run -d --name bloodlink-db-init -v bloodlink-data:/app/data bloodlink-database init

# View output logs
docker logs bloodlink-db-init

# Inspect exit code (0 = success)
docker inspect bloodlink-db-init --format='{{.State.ExitCode}}'

# Cleanup
docker rm bloodlink-db-init
```

---

## 7. Troubleshooting

| Symptom | Root Cause | Solution |
| :--- | :--- | :--- |
| `database locked (SQLITE_BUSY)` | Concurrent access without WAL mode | WAL mode is preconfigured in `DatabaseConnection`. Ensure you mount to a local filesystem volume that supports file locking. |
| `permission denied` on volume | Docker user permissions on Linux | Ensure host directory permissions allow Docker daemon writes: `chmod 777 data`. |
| `foreign key constraint failed` | Referential integrity violation | `PRAGMA foreign_keys = ON;` is strictly enforced. Ensure referenced entities exist before inserting child records. |
| Changes not persisting | Container ran without volume | Always provide `-v bloodlink-data:/app/data` or `-v "$(pwd)/data:/app/data"`. |

