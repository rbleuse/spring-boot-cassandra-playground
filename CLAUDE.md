# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

Build and test (Gradle wrapper, Java 25 toolchain auto-provisioned):

- `./gradlew build` — compile + test + assemble
- `./gradlew test` — run all tests
- `./gradlew test --tests "com.github.rbleuse.playground.SpringBootCassandraPlaygroundApplicationTests.contextLoads"` — single test
- `./gradlew bootRun` — run the app (Spring Boot's docker-compose integration auto-starts `compose.yaml`)
- `./gradlew ktlintCheck` / formatting tasks are not configured

Both `test` and `bootRun` set `FLYWAY_NATIVE_CONNECTORS=true` (required by Flyway's Cassandra connector — see `build.gradle.kts`).

## Architecture

This is a minimal Spring Boot 4 + Kotlin project demonstrating Spring Data Cassandra against **Apache Cassandra 6.0-alpha1** (a pre-release version not on Docker Hub officially), with Flyway-managed schema migrations.

Key wiring (read these together to understand startup):

- `CassandraConfiguration.kt` — defines the `cqlSession` bean. **It first connects to the `system` keyspace** to `CREATE KEYSPACE IF NOT EXISTS` the configured keyspace, then returns a session bound to that keyspace. It also defines `flywayKeyspaceOrderingCustomizer`, a `FlywayConfigurationCustomizer` bean that takes `CqlSession` as a constructor parameter — Spring's bean dependency graph then forces `cqlSession` to initialize before the starter's Flyway bean.
- `spring-boot-starter-flyway-nc` (resolved from the Maven Central snapshots repository at `https://central.sonatype.com/repository/maven-snapshots/`, declared in `build.gradle.kts` and scoped to snapshots only via `mavenContent { snapshotsOnly() }`) — provides `FlywayNcAutoConfiguration`, which builds a `Flyway` bean from `spring.flyway-nc.*` properties and a `FlywayNcMigrationInitializer` that triggers `migrate()` on bean lifecycle. Migrations live in `src/main/resources/db/migration/V*.cql`. The Flyway-related dependencies (`flyway-core`, `flyway-verb-migrate`, `flyway-nc-scanners`) come transitively from the starter; only `flyway-database-nc-cassandra` is declared directly here as the actual NC driver.
- `application.yaml` — `spring.cassandra.schema-action: none` (Flyway owns the schema). `spring.flyway-nc.url` uses the `cassandra://` native scheme, not JDBC. `spring.flyway-nc.migration-suffixes: [.cql]` is required because Flyway's native default is `.sql`.

### Cassandra 6 image

Cassandra 6.0-alpha1 has no official Docker image. `dev-tools/Dockerfile` builds one (published as `rbleuse/apache-cassandra:6.0-alpha1`), adapted from the official Cassandra 5 Dockerfile. Both `compose.yaml` and the Testcontainers initializer reference this image and bind-mount `dev-tools/cassandra.yaml`.

### Tests

`CassandraContainerInitializer` (an `ApplicationContextInitializer`, not a `@DynamicPropertySource`) starts a single shared `CassandraContainer` and overrides `spring.cassandra.contact-points`, `spring.cassandra.local-datacenter`, and **`spring.flyway-nc.url`** (must be rewritten because Flyway's URL embeds host:port and datacenter). When adding new integration tests, apply this initializer via `@ContextConfiguration(initializers = [...])` rather than re-deriving container properties.