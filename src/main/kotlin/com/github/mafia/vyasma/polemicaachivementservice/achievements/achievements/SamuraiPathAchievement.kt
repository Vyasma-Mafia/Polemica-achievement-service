package com.github.mafia.vyasma.polemicaachivementservice.achievements.achievements

import com.github.mafia.vyasma.polemicaachivementservice.achievements.Achievement
import com.github.mafia.vyasma.polemicaachivementservice.model.game.PolemicaGame

object SamuraiPathAchievement : Achievement {
    override val id = "samuraiPath"
    override val name = "Путь самурая"
    override val description = "Окончите игру в спортивную мафию"
    override val levels = listOf(1, 20, 100, 250, 1000)
    override val order: Int
        get() = 0

    override fun check(game: PolemicaGame, position: Int): Int = 1
}


