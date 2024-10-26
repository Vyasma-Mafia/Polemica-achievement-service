package com.github.mafia.vyasma.polemicaachivementservice.achievements

import com.github.mafia.vyasma.polemicaachivementservice.model.game.PolemicaGame
import com.github.mafia.vyasma.polemicaachivementservice.model.game.PolemicaGameResult
import com.github.mafia.vyasma.polemicaachivementservice.model.game.Role

object WinAsDonAchievement : Achievement {
    override val name = "Дон победитель"
    override val description = "Выиграйте за дона"
    override val id = "winAsDon"
    override val levels = listOf(1, 5, 10, 50)
    override fun check(game: PolemicaGame, position: Int): Int {
        return if (game.players.find { it.position == position }?.role == Role.DON &&
            game.result == PolemicaGameResult.BLACK_WIN
        ) {
            1
        } else {
            0
        }
    }
}
