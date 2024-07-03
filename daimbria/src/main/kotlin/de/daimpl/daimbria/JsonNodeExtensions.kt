package de.daimpl.daimbria

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import de.cmdjulian.jdsl.JacksonArrayNodeBuilder.Companion.arr
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

/**
 * Some json node extensions used for redundancy in [TransformationEngine] and [LensDslValidation]
 */
internal object JsonNodeExtensions {

    fun ObjectNode.rename(from: String, to: String) {
        // new objectNode to keep order
        val newObjectNode = ObjectMapper().createObjectNode()

        fieldNames().forEachRemaining { fieldName ->
            if (fieldName == from) {
                newObjectNode.set<JsonNode>(to, get(from))
            } else {
                newObjectNode.set<JsonNode>(fieldName, get(fieldName))
            }
        }

        removeAll()
        setAll<ObjectNode>(newObjectNode)
    }

    fun ObjectNode.copy(from: String, to: String) {
        val copyNode = get(from)

        putIfAbsent(to, copyNode)
    }

    fun ObjectNode.head(fieldName: String) {
        val property = this[fieldName]

        // Can't take first element of non-array
        val first = property.asArrayNode().firstOrNull()

        replace(fieldName, first)
    }

    fun ObjectNode.wrap(fieldName: String) {
        val property = this[fieldName]
        val newValue = arr {
            add(property)
        }
        replace(fieldName, newValue)
    }

    fun ObjectNode.hoist(targetName: String, sourceName: String) {
        val fromProperty = this[sourceName].asObjectNode()
        val property = fromProperty.remove(targetName)

        putIfAbsent(targetName, property)
    }

    fun ObjectNode.plunge(targetName: String, destinationName: String) {
        val toProperty = this[destinationName].asObjectNode()

        putIfAbsent(destinationName, objectNode())
        val property = remove(targetName)

        toProperty.putIfAbsent(targetName, property)
    }

    /**
     * Puts a number field in this ObjectNode.
     * Invokes the corresponding Jackson put method (Int, Long, Float, Double)
     */
    fun ObjectNode.putNumber(fieldName: String, number: Number?) {
        if (number == null) {
            putNull(fieldName)
            return
        }
        // autocast Number to corresponding type to invoke specific put method
        when (number) {
            is Int -> put(fieldName, number)
            is Long -> put(fieldName, number)
            is Float -> put(fieldName, number)
            is Double -> put(fieldName, number)
            else -> TODO("Unexpected number format")
        }
    }

    // validation

    /**
     * Checks if [JsonNode] is an [ObjectNode] and casts to [ObjectNode]
     *
     * @receiver [JsonNode] to check for
     * @throws InvalidJsonNodeTypeException
     */
    @OptIn(ExperimentalContracts::class)
    fun JsonNode.asObjectNode(): ObjectNode {
        contract {
            returns() implies (this@asObjectNode is ObjectNode)
        }
        if (!isObject) throw InvalidJsonNodeTypeException("Expected ObjectNode but got '${this@asObjectNode}'.")

        return this as ObjectNode
    }

    /**
     * Checks if [JsonNode] is an [ArrayNode] and smart casts to [ArrayNode]
     *
     * @receiver [JsonNode] to check for
     * @throws InvalidJsonNodeTypeException
     */
    @OptIn(ExperimentalContracts::class)
    fun JsonNode.asArrayNode(): ArrayNode {
        contract {
            returns() implies (this@asArrayNode is ArrayNode)
        }
        if (!isArray) throw InvalidJsonNodeTypeException("Expected ArrayNode but got '${this@asArrayNode}'.")

        return this as ArrayNode
    }
}