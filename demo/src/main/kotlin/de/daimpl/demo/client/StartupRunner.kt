package de.daimpl.demo.client

import org.springframework.boot.CommandLineRunner
import org.springframework.stereotype.Component

@Component
class StartupRunner(private val clientService: ClientService) : CommandLineRunner {

    override fun run(vararg args: String?) {
        clientService.sendRequest()
    }
}