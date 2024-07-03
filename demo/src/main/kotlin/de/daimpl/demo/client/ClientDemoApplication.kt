package de.daimpl.demo.client

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.builder.SpringApplicationBuilder

@SpringBootApplication
class ClientDemoApplication

fun main(args: Array<String>) {
    SpringApplicationBuilder(ClientDemoApplication::class.java)
        .properties(mapOf("server.port" to "8082"))
        .run(*args)
}

