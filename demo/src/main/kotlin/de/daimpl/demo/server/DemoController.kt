package de.daimpl.demo.server

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestAttribute
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/user")
class DemoController {
    private val mapper = ObjectMapper()

    @PostMapping
    fun processUser(
        @RequestAttribute("transformedJson") transformedJson: JsonNode,
    ): JsonNode {
        println("Transformed Payload for Server: ${mapper.writerWithDefaultPrettyPrinter().writeValueAsString(transformedJson)}")

        val modifiedData = transformedJson.deepCopy<ObjectNode>()
        modifiedData.put("name", "john")

        return modifiedData
    }
}
