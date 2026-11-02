# MiniBank Transfers service

Java 17 / Spring Boot 3.5.16, JDBC, MySQL 8.0.16+ or 8.4, Flyway and Actuator. HTTP port 8080. The application uses an external persistent MySQL database, configured in `src/main/resources/application.properties`. No temporary database is started.

Create databases/users with `minibank-deployment/database/local-mysql-setup.sql` (review/change fictional passwords), then run:

```powershell
.\mvnw.cmd spring-boot:run
```

Set DB_URL, DB_USER, DB_PASSWORD to override the properties without changing code. Set the same BANK_INTERNAL_TOKEN on both services. Linux: `bash ./mvnw spring-boot:run`. Use the workspace local setup guide for VS Code, test database settings and API examples.

Tests require a dedicated `transfers_test` schema and TEST_DB_URL, TEST_DB_USER, TEST_DB_PASSWORD; they delete test data. CI creates its own MySQL 8.4 service container. Run `.\mvnw.cmd verify` after configuring test access. To package without database tests: `.\mvnw.cmd -DskipTests package`.

The Dockerfile builds a standalone JAR, runs it as a non-root user and uses a small JVM heap. Compose/Kubernetes databases, volumes and credentials are in the deployment repository. Each service owns its own database; services communicate by HTTP. Keep demo endpoints private: the public APIs do not implement customer authentication.
