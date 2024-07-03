package de.daimpl.demo.server

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.boot.runApplication
import org.springframework.context.annotation.ComponentScan

@SpringBootApplication
@ComponentScan(basePackages = ["de.daimpl.demo.server", "de.daimpl.demo.shared"])
class ServerDemoApplication

fun main(args: Array<String>) {
    SpringApplicationBuilder(ServerDemoApplication::class.java)
        .properties(mapOf("server.port" to "8081"))
        .run(*args)
}
