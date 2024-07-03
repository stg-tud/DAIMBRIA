package de.daimpl.daimbria

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.JsonNodeType
import de.daimpl.daimbria.JsonNodeExtensions.asArrayNode
import de.daimpl.daimbria.JsonNodeExtensions.asObjectNode
import de.daimpl.daimbria.JsonNodeExtensions.copy
import de.daimpl.daimbria.JsonNodeExtensions.head
import de.daimpl.daimbria.JsonNodeExtensions.hoist
import de.daimpl.daimbria.JsonNodeExtensions.plunge
import de.daimpl.daimbria.JsonNodeExtensions.putNumber
import de.daimpl.daimbria.JsonNodeExtensions.rename
import de.daimpl.daimbria.JsonNodeExtensions.wrap

/**
 * TransformationEngine transforms JsonFields on Run-time, therefore here should be thrown as little as possible
 * to ensure a smooth running application.
 *
 * ATTENTION: This also means here happens almost no validation and should be used wisely stand-alone.
 */
object TransformationEngine {

    fun applyMigrations(json: JsonNode, ops: List<LensOp>): JsonNode = json.apply {
        ops.forEach { op -> delegateOp(op) }
    }

    private fun JsonNode.delegateOp(op: LensOp) {
        when (op) {
            is RenameProperty -> with(asObjectNode()) {
                rename(op.from, op.to)
            }

            is AddProperty -> with(asObjectNode()) {
                when (op.type) {
                    JsonNodeType.STRING -> put(op.name, op.default as String?)
                    JsonNodeType.BOOLEAN -> put(op.name, op.default as Boolean?)
                    JsonNodeType.NUMBER -> putNumber(op.name, op.default as Number?)
                    JsonNodeType.OBJECT -> putObject(op.name)
                    JsonNodeType.ARRAY -> putArray(op.name)
                    else -> TODO()
                }
            }

            is RemoveProperty -> with(asObjectNode()) {
                remove(op.name)
            }

            is CopyProperty -> with(asObjectNode()) {
                copy(op.from, op.newField)
            }

            is HeadProperty -> with(asObjectNode()) {
                head(op.name)
            }

            is WrapProperty -> with(asObjectNode()) {
                wrap(op.name)
            }

            is HoistProperty -> with(asObjectNode()) {
                hoist(op.target, op.from)
            }

            is PlungeProperty -> with(asObjectNode()) {
                plunge(op.target, op.to)
            }

            is LensIn -> with(asObjectNode()) {
                val property = this[op.target]
                applyMigrations(property, op.lens)
            }

            is LensMap -> with(asArrayNode()) {
                forEach { jsonNode ->
                    applyMigrations(jsonNode, op.lensOps)
                }
            }

            is ConvertValue<*, *> -> with(asObjectNode()) {
                op as ConvertValue<Any?, Any?>

                val property = this[op.name]
                val typedValue = when (op.typeConversion.from) {
                    JsonNodeType.STRING -> property.asText()
                    JsonNodeType.NUMBER -> property.asInt()
                    JsonNodeType.BOOLEAN -> property.asBoolean()
                    else -> TODO("Convert only applicable for String, Boolean, Number at the moment.")
                }

                val mappedValue = op.mapping(typedValue)
                when (op.typeConversion.to) {
                    JsonNodeType.STRING -> put(op.name, mappedValue as String?)
                    JsonNodeType.NUMBER -> putNumber(op.name, mappedValue as Number?)
                    JsonNodeType.BOOLEAN -> put(op.name, mappedValue as Boolean?)
                    else -> TODO("Convert only applicable for String, Boolean, Number at the moment.")
                }
            }
        }
    }
}
