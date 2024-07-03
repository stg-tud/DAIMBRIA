package de.daimpl.daimbria.transformation

import de.cmdjulian.jdsl.JacksonArrayNodeBuilder.Companion.arr
import de.cmdjulian.jdsl.JacksonObjectNodeBuilder.Companion.obj
import de.daimpl.daimbria.InvalidJsonNodeTypeException
import de.daimpl.daimbria.JsonNodeExtensions.asArrayNode
import de.daimpl.daimbria.TransformationEngine
import de.daimpl.daimbria.lensOps
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import org.junit.jupiter.api.Test

class MapTransformationTest {

    private val jsonObject = obj {
        put("array", arr {
            obj {
                put("name", "Json")
            }
            obj {
                put("name", "Ada")
            }
        })
    }

    private val jsonArray = arr {
        obj {
            put("pet", "cat")
        }
        obj {
            put("pet", "dog")
        }
    }

    @Test
    fun mapOnlyInArrayNode() {
        val mapLens = lensOps { lensMap { } }
        shouldThrow<InvalidJsonNodeTypeException> { TransformationEngine.applyMigrations(obj {}, mapLens) }
    }

    @Test
    fun inOpWithMap() {
        val mapLens = lensOps {
            lensIn("array") {
                lensMap {
                    rename("name", "nickname")
                }
            }
        }

        val result = TransformationEngine.applyMigrations(jsonObject, mapLens)["array"].asArrayNode()

        result.forEach { node ->
            node["name"].shouldBeNull()
            node["nickname"].shouldNotBeNull()
        }
    }

    @Test
    fun mapInArray() {
        val mapLens = lensOps {
            lensMap {
                rename("pet", "animal")
                add("name", STRING)
            }
        }

        val result = TransformationEngine.applyMigrations(jsonArray, mapLens).asArrayNode()

        result.forEach { node ->
            node["pet"].shouldBeNull()
            node["animal"].shouldNotBeNull()
            node["name"].shouldNotBeNull()
        }
    }
}