package de.daimpl.daimbria

import com.fasterxml.jackson.databind.node.JsonNodeType
import kotlin.reflect.KType
import kotlin.reflect.full.createType
import kotlin.reflect.full.isSupertypeOf
import kotlin.reflect.jvm.ExperimentalReflectionOnLambdas
import kotlin.reflect.jvm.reflect

sealed class LensOp {
    abstract fun reverse(): LensOp
    open val isLossy: Boolean = false
}

typealias ConvertFunction<FROM, TARGET> = (FROM) -> TARGET

data class TypeConversion(val from: JsonNodeType, val to: JsonNodeType) {
    fun reverse() = TypeConversion(to, from)
}

/**
 * Define conversion logic for mapping enums, types, etc. using Kotlin higher order functions (lambdas)
 *
 * Validates upon creation if ([mapping] / [reverseMapping]) parameter and return types are according to [typeConversion]
 * @property name the name of the field to convert
 * @property mapping the mapping function to apply
 * @property reverseMapping the reverse mapping function to apply in case of backward compatibility
 * @property typeConversion defines the source and target [JsonNodeType] the conversion targets
 */
data class ConvertValue<FROM : Any?, TO : Any?>(
    val name: String,
    val mapping: ConvertFunction<FROM, TO>,
    val reverseMapping: ConvertFunction<TO, FROM>,
    val typeConversion: TypeConversion
) : LensOp() {

    init {
        validateFunctionTypes()
    }

    @OptIn(ExperimentalReflectionOnLambdas::class)
    private fun validateFunctionTypes() {
        // Carefull! Used non-null asserted (!!) here because we expect well defined lambdas
        val mappingParameterType = mapping.reflect()!!.parameters.first().type
        val mappingReturnType = mapping.reflect()!!.returnType

        val reverseMappingParameterType = reverseMapping.reflect()!!.parameters.first().type
        val reverseMappingReturnType = reverseMapping.reflect()!!.returnType

        require(
            mappingParameterType.isSupertypeOf(reverseMappingReturnType)
                    || reverseMappingReturnType.isSupertypeOf(mappingParameterType)
        ) { "Convert mapping parameter type and reverse mapping return type do not match! Convert field '$name'" }

        require(
            mappingReturnType.isSupertypeOf(reverseMappingParameterType)
                    || reverseMappingParameterType.isSupertypeOf(mappingReturnType)
        ) { "Convert mapping return type and reverse mapping parameter type do not match! Convert field '$name'" }

        // todo use variable for nullable instead of hard-coded (after nullability validation implemented)
        validateType(mappingParameterType, typeConversion.from, true)
        validateType(mappingReturnType, typeConversion.to, true)
        validateType(reverseMappingParameterType, typeConversion.to, true)
        validateType(reverseMappingReturnType, typeConversion.from, true)
    }

    // The method isSupertypeOf(type) returns true if type is supertype OR the same
    // -> The == / equals method doesn't work here
    private fun validateType(actualFunctionType: KType, expectedJsonType: JsonNodeType, nullable: Boolean) {
        when (expectedJsonType) {
            JsonNodeType.STRING -> {
                val stringType = String::class.createType(nullable = nullable)
                require(stringType.isSupertypeOf(actualFunctionType)) {
                    "Convert field '$name': Defined type was String but function uses ${actualFunctionType.classifier}"
                }
            }

            JsonNodeType.NUMBER -> {
                val numberType = Number::class.createType(nullable = nullable)
                require(numberType.isSupertypeOf(actualFunctionType)) {
                    "Convert field '$name': Defined type was Number but function uses ${actualFunctionType.classifier}"
                }
            }

            JsonNodeType.BOOLEAN -> {
                val booleanType = Boolean::class.createType(nullable = nullable)
                require(booleanType.isSupertypeOf(actualFunctionType)) {
                    "Convert field '$name': Defined type was Boolean but function uses ${actualFunctionType.classifier}"
                }
            }

            else -> TODO("Other types for convert validation currently not supported!")
        }
    }

    override fun reverse() = ConvertValue(
        name = name,
        mapping = reverseMapping,
        reverseMapping = mapping,
        typeConversion = typeConversion.reverse()
    )
}

/**
 * Renames a property
 */
data class RenameProperty(val from: String, val to: String) : LensOp() {
    override fun reverse() = RenameProperty(to, from)
}

/**
 * Adds a property of certain type and name
 *
 * @property name name of the property to be added
 * @property type the json type of added property
 * @property default a default applied to the new field after transformation
 */
data class AddProperty(val name: String, val type: JsonNodeType, val default: Any? = null) : LensOp() {
    override fun reverse() = RemoveProperty(name)
    override val isLossy = true
}

/**
 * Removes a property of certain name
 *
 * @property name name of the property to be removed
 * @property type type information needed for inverse operator
 * @property default default needed for inverse operator
 */
data class RemoveProperty(val name: String, val type: JsonNodeType, val default: Any? = null) : LensOp() {
    // type and default not necessary when constructed as reverse of AddProperty
    constructor(name: String) : this(name, JsonNodeType.MISSING, null)

    override fun reverse() = AddProperty(name, type, default)
}

/**
 * Copies a property node
 */
data class CopyProperty(val from: String, val newField: String) : LensOp() {
    override fun reverse() = CopyProperty(newField, from)
}

/**
 * Move property out of inner object
 *
 * @property target name of the property to be hoisted
 * @property from name of the property from where to hoist
 */
data class HoistProperty(val target: String, val from: String) : LensOp() {
    override fun reverse() = PlungeProperty(target, from)
}

/**
 * Move property into inner object
 *
 * @property target name of the property to be plunged
 * @property to name of the property to which is plunged
 *
 */
data class PlungeProperty(val target: String, val to: String) : LensOp() {
    override fun reverse() = HoistProperty(target, to)
}

/**
 * Wraps a scalar value to an array containing the value
 */
data class WrapProperty(val name: String) : LensOp() {
    override fun reverse() = HeadProperty(name)
}

/**
 * Converts an array to a scalar by taking its first value
 */
data class HeadProperty(val name: String) : LensOp() {
    override fun reverse() = WrapProperty(name)
    override val isLossy = true
}

/**
 * Runs lens operators inside an object
 */
data class LensIn(val target: String, val lens: List<LensOp>) : LensOp() {
    override fun reverse() = LensIn(target, lens.reversed())
}

/**
 * Applies lens operators for each element in outer object
 */
data class LensMap(val lensOps: List<LensOp>) : LensOp() {
    override fun reverse() = LensMap(lensOps.reversed())
}