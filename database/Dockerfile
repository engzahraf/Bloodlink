# ========================================================
# BloodLink Database Module - Official Dockerfile
# Multi-stage build for minimum image size & high security
# ========================================================

# Stage 1: Compile Java sources
FROM eclipse-temurin:21-jdk AS builder

WORKDIR /build

# Copy JDBC library and source code
COPY lib/ ./lib/
COPY src/ ./src/

# Compile all main and test Java classes
RUN mkdir -p bin && \
    find src -name "*.java" > sources.txt && \
    javac -cp "lib/*" -d bin @sources.txt

# Stage 2: Lightweight JRE runtime
FROM eclipse-temurin:21-jre

LABEL maintainer="Member 03 - Database Developer <bloodlink@project.org>"
LABEL description="BloodLink SQLite Database Module"

WORKDIR /app

# Copy compiled bytecode and SQLite JDBC driver
COPY --from=builder /build/bin ./bin
COPY --from=builder /build/lib ./lib

# Create directory for persistent SQLite database storage
RUN mkdir -p /app/data

# Declare volume for data persistence
VOLUME ["/app/data"]

# Default database path pointing to the mounted volume
ENV BLOODLINK_DB_PATH=/app/data/bloodlink.db

# Generate entrypoint script that supports 'init', 'test', or custom Java class execution
RUN printf '#!/bin/sh\n\
set -e\n\
if [ "$1" = "test" ]; then\n\
  echo "Running BloodLink Database Automated Test Suite..."\n\
  exec java -cp "bin:lib/*" com.bloodlink.database.DatabaseTestSuite\n\
elif [ "$1" = "init" ] || [ -z "$1" ]; then\n\
  echo "Running BloodLink Database Initializer..."\n\
  exec java -cp "bin:lib/*" com.bloodlink.database.DatabaseInitializer\n\
else\n\
  exec java -cp "bin:lib/*" "$@"\n\
fi\n' > /app/entrypoint.sh && chmod +x /app/entrypoint.sh

ENTRYPOINT ["/app/entrypoint.sh"]
CMD ["init"]

