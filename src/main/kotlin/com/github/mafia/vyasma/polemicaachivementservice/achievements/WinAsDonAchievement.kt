package com.github.mafia.vyasma.polemicaachivementservice.achievements

import com.github.mafia.vyasma.polemicaachivementservice.model.game.PolemicaGame
import com.github.mafia.vyasma.polemicaachivementservice.model.game.PolemicaGameResult
import com.github.mafia.vyasma.polemicaachivementservice.model.game.Role
import com.github.mafia.vyasma.polemicaachivementservice.utils.getPositionRole

object WinAsDonAchievement : Achievement {
    override val id = "winAsDon"
    override val name = "Дон победитель"
    override val description = "Выиграйте за дона"
    override val levels = listOf(1, 5, 10, 25, 50)
    override fun check(game: PolemicaGame, position: Int): Int = boolToInt(
        game.getPositionRole(position) == Role.DON &&
            game.result == PolemicaGameResult.BLACK_WIN
    )
}
