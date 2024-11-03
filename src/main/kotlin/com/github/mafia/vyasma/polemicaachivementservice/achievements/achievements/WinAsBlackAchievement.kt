package com.github.mafia.vyasma.polemicaachivementservice.achievements.achievements

import com.github.mafia.vyasma.polemicaachivementservice.achievements.Achievement
import com.github.mafia.vyasma.polemicaachivementservice.model.game.PolemicaGame
import com.github.mafia.vyasma.polemicaachivementservice.model.game.PolemicaGameResult
import com.github.mafia.vyasma.polemicaachivementservice.model.game.Role
import com.github.mafia.vyasma.polemicaachivementservice.utils.getPositionRole

object WinAsBlackAchievement : Achievement {
    override val id = "winAsBlack"
    override val name = "Я не боюсь черную карту"
    override val description = "Выиграйте за дона или мафию"
    override val levels = listOf(1, 3, 10, 30, 100)
    override fun check(game: PolemicaGame, position: Int): Int = boolToInt(
        listOf(Role.DON, Role.MAFIA).contains(game.getPositionRole(position)) &&
            game.result == PolemicaGameResult.BLACK_WIN
    )
}
