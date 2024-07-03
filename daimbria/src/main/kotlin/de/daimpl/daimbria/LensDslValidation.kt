package de.daimpl.daimbria

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.JsonNodeType
import com.fasterxml.jackson.databind.node.ObjectNode
import de.daimpl.daimbria.JsonNodeExtensions.asArrayNode
import de.daimpl.daimbria.JsonNodeExtensions.asObjectNode
import de.daimpl.daimbria.JsonNodeExtensions.copy
import de.daimpl.daimbria.JsonNodeExtensions.head
import de.daimpl.daimbria.JsonNodeExtensions.hoist
import de.daimpl.daimbria.JsonNodeExtensions.plunge
import de.daimpl.daimbria.JsonNodeExtensions.rename
import de.daimpl.daimbria.JsonNodeExtensions.wrap

object LensDslValidation {

    fun createJsonRepresentation(validationModel: JsonNode, ops: List<LensOp>): JsonNode {
        ops.forEach { op -> validationModel.apply { delegateOp(op) } }
        return validationModel
    }

    private fun JsonNode.delegateOp(op: LensOp) {
        when (op) {
            is RenameProperty -> with(asObjectNode()) {
                require(has(op.from)) { "Cannot rename property '${op.from}' because it does not exist." }

                rename(op.from, op.to)
            }

            is AddProperty -> with(asObjectNode()) {
                require(!has(op.name)) { "Cannot add property '${op.name}' because it does already exist." }
                val typeMismatchMessage = """Cannot add property '${op.name}' because its default type 
                    (${op.default?.let { it::class.simpleName }} doesn't match definition (${op.type.name})."""

                when (op.type) {
                    JsonNodeType.STRING -> {
                        require(op.default == null || op.default is String) { typeMismatchMessage }
                        put(op.name, op.type.name)
                    }

                    JsonNodeType.NUMBER -> {
                        require(op.default == null || op.default is Number) { typeMismatchMessage }
                        put(op.name, op.type.name)
                    }

                    JsonNodeType.BOOLEAN -> {
                        require(op.default == null || op.default is Boolean) { typeMismatchMessage }
                        put(op.name, op.type.name)
                    }

                    JsonNodeType.OBJECT -> putObject(op.name)
                    JsonNodeType.ARRAY -> putArray(op.name)
                    else -> TODO("Adding other types than String, Boolean, Number, Object, Array currently not supported")
                }
            }

            is RemoveProperty -> with(asObjectNode()) {
                require(has(op.name)) { "Cannot remove property '${op.name}' because it does not exist." }
                val propertyType = getPropertyType(op.name)
                require(propertyType == op.type) { "Type of remove operation (${op.type}) has to be the same as property type ($propertyType)" }

                remove(op.name)
            }

            is CopyProperty -> with(asObjectNode()) {
                require(has(op.from)) { "Cannot copy property '${op.from}' because it does not exist." }
                require(!has(op.newField)) { "Cannot copy to new field '${op.newField}' because it already exists." }

                copy(op.from, op.newField)
            }

            is HeadProperty -> with(asObjectNode()) {
                require(has(op.name)) { "Cannot get head element from '${op.name}' because it does not exist." }

                head(op.name)
            }

            is WrapProperty -> with(asObjectNode()) {
                require(has(op.name)) { "Cannot wrap property '${op.name}' because it does not exist." }

                wrap(op.name)
            }

            is HoistProperty -> with(asObjectNode()) {
                require(has(op.from)) { "Cannot hoist '${op.target}' from property '${op.from}' because it does not exist." }

                hoist(op.target, op.from)
            }

            is PlungeProperty -> with(asObjectNode()) {
                require(has(op.to)) { "Cannot plunge '${op.target}' to property '${op.to}' because it does not exist." }

                plunge(op.target, op.to)
            }

            is LensIn -> with(asObjectNode()) {
                require(has(op.target)) { "Cannot apply lens operations to property '${op.target}' because it does not exist." }

                val property = this[op.target]
                createJsonRepresentation(property, op.lens)
            }

            is LensMap -> with(asArrayNode()) {
                forEach { jsonNode ->
                    createJsonRepresentation(jsonNode, op.lensOps)
                }
            }

            is ConvertValue<*, *> -> with(asObjectNode()) {
                require(has(op.name)) { "Cannot convert field '${op.name}' because it does not exist." }
                val propertyType = getPropertyType(op.name)
                require(propertyType == op.typeConversion.from) {
                    "Cannot convert field '${op.name}' because its type is not compatible to mapping function type."
                }

                when (val destinationType = op.typeConversion.to) {
                    JsonNodeType.STRING, JsonNodeType.NUMBER, JsonNodeType.BOOLEAN -> destinationType
                    else -> error("Json node type '$destinationType' currently not supported as target type for convert operation.")
                }

                // validation of function types in constructor of ConvertValue lens operator

                put(op.name, op.typeConversion.to.name)
            }
        }
    }

    private fun ObjectNode.getPropertyType(name: String): JsonNodeType =
        try {
            JsonNodeType.valueOf(this[name].asText())
        } catch (e: IllegalArgumentException) {
            error("Encountered unexpected error during convert validation: ${e.message}.")
        }
}
