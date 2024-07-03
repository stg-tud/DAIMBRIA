package de.daimpl.daimbria.transformation

import de.cmdjulian.jdsl.JacksonArrayNodeBuilder.Companion.arr
import de.cmdjulian.jdsl.JacksonObjectNodeBuilder.Companion.obj
import de.daimpl.daimbria.InvalidJsonNodeTypeException
import de.daimpl.daimbria.TransformationEngine
import de.daimpl.daimbria.lensOps
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.nulls.shouldBeNull
import org.junit.jupiter.api.Test

class RemoveTransformationTest {

    private val json = obj {
        put("string", "stringValue")
    }

    @Test
    fun removeOnlyFromObjectNode() {
        val removeLens = lensOps { remove("addedProperty", STRING) }
        shouldThrow<InvalidJsonNodeTypeException> { TransformationEngine.applyMigrations(arr {}, removeLens) }
    }

    @Test
    fun remove() {
        val removeLens = lensOps { remove("string", STRING) }

        val result = TransformationEngine.applyMigrations(json, removeLens)

        result["string"].shouldBeNull()
    }
}