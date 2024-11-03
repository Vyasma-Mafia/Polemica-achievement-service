package com.github.mafia.vyasma.polemicaachivementservice.achievements.achievements

import com.github.mafia.vyasma.polemicaachivementservice.achievements.Achievement
import com.github.mafia.vyasma.polemicaachivementservice.model.game.PolemicaGame
import com.github.mafia.vyasma.polemicaachivementservice.utils.getBlacksOnTable
import com.github.mafia.vyasma.polemicaachivementservice.utils.getPositionRole
import com.github.mafia.vyasma.polemicaachivementservice.utils.isBlack
import com.github.mafia.vyasma.polemicaachivementservice.utils.isBlackWin

object WinThreeToThreeLastAchievement : Achievement {
    override val id = "winThreeToThree"
    override val name = "Баланс не нужен"
    override val description = "Выиграйте за черного 3 в 3"
    override val levels = listOf(1, 2, 5, 20, 50)
    override fun check(game: PolemicaGame, position: Int): Int {
        if (!game.getPositionRole(position).isBlack() || !game.isBlackWin()) {
            return boolToInt(false)
        }
        val blackOnTable = game.getBlacksOnTable()
        return boolToInt(blackOnTable.size == 3)
    }
}
