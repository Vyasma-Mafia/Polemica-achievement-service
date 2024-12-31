package com.github.mafia.vyasma.polemicaachivementservice.achievements.achievements

import com.github.mafia.vyasma.polemica.library.model.game.PolemicaGame
import com.github.mafia.vyasma.polemica.library.model.game.Position
import com.github.mafia.vyasma.polemica.library.utils.getRole
import com.github.mafia.vyasma.polemica.library.utils.isRed
import com.github.mafia.vyasma.polemica.library.utils.isRedWin
import com.github.mafia.vyasma.polemica.library.utils.playersOnTable
import com.github.mafia.vyasma.polemicaachivementservice.achievements.Achievement
import com.github.mafia.vyasma.polemicaachivementservice.achievements.AchievementCategory

object WinAsRedInLastAchievement : Achievement {
    override val id = "winAsRedInLast"
    override val name = "Везунчик"
    override val description = "Выиграйте за красного в угадайке"
    override val category = AchievementCategory.RED
    override val levels = listOf(1, 2, 5, 15, 30)
    override fun check(game: PolemicaGame, position: Position): Int {
        if (!game.getRole(position).isRed() || !game.isRedWin()) {
            return boolToInt(false)
        }
        val onTable = game.playersOnTable()
        return boolToInt(onTable.size == 2 && onTable.contains(position))
    }
}
