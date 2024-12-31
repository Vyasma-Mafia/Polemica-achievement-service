package com.github.mafia.vyasma.polemicaachivementservice.achievements.achievements

import com.github.mafia.vyasma.polemica.library.model.game.PolemicaGame
import com.github.mafia.vyasma.polemica.library.model.game.Position
import com.github.mafia.vyasma.polemicaachivementservice.achievements.Achievement

object SamuraiPathAchievement : Achievement {
    override val id = "samuraiPath"
    override val name = "Путь самурая"
    override val description = "Окончите игру в спортивную мафию"
    override val levels = listOf(1, 10, 30, 100, 500)
    override val order: Int
        get() = 0

    override fun check(game: PolemicaGame, position: Position): Int = 1
}


