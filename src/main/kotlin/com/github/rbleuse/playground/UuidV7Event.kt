package com.github.rbleuse.playground

import org.springframework.data.cassandra.core.mapping.PrimaryKey
import org.springframework.data.cassandra.core.mapping.Table
import org.springframework.data.cassandra.repository.CassandraRepository
import java.util.UUID

@Table("uuid_v7_events")
data class UuidV7Event(
    @field:PrimaryKey
    val id: UUID,
    val payload: String,
)

interface UuidV7EventRepository : CassandraRepository<UuidV7Event, UUID>
