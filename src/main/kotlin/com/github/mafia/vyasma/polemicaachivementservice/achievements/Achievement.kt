package com.github.mafia.vyasma.polemicaachivementservice.achievements

import com.github.mafia.vyasma.polemicaachivementservice.model.game.PolemicaGame

interface Achievement {
    val id: String
    val name: String
    val description: String
    val levels: List<Int>
    val order: Int
        get() = 100

    fun check(game: PolemicaGame, position: Int): Int

    fun boolToInt(value: Boolean): Int = if (value) 1 else 0
}
