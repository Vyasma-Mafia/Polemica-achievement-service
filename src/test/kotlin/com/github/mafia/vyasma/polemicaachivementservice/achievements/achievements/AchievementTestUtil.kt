package com.github.mafia.vyasma.polemicaachivementservice.achievements.achievements

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.github.mafia.vyasma.polemicaachivementservice.achievements.Achievement
import com.github.mafia.vyasma.polemicaachivementservice.model.game.PolemicaGame
import com.github.mafia.vyasma.polemicaachivementservice.model.game.Position
import org.junit.Assert
import org.springframework.util.ResourceUtils

val MAPPER: ObjectMapper = ObjectMapper().registerKotlinModule().registerModules(JavaTimeModule())

fun testAchievement(achievement: Achievement, gameId: Long, position: Position, expected: Int) {
    val file = ResourceUtils.getFile("classpath:games/$gameId.json")
    val game = MAPPER.readValue<PolemicaGame>(file.readText())
    val res = achievement.check(game, position)
    Assert.assertEquals(expected, res)
}
