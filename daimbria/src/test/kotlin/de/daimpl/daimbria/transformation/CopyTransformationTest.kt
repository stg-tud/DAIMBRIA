package de.daimpl.daimbria.transformation

import de.cmdjulian.jdsl.JacksonArrayNodeBuilder.Companion.arr
import de.cmdjulian.jdsl.JacksonObjectNodeBuilder.Companion.obj
import de.daimpl.daimbria.InvalidJsonNodeTypeException
import de.daimpl.daimbria.TransformationEngine
import de.daimpl.daimbria.lensOps
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class CopyTransformationTest {

    private val json = obj {
        put("field", "value")
        put("name", "Gustav")
    }

    @Test
    fun copyOnlyInObject() {
        val copyLens = lensOps { copy("field", "platz") }
        shouldThrow<InvalidJsonNodeTypeException> { TransformationEngine.applyMigrations(arr {}, copyLens) }
    }

    @Test
    fun copyField() {
        val copyLens = lensOps { copy("field", "platz") }

        val result = TransformationEngine.applyMigrations(json, copyLens).shouldNotBeNull()

        result["field"].shouldNotBeNull()
        result["platz"].shouldNotBeNull()
        result["platz"] shouldBeEqual result["field"]
    }

    @Test
    fun existingFieldNotOverwritten() {
        val copyLens = lensOps { copy("field", "name") }

        val result = TransformationEngine.applyMigrations(json, copyLens)

        result["name"].textValue() shouldBe "Gustav"
    }
}