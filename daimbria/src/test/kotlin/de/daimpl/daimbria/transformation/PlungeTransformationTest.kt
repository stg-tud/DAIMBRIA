package de.daimpl.daimbria.transformation

import de.cmdjulian.jdsl.JacksonArrayNodeBuilder.Companion.arr
import de.cmdjulian.jdsl.JacksonObjectNodeBuilder.Companion.obj
import de.daimpl.daimbria.InvalidJsonNodeTypeException
import de.daimpl.daimbria.TransformationEngine
import de.daimpl.daimbria.lensOps
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class PlungeTransformationTest {

    private val json = obj {
        put("field", "value")
        put("object", obj { })
        put("array", arr {})
    }

    @Test
    fun plungeOnlyInObject() {
        val plungeLens = lensOps { plunge("field", "object") }
        shouldThrow<InvalidJsonNodeTypeException> { TransformationEngine.applyMigrations(arr {}, plungeLens) }
    }

    @Test
    fun simplePlunge() {
        val plungeLens = lensOps { plunge("field", "object") }

        val result = TransformationEngine.applyMigrations(json, plungeLens).shouldNotBeNull()

        result["field"].shouldBeNull()
        result["object"].shouldNotBeNull()["field"].shouldNotBeNull().textValue() shouldBe "value"
    }

    @Test
    fun plungeInArrayThrows() {
        val plungeLens = lensOps { plunge("field", "array") }

        shouldThrow<InvalidJsonNodeTypeException> { TransformationEngine.applyMigrations(json, plungeLens) }
    }
}