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

class RenameTransformationTest {

    private val json = obj { put("field", "value") }

    @Test
    fun renameOnlyInObjectNode() {
        val renameLens = lensOps { rename("field", "something") }
        shouldThrow<InvalidJsonNodeTypeException> { TransformationEngine.applyMigrations(arr {}, renameLens) }
    }

    @Test
    fun renameProperty() {
        val renameLens = lensOps { rename("field", "property") }

        val result = TransformationEngine.applyMigrations(json, renameLens)

        result["field"].shouldBeNull()
        result["property"].shouldNotBeNull().textValue() shouldBe "value"
    }
}