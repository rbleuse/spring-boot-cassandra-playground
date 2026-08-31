package io.github.rbleuse.playground

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class SpringBootCassandraPlaygroundApplication

fun main(args: Array<String>) {
    runApplication<SpringBootCassandraPlaygroundApplication>(*args)
}
