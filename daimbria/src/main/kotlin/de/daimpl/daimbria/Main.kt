package de.daimpl.daimbria

import com.fasterxml.jackson.databind.node.JsonNodeType
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import de.cmdjulian.jdsl.JacksonArrayNodeBuilder.Companion.arr
import de.daimpl.daimbria.JsonNodeExtensions.asObjectNode
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

class UnixTimestampConverter {
    fun convertToIso8601(unixTimestamp: Long): String {
        val instant = Instant.ofEpochSecond(unixTimestamp)
        val formatter = DateTimeFormatter.ISO_INSTANT.withZone(ZoneOffset.UTC)
        return formatter.format(instant)
    }
}

fun main() {
    // Program A JSON-Payload in schema 1.0
    //language=JSON
    val jsonPayloadA =
        """
        {
            "user_id": "12345",
            "username": "john_doe",
            "email": "john@example.com",
            "registered_at": 1622548800,
            "preferences": {
                "theme": "dark",
                "notifications": true
            },  
            "tags": "premium_user"
        }
        """.trimIndent()

    val mapper = jacksonObjectMapper()
    val prettyPrinter = mapper.writerWithDefaultPrettyPrinter()
    val jsonParsedA = mapper.readTree(jsonPayloadA)

    println("Incoming Payload from Program A: ${prettyPrinter.writeValueAsString(jsonParsedA)}")

    // Lens 1.0 -> 1.1
    val lens1 = lens("1.0", "1.1") {
        rename("user_id", "id")
        rename("username", "name")
        convert("registered_at") {
            NUMBER mapsTo STRING
            mapping { from: Number -> UnixTimestampConverter().convertToIso8601(from.toLong()) }
            reverseMapping { from: String? -> Instant.parse(from).epochSecond }
        }
    }

    // Lens 1.1 -> 1.2
    val lens2 = lens("1.1", "1.2") {
        remove("email", JsonNodeType.STRING)
        hoist("theme", "preferences")
        wrap("tags")
    }

    // initial schema 1.0
    // todo more complex init models currently not supported
    // todo -> add sub-adding
    val lens0 = lens("empty", "1.0") {
        add("user_id", STRING, "")
        add("username", STRING, "")
        add("email", STRING, "")
        add("registered_at", NUMBER, 0)
        add("preferences", OBJECT)
        add("theme", STRING)
        plunge("theme", "preferences")
        add("tags", STRING, "")
    }
    
    val lensGraph = lensGraph(lens0) {
        +lens1
        +lens2
    }

    // Program B transforms the data into schema 1.2
    val pathToB = lensGraph.lensFromTo("1.0", "1.2")
    val jsonTransformedToB = TransformationEngine.applyMigrations(jsonParsedA, pathToB)

    println("Transformed Payload for Program B: ${prettyPrinter.writeValueAsString(jsonTransformedToB)}")

    // Program B modifies the data
    val modifiedJsonB = jsonTransformedToB.asObjectNode().put("name", "john")

    // Although there are two tags now only the first is passed back to A
    modifiedJsonB.replace("tags", arr {
        add("premium_user")
        add("Darmstadt")
    })

    // Program B sends the modified data back to Program A in schema 1.0
    val pathToA = lensGraph.lensFromTo("1.2", "1.0")
    val jsonTransformedToA = TransformationEngine.applyMigrations(modifiedJsonB, pathToA)

    println("Modified by B and transformed back for A: ${prettyPrinter.writeValueAsString(jsonTransformedToA)}")
}
