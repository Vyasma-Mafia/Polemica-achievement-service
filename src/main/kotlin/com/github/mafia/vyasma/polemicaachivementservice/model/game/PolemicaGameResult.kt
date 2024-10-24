package com.github.mafia.vyasma.polemicaachivementservice.model.game

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider

enum class PolemicaGameResult(
    val value: Int
) {
    RED_WIN(0),
    BLACK_WIN(1)
}

class PolemicaGameResultSerializer : JsonSerializer<PolemicaGameResult>() {
    override fun serialize(role: PolemicaGameResult, gen: JsonGenerator, serializers: SerializerProvider) {
        gen.writeNumber(role.value)
    }
}

class PolemicaGameResultDeserializer : JsonDeserializer<PolemicaGameResult>() {
    override fun deserialize(parser: JsonParser, ctxt: DeserializationContext): PolemicaGameResult {
        val intValue = parser.intValue
        return PolemicaGameResult.entries.find { it.value == intValue }
            ?: throw IllegalArgumentException("Invalid value for PolemicaGameResult: $intValue")
    }
}
