package com.github.mafia.vyasma.polemicaachivementservice.achievements

import com.github.mafia.vyasma.polemica.library.model.game.PolemicaGame
import com.github.mafia.vyasma.polemica.library.model.game.Position

interface Achievement {
    val id: String
    val name: String
    val description: String
    val levels: List<Int>
    val order: Int
        get() = 100
    val category: AchievementCategory
        get() = AchievementCategory.COMMON

    fun check(game: PolemicaGame, position: Position): Int

    fun boolToInt(value: Boolean): Int = if (value) 1 else 0
}
