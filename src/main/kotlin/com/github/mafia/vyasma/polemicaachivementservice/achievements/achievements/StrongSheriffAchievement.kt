package com.github.mafia.vyasma.polemicaachivementservice.achievements.achievements

import com.github.mafia.vyasma.polemicaachivementservice.achievements.Achievement
import com.github.mafia.vyasma.polemicaachivementservice.model.game.PolemicaGame
import com.github.mafia.vyasma.polemicaachivementservice.model.game.PolemicaGameResult
import com.github.mafia.vyasma.polemicaachivementservice.model.game.Role
import com.github.mafia.vyasma.polemicaachivementservice.utils.getPositionRole

object StrongSheriffAchievement : Achievement {
    override val id = "strongSheriff"
    override val name = "Сильный шериф"
    override val description = "Выиграйте за шерифа"
    override val levels = listOf(1, 5, 10, 25, 50)
    override fun check(game: PolemicaGame, position: Int): Int = boolToInt(
        game.getPositionRole(position) == Role.SHERIFF &&
            game.result == PolemicaGameResult.RED_WIN
    )
}


