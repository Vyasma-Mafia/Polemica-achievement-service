package com.github.mafia.vyasma.polemicaachivementservice.achievements.achievements

import com.github.mafia.vyasma.polemicaachivementservice.achievements.Achievement
import com.github.mafia.vyasma.polemicaachivementservice.model.game.PolemicaGame
import com.github.mafia.vyasma.polemicaachivementservice.model.game.Position
import com.github.mafia.vyasma.polemicaachivementservice.utils.check
import com.github.mafia.vyasma.polemicaachivementservice.utils.getRole
import com.github.mafia.vyasma.polemicaachivementservice.utils.isBlack

object FullMafsAchievement : Achievement {
    override val id = "fullMafs"
    override val name = "Полный ЛХ"
    override val description = "Оставьте в лучший ход 3 мафии"
    override val levels = listOf(1, 2, 3, 5, 10)
    override fun check(game: PolemicaGame, position: Position): Int = game.check {
        val lastWordMafs =
            position.guess()?.mafs?.map { game.getRole(it) }
        return boolToInt(lastWordMafs?.filter { it.isBlack() }?.size == 3)
    }
}


