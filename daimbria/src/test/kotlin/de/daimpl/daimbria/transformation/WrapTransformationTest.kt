package de.daimpl.daimbria.transformation

import de.cmdjulian.jdsl.JacksonArrayNodeBuilder.Companion.arr
import de.cmdjulian.jdsl.JacksonObjectNodeBuilder.Companion.obj
import de.daimpl.daimbria.InvalidJsonNodeTypeException
import de.daimpl.daimbria.JsonNodeExtensions.asArrayNode
import de.daimpl.daimbria.TransformationEngine
import de.daimpl.daimbria.lensOps
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class WrapTransformationTest {

    val json = obj {
        put("field", "value")
        put("object", obj {
            put("name", "Gutrhuhn")
        })
    }

    @Test
    fun wrapOnlyInObject() {
        val wrapLens = lensOps { wrap("array") }
        shouldThrow<InvalidJsonNodeTypeException> { TransformationEngine.applyMigrations(arr {}, wrapLens) }
    }

    @Test
    fun wrapProperty() {
        val wrapLens = lensOps { wrap("field") }

        val result = TransformationEngine.applyMigrations(json, wrapLens).shouldNotBeNull()

        result["field"].isArray.shouldBeTrue()
        result["field"].asArrayNode().first().textValue() shouldBe "value"
    }

    @Test
    fun wrapComplexProperty() {
        val wrapLens = lensOps { wrap("object") }

        val result = TransformationEngine.applyMigrations(json, wrapLens).shouldNotBeNull()

        result["object"].isArray.shouldBeTrue()
        result["object"].asArrayNode().first()["name"].textValue() shouldBe "Gutrhuhn"
    }
}