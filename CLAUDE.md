# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

Build and test (Gradle wrapper, Java 25 toolchain auto-provisioned):

- `./gradlew build` — compile + test + assemble
- `./gradlew test` — run all tests
- `./gradlew test --tests "com.github.rbleuse.playground.SpringBootCassandraPlaygroundApplicationTests"` — single test class
- `./gradlew bootRun` — run the app (Spring Boot's docker-compose integration auto-starts `compose.yaml`)
- `./gradlew ktlintCheck` / formatting tasks are not configured

Both `test` and `bootRun` set `FLYWAY_NATIVE_CONNECTORS=true` (required by Flyway's Cassandra connector — see `build.gradle.kts`).

## Architecture

This is a minimal Spring Boot 4 + Kotlin project demonstrating Spring Data Cassandra against **Apache Cassandra 6.0-alpha1** (a pre-release version not on Docker Hub officially), with Flyway-managed schema migrations via the native (non-JDBC) Cassandra connector.

Key wiring (read these together to understand startup):

- `CassandraConfiguration.kt` — defines the `cqlSession` bean. **It first connects to the `system` keyspace** to `CREATE KEYSPACE IF NOT EXISTS` the configured keyspace, then returns a session bound to that keyspace. The class extends `AbstractDependsOnBeanFactoryPostProcessor(FlywayNcMigrationInitializer, CqlSession)`, which registers a bean dependency forcing `FlywayNcMigrationInitializer` to be created after `cqlSession` — so the keyspace exists before Flyway migrates.
- Dependencies come from the `io.github.rbleuse:spring-boot-flyway-nc-dependencies` BOM (imported via `dependencyManagement`) plus the `spring-boot-starter-flyway-nc-cassandra` starter (and `spring-boot-starter-flyway-nc-cassandra-test` for tests). These are snapshots resolved from the Maven Central snapshots repository at `https://central.sonatype.com/repository/maven-snapshots/`, declared in `build.gradle.kts` and scoped to snapshots only via `mavenContent { snapshotsOnly() }`. The starter provides `CassandraFlywayNcAutoConfiguration`, which configures Flyway from `spring.flyway-nc.*` properties and provides `FlywayNcMigrationInitializer`, which triggers `migrate()` during startup. Migrations live in `src/main/resources/db/migration/V*.cql`.
- `application.yaml` — `spring.cassandra.schema-action: none` (Flyway owns the schema). `spring.flyway-nc.default-schema` points at `${spring.cassandra.keyspace-name}`.

### Cassandra 6 image

Cassandra 6.0-alpha1 has no official Docker image. `dev-tools/Dockerfile` builds one (published as `rbleuse/apache-cassandra:6.0-alpha1`), adapted from the official Cassandra 5 Dockerfile. Both `compose.yaml` and the Testcontainers tests reference this image and bind-mount `dev-tools/cassandra.yaml`.

### Tests

`SpringBootCassandraPlaygroundApplicationTests` is a `@DataCassandraTest` that `@Import`s `CassandraConfiguration` and `CassandraFlywayNcAutoConfiguration`. It starts a shared `CassandraContainer` exposed via a `@ServiceConnection` companion field, so Spring Boot derives the Cassandra connection properties (contact points, datacenter, etc.) automatically — no manual property overrides or context initializer. Assertions use Kotest. New integration tests should follow the same pattern.
