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

class HoistTransformationTest {

    private val json = obj {
        put("object", obj {
            put("field", "value")
        })
        put("array", arr {
            add("value")
        })
    }

    @Test
    fun hoistOnlyInObject() {
        val hoistLens = lensOps { hoist("field", "object") }
        shouldThrow<InvalidJsonNodeTypeException> { TransformationEngine.applyMigrations(arr {}, hoistLens) }
    }

    @Test
    fun hoistSimpleProperty() {
        val hoistLens = lensOps { hoist("field", "object") }

        val result = TransformationEngine.applyMigrations(json, hoistLens)

        result["field"].shouldNotBeNull().textValue() shouldBe "value"
        result["object"].shouldNotBeNull()["field"].shouldBeNull()
    }

    @Test
    fun hoistFromArrayThrows() {
        val hoistLens = lensOps { hoist("field", "array") }

        shouldThrow<InvalidJsonNodeTypeException> { TransformationEngine.applyMigrations(json, hoistLens) }
    }
}