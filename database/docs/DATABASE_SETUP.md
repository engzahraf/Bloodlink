# BloodLink – Database Setup Guide

## 1. Prerequisites
- **Java Development Kit (JDK)**: JDK 17, 21, or 25+ (verified with Temurin OpenJDK 25.0.2).
- **SQLite JDBC Driver**: Bundled locally in `database/lib/sqlite-jdbc-3.51.3.0.jar`.
- No separate database server installation (e.g., MySQL, PostgreSQL) is required.

---

## 2. Directory Layout
```text
Bloodlink/
└── database/
    ├── bin/                                  # Compiled class files
    ├── docs/                                 # Architecture & design documentation
    │   ├── DATABASE_SCHEMA.md
    │   ├── DATABASE_INTEGRATION_GUIDE.md
    │   ├── DATABASE_SETUP.md
    │   ├── DATABASE_TESTING.md
    │   └── ER_DIAGRAM.md
    ├── lib/                                  # Self-contained JDBC dependencies
    │   └── sqlite-jdbc-3.51.3.0.jar
    └── src/
        ├── main/
        │   └── java/
        │       └── com/bloodlink/database/
        │           ├── DatabaseConnection.java
        │           ├── DatabaseInitializer.java
        │           ├── dao/
        │           ├── enums/
        │           ├── exceptions/
        │           ├── models/
        │           └── security/
        └── test/
            └── java/
                └── com/bloodlink/database/
                    └── DatabaseTestSuite.java
```

---

## 3. Compilation Commands

### Windows (PowerShell)
```powershell
# Navigate to repository root
cd d:\Projects\Ongoing\BloodLink\Bloodlink

# Compile all source files into database/bin
$sources = (Get-ChildItem -Path "database\src" -Recurse -Filter "*.java").FullName
javac -cp "database\lib\*" -d "database\bin" $sources
```

### Linux / macOS (Bash)
```bash
mkdir -p database/bin
javac -cp "database/lib/*" -d database/bin $(find database/src -name "*.java")
```

---

## 4. Initializing the Database in Java Code
Call `DatabaseInitializer.initialize()` at server startup. The initializer will create the `data/` directory and `bloodlink.db` database automatically if they do not exist.

```java
import com.bloodlink.database.DatabaseInitializer;

public class ServerMain {
    public static void main(String[] args) {
        // 1. Initialize schema, tables, and indexes
        DatabaseInitializer.initialize();

        // 2. Optionally seed a default administrator if missing
        DatabaseInitializer.seedDefaultAdminIfMissing(
            "admin@bloodlink.com",
            "Admin@BloodLink2026",
            "System Administrator",
            "+94-77-1234567"
        );

        System.out.println("BloodLink database ready.");
    }
}
```

---

## 5. SQLite Performance & Concurrency Features
- **Foreign Keys**: Enforced on every connection using `PRAGMA foreign_keys = ON;`.
- **Write-Ahead Logging (WAL)**: Enabled via `PRAGMA journal_mode = WAL;`, allowing concurrent read operations without blocking concurrent write operations.
- **Busy Timeout**: Configured to `5000ms` (`PRAGMA busy_timeout = 5000;`) to prevent `SQLITE_BUSY` exceptions during concurrent socket traffic.

