package com.github.mafia.vyasma.polemicaachivementservice.achievements.achievements

import com.github.mafia.vyasma.polemica.library.model.game.PolemicaGame
import com.github.mafia.vyasma.polemica.library.model.game.Position
import com.github.mafia.vyasma.polemica.library.utils.getRole
import com.github.mafia.vyasma.polemica.library.utils.isBlack
import com.github.mafia.vyasma.polemicaachivementservice.achievements.Achievement
import com.github.mafia.vyasma.polemicaachivementservice.achievements.AchievementCategory

object PartialMafsGuessAchievement : Achievement {
    override val id = "partialMafsGuess"
    override val name = "Два из трех"
    override val description = "Оставьте в лучший ход 2 мафии"
    override val category = AchievementCategory.RED
    override val levels = listOf(1, 2, 5, 12, 30)
    override fun check(game: PolemicaGame, position: Position): Int {
        val lastWordMafs =
            game.players?.find { it.position == position }?.guess?.mafs?.map { game.getRole(it) }
        return boolToInt(lastWordMafs?.filter { it.isBlack() }?.size == 2)
    }
}


