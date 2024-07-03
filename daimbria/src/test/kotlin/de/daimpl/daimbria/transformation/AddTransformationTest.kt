package de.daimpl.daimbria.transformation

import com.fasterxml.jackson.databind.node.JsonNodeType
import de.cmdjulian.jdsl.JacksonArrayNodeBuilder.Companion.arr
import de.cmdjulian.jdsl.JacksonObjectNodeBuilder.Companion.obj
import de.daimpl.daimbria.InvalidJsonNodeTypeException
import de.daimpl.daimbria.TransformationEngine
import de.daimpl.daimbria.lensOps
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

class AddTransformationTest {

    private val json = obj { }

    @Test
    fun addOnlyToObjectNode() {
        val addLens = lensOps { add("addedProperty", STRING) }
        shouldThrow<InvalidJsonNodeTypeException> { TransformationEngine.applyMigrations(arr {}, addLens) }
    }

    @ParameterizedTest
    @MethodSource("supportedPrimitiveTypes")
    // array and object types are different handled, therefore just testing String, Boolean, Number here
    fun addNullPrimitiveTypes(type: JsonNodeType) {
        val addLens = lensOps { add("addedProperty", type, null) }

        val result = TransformationEngine.applyMigrations(json, addLens)["addedProperty"].shouldNotBeNull()

        result.isNull.shouldBeTrue()
    }

    @Test
    fun addString() {
        val addLens = lensOps { add("addedProperty", STRING, "default") }

        val result = TransformationEngine.applyMigrations(json, addLens)["addedProperty"].shouldNotBeNull()

        result.isTextual.shouldBeTrue()
        result.textValue() shouldBe "default"
    }

    @Test
    fun addBoolean() {
        val addLens = lensOps { add("addedProperty", BOOLEAN, false) }

        val result = TransformationEngine.applyMigrations(json, addLens)["addedProperty"].shouldNotBeNull()

        result.isBoolean.shouldBeTrue()
        result.booleanValue().shouldBeFalse()
    }

    @Test
    fun addInt() {
        val addLens = lensOps { add("addedProperty", NUMBER, 42) }

        val result = TransformationEngine.applyMigrations(json, addLens)["addedProperty"].shouldNotBeNull()

        result.isNumber.shouldBeTrue()
        result.isInt.shouldBeTrue()
        result.isLong.shouldBeFalse()
        result.isDouble.shouldBeFalse()
        result.isFloat.shouldBeFalse()
        result.numberValue() shouldBe 42
    }

    @Test
    fun addLong() {
        val addLens = lensOps { add("addedProperty", NUMBER, 42L) }

        val result = TransformationEngine.applyMigrations(json, addLens)["addedProperty"].shouldNotBeNull()

        result.isNumber.shouldBeTrue()
        result.isInt.shouldBeFalse()
        result.isLong.shouldBeTrue()
        result.isDouble.shouldBeFalse()
        result.isFloat.shouldBeFalse()
        result.numberValue() shouldBe 42L
    }

    @Test
    fun addDouble() {
        val addLens = lensOps { add("addedProperty", NUMBER, 42.42) }

        val result = TransformationEngine.applyMigrations(json, addLens)["addedProperty"].shouldNotBeNull()

        result.isNumber.shouldBeTrue()
        result.isInt.shouldBeFalse()
        result.isLong.shouldBeFalse()
        result.isDouble.shouldBeTrue()
        result.isFloat.shouldBeFalse()
        result.numberValue() shouldBe 42.42
    }

    @Test
    fun addFloat() {
        val addLens = lensOps { add("addedProperty", NUMBER, 42.42f) }

        val result = TransformationEngine.applyMigrations(json, addLens)["addedProperty"].shouldNotBeNull()

        result.isNumber.shouldBeTrue()
        result.isInt.shouldBeFalse()
        result.isLong.shouldBeFalse()
        result.isDouble.shouldBeFalse()
        result.isFloat.shouldBeTrue()
        result.numberValue() shouldBe 42.42f
    }

    @Test
    fun addObject() {
        val addLens = lensOps { add("addedProperty", OBJECT) }

        val result = TransformationEngine.applyMigrations(json, addLens)["addedProperty"].shouldNotBeNull()

        result.isObject.shouldBeTrue()
    }

    @Test
    fun addArray() {
        val addLens = lensOps { add("addedProperty", ARRAY) }

        val result = TransformationEngine.applyMigrations(json, addLens)["addedProperty"].shouldNotBeNull()

        result.isArray.shouldBeTrue()
    }

    companion object {
        @JvmStatic
        fun supportedPrimitiveTypes() = listOf(
            JsonNodeType.BOOLEAN,
            JsonNodeType.NUMBER,
            JsonNodeType.STRING
        )
    }
}