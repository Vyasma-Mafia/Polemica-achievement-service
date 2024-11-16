package com.github.mafia.vyasma.polemicaachivementservice.achievements

import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.github.mafia.vyasma.polemicaachivementservice.utils.enums.StringEnum
import com.github.mafia.vyasma.polemicaachivementservice.utils.enums.StringEnumDeserializer
import com.github.mafia.vyasma.polemicaachivementservice.utils.enums.StringEnumSerializer

@JsonSerialize(using = StringEnumSerializer::class)
@JsonDeserialize(using = AchievementCategoryDeserializer::class)
enum class AchievementCategory(override val value: String) : StringEnum {
    COMMON("common"),
    RED("red"),
    BLACK("black")
}

class AchievementCategoryDeserializer :
    StringEnumDeserializer<AchievementCategory>(AchievementCategory.entries.toTypedArray())
