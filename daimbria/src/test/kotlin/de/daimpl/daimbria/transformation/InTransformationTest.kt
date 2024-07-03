package de.daimpl.daimbria.transformation

import de.cmdjulian.jdsl.JacksonArrayNodeBuilder.Companion.arr
import de.cmdjulian.jdsl.JacksonObjectNodeBuilder.Companion.obj
import de.daimpl.daimbria.InvalidJsonNodeTypeException
import de.daimpl.daimbria.JsonNodeExtensions.asArrayNode
import de.daimpl.daimbria.TransformationEngine
import de.daimpl.daimbria.lensOps
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class InTransformationTest {

    private val json = obj {
        put("in", obj {
            put("field", "value")
            put("name", "Gustav")
            put("array", arr {
                add(42)
                add("element")
            })
            put("plunge", 42)
            put("object", obj {
                put("field", "value")
                put("hoist", true)
            })
        })
    }

    @Test
    fun inOpOnlyInObjectNode() {
        val inLens = lensOps { lensIn("field") { } }
        shouldThrow<InvalidJsonNodeTypeException> { TransformationEngine.applyMigrations(arr {}, inLens) }
    }

    @Test
    fun addString() {
        val addLens = lensOps {
            lensIn("in") {
                add("addedProperty", STRING, "default")
            }
        }

        val result = TransformationEngine.applyMigrations(json, addLens)["in"]["addedProperty"].shouldNotBeNull()

        result.isTextual.shouldBeTrue()
        result.textValue() shouldBe "default"
    }

    @Test
    fun remove() {
        val removeLens = lensOps {
            lensIn("in") {
                remove("field", STRING)
            }
        }

        val result = TransformationEngine.applyMigrations(json, removeLens)["in"].shouldNotBeNull()

        result["field"].shouldBeNull()
    }

    @Test
    fun renameProperty() {
        val renameLens = lensOps {
            lensIn("in") {
                rename("field", "property")
            }
        }

        val result = TransformationEngine.applyMigrations(json, renameLens)["in"]

        result["field"].shouldBeNull()
        result["property"].shouldNotBeNull().textValue() shouldBe "value"
    }

    @Test
    fun copyField() {
        val copyLens = lensOps {
            lensIn("in") {
                copy("field", "platz")
            }
        }

        val result = TransformationEngine.applyMigrations(json, copyLens)["in"].shouldNotBeNull()

        result["field"].shouldNotBeNull()
        result["platz"].shouldNotBeNull()
        result["platz"] shouldBeEqual result["field"]
    }

    @Test
    fun headArray() {
        val headLens = lensOps {
            lensIn("in") {
                head("array")
            }
        }

        val result = TransformationEngine.applyMigrations(json, headLens)["in"].shouldNotBeNull()

        result["array"].isArray.shouldBeFalse()
        result["array"].isNumber.shouldBeTrue()
        result["array"].numberValue() shouldBe 42
    }

    @Test
    fun wrapProperty() {
        val wrapLens = lensOps {
            lensIn("in") {
                wrap("field")
            }
        }

        val result = TransformationEngine.applyMigrations(json, wrapLens)["in"].shouldNotBeNull()

        result["field"].isArray.shouldBeTrue()
        result["field"].asArrayNode().first().textValue() shouldBe "value"
    }

    @Test
    fun hoistSimpleProperty() {
        val hoistLens = lensOps {
            lensIn("in") {
                hoist("hoist", "object")
            }
        }

        val result = TransformationEngine.applyMigrations(json, hoistLens)["in"]

        result["hoist"].shouldNotBeNull().booleanValue() shouldBe true
        result["object"].shouldNotBeNull()["hoist"].shouldBeNull()
    }

    @Test
    fun simplePlunge() {
        val plungeLens = lensOps {
            lensIn("in") {
                plunge("plunge", "object")
            }
        }

        val result = TransformationEngine.applyMigrations(json, plungeLens)["in"].shouldNotBeNull()

        result["plunge"].shouldBeNull()
        result["object"].shouldNotBeNull()["plunge"].shouldNotBeNull().numberValue() shouldBe 42
    }
}