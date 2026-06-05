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

## How it fits together

- **`CassandraConfiguration`** — opens a session against the `system` keyspace, runs `CREATE KEYSPACE IF NOT EXISTS`, then returns a session bound to the configured keyspace. Required because Flyway expects the keyspace to exist.
- **`FlywayConfiguration`** — defines a `Flyway` bean (`initMethod = "migrate"`, `@DependsOn("cqlSession")`) so migrations run after keyspace bootstrap. Migrations live in `src/main/resources/db/migration/V*.cql`.
- **`application.yaml`** — `spring.cassandra.schema-action: none` (Flyway owns the schema). `spring.flyway.url` uses the `cassandra://` native scheme.

## Cassandra 6 image

Cassandra 6.0-alpha1 has no official Docker image. `dev-tools/Dockerfile` builds one (published as `rbleuse/apache-cassandra:6.0-alpha1`), adapted from the official Cassandra 5 Dockerfile. Both `compose.yaml` and the Testcontainers initializer reference this image and bind-mount `dev-tools/cassandra.yaml`.

## Tests

`CassandraContainerInitializer` starts a shared `CassandraContainer` and overrides `spring.cassandra.contact-points`, `spring.cassandra.local-datacenter`, and `spring.flyway.url`. New integration tests should apply it via `@ContextConfiguration(initializers = [...])`.
