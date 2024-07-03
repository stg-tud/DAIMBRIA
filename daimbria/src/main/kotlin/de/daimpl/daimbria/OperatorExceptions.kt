package de.daimpl.daimbria

import com.fasterxml.jackson.databind.node.JsonNodeType
import kotlin.reflect.KClass

class InvalidConversionTypeException(expected: JsonNodeType, actual: KClass<*>, sourceOrTarget: String) :
    IllegalArgumentException("Expected type '$expected' for $sourceOrTarget type but got type '${actual.simpleName}'.")