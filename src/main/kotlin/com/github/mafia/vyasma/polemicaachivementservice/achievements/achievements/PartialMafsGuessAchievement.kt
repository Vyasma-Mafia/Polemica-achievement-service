package com.github.mafia.vyasma.polemicaachivementservice.achievements.achievements

import com.github.mafia.vyasma.polemicaachivementservice.achievements.Achievement
import com.github.mafia.vyasma.polemicaachivementservice.model.game.PolemicaGame
import com.github.mafia.vyasma.polemicaachivementservice.utils.getPositionRole
import com.github.mafia.vyasma.polemicaachivementservice.utils.isBlack

object PartialMafsGuessAchievement : Achievement {
    override val id = "partialMafsGuess"
    override val name = "Два из трех"
    override val description = "Оставьте в лучший ход 2 мафии"
    override val levels = listOf(1, 3, 7, 15, 30)
    override fun check(game: PolemicaGame, position: Int): Int {
        val lastWordMafs =
            game.players.find { it.position == position }?.guess?.mafs?.map { game.getPositionRole(it) }
        return boolToInt(lastWordMafs?.filter { it.isBlack() }?.size == 2)
    }
}


