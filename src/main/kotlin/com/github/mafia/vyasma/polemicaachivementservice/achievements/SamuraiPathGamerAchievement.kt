package com.github.mafia.vyasma.polemicaachivementservice.achievements

import com.github.mafia.vyasma.polemicaachivementservice.model.game.PolemicaGame

object SamuraiPathGamerAchievement : Achievement {
    override val id = "SamuraiPath"
    override val name = "Путь самурая"
    override val description = "Окончите игру в спортивную мафию"
    override val levels = listOf(1, 20, 100, 250, 1000)
    override val order: Int
        get() = 0

    override fun check(game: PolemicaGame, position: Int): Int = 1
}


