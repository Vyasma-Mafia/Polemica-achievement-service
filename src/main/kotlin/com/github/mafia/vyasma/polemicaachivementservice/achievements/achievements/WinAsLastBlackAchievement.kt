package com.github.mafia.vyasma.polemicaachivementservice.achievements.achievements

import com.github.mafia.vyasma.polemicaachivementservice.achievements.Achievement
import com.github.mafia.vyasma.polemicaachivementservice.achievements.AchievementCategory
import com.github.mafia.vyasma.polemicaachivementservice.model.game.PolemicaGame
import com.github.mafia.vyasma.polemicaachivementservice.model.game.Position
import com.github.mafia.vyasma.polemicaachivementservice.utils.getBlacksOnTable
import com.github.mafia.vyasma.polemicaachivementservice.utils.getRole
import com.github.mafia.vyasma.polemicaachivementservice.utils.isBlack
import com.github.mafia.vyasma.polemicaachivementservice.utils.isBlackWin

object WinAsLastBlackAchievement : Achievement {
    override val id = "winAsLastBlack"
    override val name = "Последний герой"
    override val description = "Выиграйте за дона или мафию, оставшись последним черным за столом"
    override val category = AchievementCategory.BLACK
    override val levels = listOf(1, 2, 5, 15, 30)
    override fun check(game: PolemicaGame, position: Position): Int {
        if (!game.getRole(position).isBlack() || !game.isBlackWin()) {
            return boolToInt(false)
        }
        val blackOnTable = game.getBlacksOnTable()
        return boolToInt(blackOnTable.size == 1 && blackOnTable.contains(position))
    }
}
