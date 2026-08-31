package io.github.rbleuse.playground

import org.springframework.data.cassandra.core.mapping.PrimaryKey
import org.springframework.data.cassandra.core.mapping.Table
import org.springframework.data.cassandra.repository.CassandraRepository

@Table("snowflake_events")
data class SnowflakeEvent(
    @field:PrimaryKey
    val id: Long,
    val payload: String,
) {
    init {
        SnowflakeIdValidator.requireValid(id)
    }
}

interface SnowflakeEventRepository : CassandraRepository<SnowflakeEvent, Long>
