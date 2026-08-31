package io.github.rbleuse.playground

import java.time.Clock

object SnowflakeIdValidator {
    const val EPOCH_MILLIS = 1_288_834_974_657L
    private const val TIMESTAMP_SHIFT = 22

    fun requireValid(
        id: Long,
        clock: Clock = Clock.systemUTC(),
    ) {
        require(id > 0) { "Snowflake ID must be positive" }

        val timestamp = EPOCH_MILLIS + (id ushr TIMESTAMP_SHIFT)
        require(timestamp <= clock.millis()) {
            "Snowflake ID timestamp must not be in the future"
        }
    }
}
