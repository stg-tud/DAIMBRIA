package de.daimpl.demo.client

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate

@Service
class ClientService(
    private val restTemplate: RestTemplate,
) {
    private val mapper = ObjectMapper()

    fun sendRequest() {
        val userDto =
            UserDto(
                userId = "12345",
                username = "john_doe",
                email = "john@example.com",
                registeredAt = 1622548800,
                preferences = Preferences("dark", true),
                tags = "premium_user",
            )
        val payload = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(userDto)
        val jsonPayload: JsonNode = mapper.readTree(payload)

        println("Payload sending from Client: $payload")

        val headers = HttpHeaders()
        headers.set("X-Schema-Version", "1.0")
        headers.set("Content-Type", "application/json")

        val entity = HttpEntity(jsonPayload.toString(), headers)

        val response: ResponseEntity<String> =
            restTemplate.exchange(
                "http://localhost:8081/api/user",
                HttpMethod.POST,
                entity,
                String::class.java,
            )

        val jsonResponse: JsonNode = mapper.readTree(response.body)
        val prettyJson = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonResponse)
        println("Response from server: $prettyJson")
    }
}
