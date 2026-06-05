package com.github.rbleuse.playground

import com.datastax.oss.driver.api.core.ConsistencyLevel
import com.datastax.oss.driver.api.core.CqlSession
import com.datastax.oss.driver.api.core.cql.SimpleStatement
import com.datastax.oss.driver.api.core.servererrors.InvalidQueryException
import io.github.rbleuse.flywaync.cassandra.CassandraFlywayNcAutoConfiguration
import io.kotest.assertions.throwables.shouldThrowExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.throwable.shouldHaveMessage
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.cassandra.test.autoconfigure.DataCassandraTest
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.context.annotation.Import
import org.testcontainers.cassandra.CassandraContainer
import org.testcontainers.utility.DockerImageName
import org.testcontainers.utility.MountableFile
import java.time.Duration

@DataCassandraTest
@Import(CassandraConfiguration::class, CassandraFlywayNcAutoConfiguration::class)
class SpringBootCassandraPlaygroundApplicationTests @Autowired constructor(
    private val cqlSession: CqlSession
) {
    companion object {
        @ServiceConnection
        val cassandra: CassandraContainer = CassandraContainer(
            DockerImageName
                .parse("rbleuse/apache-cassandra:6.0-alpha1")
                .asCompatibleSubstituteFor("cassandra")
        )
            .withEnv("CASSANDRA_ENDPOINT_SNITCH", "GossipingPropertyFileSnitch")
            .withCopyFileToContainer(
                MountableFile.forHostPath("dev-tools/cassandra.yaml"),
                "/etc/cassandra/cassandra.yaml"
            ).withStartupTimeout(Duration.ofMinutes(2))
    }

    @BeforeEach
    fun cleanup() {
        cqlSession.execute("TRUNCATE users_by_country;")
    }

    @Test
    fun `should insert with transaction`() {
        cqlSession.execute(
            SimpleStatement.newInstance(
            """
                BEGIN TRANSACTION
                INSERT INTO users_by_country (country, user_id) VALUES ('USA', 123e4567-e89b-12d3-a456-426614174000);
                INSERT INTO users_by_country (country, user_id) VALUES ('FRANCE', 123e4567-e89b-12d3-a456-426614174001);
                COMMIT TRANSACTION;
            """.trimIndent()
            ).setConsistencyLevel(ConsistencyLevel.QUORUM)
        )

        cqlSession.execute("select * from users_by_country").all() shouldHaveSize 2
    }

    @Test
    fun `should fail to insert with transaction when user id is null`() {
        shouldThrowExactly<InvalidQueryException> {
            cqlSession.execute(
                SimpleStatement.newInstance(
                """
                    BEGIN TRANSACTION
                    INSERT INTO users_by_country (country, user_id) VALUES ('USA', 123e4567-e89b-12d3-a456-426614174000);
                    INSERT INTO users_by_country (country, user_id) VALUES ('FRANCE', null);
                    COMMIT TRANSACTION;
                """.trimIndent()
                ).setConsistencyLevel(ConsistencyLevel.QUORUM)
            )
        }.shouldHaveMessage("Column value does not satisfy value constraint for column 'user_id' as it is null.")

        cqlSession.execute("select * from users_by_country").all() shouldHaveSize 0
    }
}
