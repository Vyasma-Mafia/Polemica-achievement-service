package com.github.mafia.vyasma.polemicaachivementservice.achievements.achievements

import com.github.mafia.vyasma.polemicaachivementservice.achievements.Achievement
import com.github.mafia.vyasma.polemicaachivementservice.model.game.PolemicaGame
import com.github.mafia.vyasma.polemicaachivementservice.utils.getPositionRole
import com.github.mafia.vyasma.polemicaachivementservice.utils.isRed
import com.github.mafia.vyasma.polemicaachivementservice.utils.isRedWin
import com.github.mafia.vyasma.polemicaachivementservice.utils.playersOnTable

object WinAsRedInLastAchievement : Achievement {
    override val id = "winAsRedInLast"
    override val name = "Везунчик"
    override val description = "Выиграйте за красного в угадайке"
    override val levels = listOf(1, 2, 5, 15, 30)
    override fun check(game: PolemicaGame, position: Int): Int {
        if (!game.getPositionRole(position).isRed() || !game.isRedWin()) {
            return boolToInt(false)
        }
        val onTable = game.playersOnTable()
        return boolToInt(onTable.size == 2 && onTable.contains(position))
    }
}
