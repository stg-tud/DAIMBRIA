package de.daimpl.demo.shared

import com.fasterxml.jackson.databind.node.JsonNodeType
import de.daimpl.daimbria.*
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Instant

@Configuration
class SchemaConfig {

    @Bean
    fun transformationEngine(): TransformationEngine = TransformationEngine

    @Bean
    fun lensGraph(): LensGraph {
        val lens1 = lens("1.0", "1.1") {
            rename("userId", "id")
            rename("username", "name")
            convert("registeredAt") {
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
            add("userId", STRING, "")
            add("username", STRING, "")
            add("email", STRING, "")
            add("registeredAt", NUMBER, 0)
            add("preferences", OBJECT)
            add("theme", STRING)
            plunge("theme", "preferences")
            add("tags", STRING, "")
        }

        return lensGraph(lens0) {
            +lens1
            +lens2
        }
    }
}