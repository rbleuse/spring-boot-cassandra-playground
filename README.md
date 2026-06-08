# spring-boot-cassandra-playground

Minimal Spring Boot 4 + Kotlin demo running Spring Data Cassandra against **Apache Cassandra 6.0-alpha1**, with Flyway-managed schema migrations via the native (non-JDBC) Cassandra connector.

## Requirements

- JDK 25 (auto-provisioned by the Gradle toolchain)
- Docker (for `compose.yaml` / Testcontainers)

## Commands

```sh
./gradlew build      # compile + test + assemble
./gradlew test       # run all tests
./gradlew bootRun    # run the app (auto-starts compose.yaml)
```

Both `test` and `bootRun` set `FLYWAY_NATIVE_CONNECTORS=true`, required by Flyway's Cassandra connector.

## Dependencies

The Flyway native connector wiring comes from the `io.github.rbleuse` artifacts: the `spring-boot-flyway-nc-dependencies` BOM plus the `spring-boot-starter-flyway-nc-cassandra` starter (and `-test` variant for tests). These are snapshots resolved from the Maven Central snapshots repository (`https://central.sonatype.com/repository/maven-snapshots/`, scoped to snapshots only).

## How it fits together

- **`CassandraConfiguration`** — defines the `cqlSession` bean: it opens a session against the `system` keyspace, runs `CREATE KEYSPACE IF NOT EXISTS`, then returns a session bound to the configured keyspace (Flyway expects the keyspace to already exist). The class extends `AbstractDependsOnBeanFactoryPostProcessor(FlywayNcMigrationInitializer, CqlSession)`, which forces `FlywayNcMigrationInitializer` to depend on `cqlSession` so the keyspace is bootstrapped before migrations run.
- **`CassandraFlywayNcAutoConfiguration`** (from the starter) — auto-configures Flyway from `spring.flyway-nc.*` properties and provides `FlywayNcMigrationInitializer`, which triggers `migrate()` during startup. Migrations live in `src/main/resources/db/migration/V*.cql`.
- **`application.yaml`** — `spring.cassandra.schema-action: none` (Flyway owns the schema). `spring.flyway-nc.default-schema` points at the configured keyspace.

## Cassandra 6 image

Cassandra 6.0-alpha1 has no official Docker image. `dev-tools/Dockerfile` builds one (published as `rbleuse/apache-cassandra:6.0-alpha1`), adapted from the official Cassandra 5 Dockerfile. Both `compose.yaml` and the Testcontainers tests reference this image and bind-mount `dev-tools/cassandra.yaml`.

## Tests

`SpringBootCassandraPlaygroundApplicationTests` is a `@DataCassandraTest` that imports `CassandraConfiguration` and `CassandraFlywayNcAutoConfiguration`. It starts a shared `CassandraContainer` exposed via a `@ServiceConnection` companion field, so Spring Boot derives the Cassandra connection properties automatically. New integration tests can follow the same pattern.