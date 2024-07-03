package de.daimpl.daimbria.transformation

import de.cmdjulian.jdsl.JacksonArrayNodeBuilder.Companion.arr
import de.cmdjulian.jdsl.JacksonObjectNodeBuilder.Companion.obj
import de.daimpl.daimbria.InvalidJsonNodeTypeException
import de.daimpl.daimbria.TransformationEngine
import de.daimpl.daimbria.lensOps
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class ConvertTransformationTest {

    private val json = obj {
        put("string", "hello world")
        put("int", 42)
        put("float", 42.42)
        put("stringNumber", "13")
    }

    @Test
    fun convertOnlyInObjectNode() {
        val convertLens = lensOps {
            convert("field") {
                STRING mapsTo STRING
                mapping { name: String -> name }
                reverseMapping { name: String -> name }
            }
        }
        shouldThrow<InvalidJsonNodeTypeException> { TransformationEngine.applyMigrations(arr {}, convertLens) }
    }

    @Test
    fun convertStringToInt() {
        val convertLens = lensOps {
            convert("stringNumber") {
                STRING mapsTo NUMBER
                mapping { value: String -> value.toInt() }
                reverseMapping { number: Int -> number.toString() }
            }
        }

        val result = TransformationEngine.applyMigrations(json, convertLens)

        result["stringNumber"].isInt.shouldBeTrue()
        result["stringNumber"].intValue() shouldBe 13
    }

    @Test
    fun convertStringToFloat() {
        val convertLens = lensOps {
            convert("stringNumber") {
                STRING mapsTo NUMBER
                mapping { value: String -> value.toFloat() }
                reverseMapping { number: Float -> number.toString() }
            }
        }

        val result = TransformationEngine.applyMigrations(json, convertLens)

        result["stringNumber"].isFloat.shouldBeTrue()
        result["stringNumber"].floatValue() shouldBe 13f
    }
}