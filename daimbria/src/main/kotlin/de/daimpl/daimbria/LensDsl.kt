package de.daimpl.daimbria

import com.fasterxml.jackson.databind.node.JsonNodeType

fun lens(from: String, to: String, block: LensDsl.() -> Unit): Lens {
    val dsl = LensDsl()
    dsl.block()
    return dsl.buildLensDefinition(from, to)
}

fun rootLens(version: String, block: LensDsl.() -> Unit) = lens("empty", version, block)

internal fun lensOps(block: LensDsl.() -> Unit): List<LensOp> {
    val dsl = LensDsl()
    dsl.block()
    return dsl.buildLensOps()
}

class LensDsl {
    val STRING = JsonNodeType.STRING
    val NUMBER = JsonNodeType.NUMBER
    val BOOLEAN = JsonNodeType.BOOLEAN
    val OBJECT = JsonNodeType.OBJECT
    val ARRAY = JsonNodeType.ARRAY

    private val lensOps = mutableListOf<LensOp>()

    fun rename(from: String, to: String) {
        val op = RenameProperty(from = from, to = to)
        lensOps.add(op)
    }

    fun add(name: String, type: JsonNodeType, default: Any? = null) {
        val op = AddProperty(name = name, type = type, default = default)
        lensOps.add(op)
    }

    fun remove(name: String, type: JsonNodeType, default: Any? = null) {
        val op = RemoveProperty(name = name, type = type, default = default)
        lensOps.add(op)
    }

    fun copy(from: String, to: String) {
        val op = CopyProperty(from = from, newField = to)
        lensOps.add(op)
    }

    fun hoist(target: String, from: String) {
        val op = HoistProperty(target = target, from = from)
        lensOps.add(op)
    }

    fun plunge(target: String, to: String) {
        val op = PlungeProperty(target = target, to = to)
        lensOps.add(op)
    }

    fun wrap(target: String) {
        val op = WrapProperty(name = target)
        lensOps.add(op)
    }

    fun head(target: String) {
        val op = HeadProperty(name = target)
        lensOps.add(op)
    }

    fun lensIn(name: String, block: LensDsl.() -> Unit) {
        val lens = lensOps(block)
        val op = LensIn(name, lens)
        lensOps.add(op)
    }

    fun lensMap(block: LensDsl.() -> Unit) {
        val lens = lensOps(block)
        val op = LensMap(lens)
        lensOps.add(op)
    }

    fun <FROM, TARGET> convert(name: String, block: ConvertDsl<FROM, TARGET>.() -> Unit) {
        val dsl = ConvertDsl<FROM, TARGET>(name)
        dsl.block()

        lensOps.add(dsl.buildOp())
    }

    inner class ConvertDsl<FROM, TARGET>(private val name: String) {

        private lateinit var mapping: ConvertFunction<FROM, TARGET>

        private lateinit var reverseMapping: ConvertFunction<TARGET, FROM>

        private lateinit var typeConversion: TypeConversion

        fun mapping(func: ConvertFunction<FROM, TARGET>) {
            mapping = func
        }

        fun reverseMapping(func: ConvertFunction<TARGET, FROM>) {
            reverseMapping = func
        }

        infix fun JsonNodeType.mapsTo(target: JsonNodeType) {
            typeConversion = TypeConversion(this, target)
        }

        fun buildOp(): ConvertValue<FROM, TARGET> {
            require(::typeConversion.isInitialized) { "Type Conversion was not provided but is required" }

            return ConvertValue(
                name = name,
                mapping = mapping,
                reverseMapping = reverseMapping,
                typeConversion = typeConversion
            )
        }
    }

    // builder

    fun buildLensDefinition(from: String, to: String) = Lens(from, to, lensOps)

    fun buildLensOps() = lensOps.toList()
}