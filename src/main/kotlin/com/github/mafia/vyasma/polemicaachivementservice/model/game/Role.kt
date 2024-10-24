package com.github.mafia.vyasma.polemicaachivementservice.model.game

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize

@JsonSerialize(using = RoleSerializer::class)
@JsonDeserialize(using = RoleDeserializer::class)
enum class Role(val value: Int) {
    DON(0),
    MAFIA(1),
    PEACE(2),
    SHERIFF(3)
}

class RoleSerializer : JsonSerializer<Role>() {
    override fun serialize(role: Role, gen: JsonGenerator, serializers: SerializerProvider) {
        gen.writeNumber(role.value)
    }
}

class RoleDeserializer : JsonDeserializer<Role>() {
    override fun deserialize(parser: JsonParser, ctxt: DeserializationContext): Role {
        val intValue = parser.intValue
        return Role.entries.find { it.value == intValue }
            ?: throw IllegalArgumentException("Invalid value for Role: $intValue")
    }
}
