package com.github.mafia.vyasma.polemicaachivementservice.achievements.achievements

import com.github.mafia.vyasma.polemicaachivementservice.achievements.Achievement
import com.github.mafia.vyasma.polemicaachivementservice.model.game.PolemicaGame
import com.github.mafia.vyasma.polemicaachivementservice.utils.getBlacksOnTable
import com.github.mafia.vyasma.polemicaachivementservice.utils.getPositionRole
import com.github.mafia.vyasma.polemicaachivementservice.utils.isBlack
import com.github.mafia.vyasma.polemicaachivementservice.utils.isBlackWin

object WinAsLastBlackAchievement : Achievement {
    override val id = "winAsLastBlack"
    override val name = "Последний герой"
    override val description = "Выиграйте за дона или мафию, оставшись последним черным за столом"
    override val levels = listOf(1, 3, 5, 10, 25)
    override fun check(game: PolemicaGame, position: Int): Int {
        if (!game.getPositionRole(position).isBlack() || !game.isBlackWin()) {
            return boolToInt(false)
        }
        val blackOnTable = game.getBlacksOnTable()
        return boolToInt(blackOnTable.size == 1 && blackOnTable.contains(position))
    }
}
