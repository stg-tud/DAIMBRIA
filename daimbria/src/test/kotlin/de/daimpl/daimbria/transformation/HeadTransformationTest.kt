package de.daimpl.daimbria.transformation

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

class HeadTransformationTest {

    private val json = obj {
        put("array", arr {
            add(42)
            add("element")
        })
        put("object", obj {
            put("field", "value")
        })
        put("String", "hello world")
    }

    @Test
    fun headOnlyInObject() {
        val headLens = lensOps { head("array") }
        shouldThrow<InvalidJsonNodeTypeException> { TransformationEngine.applyMigrations(arr {}, headLens) }
    }

    @Test
    fun headArray() {
        val headLens = lensOps { head("array") }

        val result = TransformationEngine.applyMigrations(json, headLens).shouldNotBeNull()

        result["array"].isArray.shouldBeFalse()
        result["array"].isNumber.shouldBeTrue()
        result["array"].numberValue() shouldBe 42
    }

    @Test
    fun headObjectThrows() {
        val headLens = lensOps { head("object") }
        shouldThrow<InvalidJsonNodeTypeException> { TransformationEngine.applyMigrations(json, headLens) }
    }

    @Test
    fun headPrimitiveThrows() {
        val headLens = lensOps { head("String") }
        shouldThrow<InvalidJsonNodeTypeException> { TransformationEngine.applyMigrations(json, headLens) }
    }
}