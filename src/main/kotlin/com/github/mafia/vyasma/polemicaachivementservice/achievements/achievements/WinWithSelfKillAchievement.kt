package com.github.mafia.vyasma.polemicaachivementservice.achievements.achievements

import com.github.mafia.vyasma.polemica.library.model.game.PolemicaGame
import com.github.mafia.vyasma.polemica.library.model.game.Position
import com.github.mafia.vyasma.polemica.library.utils.KickReason
import com.github.mafia.vyasma.polemica.library.utils.check
import com.github.mafia.vyasma.polemica.library.utils.getKickedFromTable
import com.github.mafia.vyasma.polemica.library.utils.isBlack
import com.github.mafia.vyasma.polemica.library.utils.isBlackWin
import com.github.mafia.vyasma.polemicaachivementservice.achievements.Achievement
import com.github.mafia.vyasma.polemicaachivementservice.achievements.AchievementCategory

object WinWithSelfKillAchievement : Achievement {
    override val id = "winWithSelfKill"
    override val name = "Твоя смерть была не напрасна"
    override val description = "Выиграйте игру за черных, когда один из черных ушел самострелом"
    override val category = AchievementCategory.BLACK
    override val levels = listOf(1, 2, 5, 12, 30)
    override fun check(game: PolemicaGame, position: Position): Int = game.check {
        assert { position.role().isBlack() }
        assert { game.isBlackWin() }
        val kicked = game.getKickedFromTable()
        return boolToInt(
            kicked.any { it.position.role().isBlack() && it.reason == KickReason.KILL }
        )
    }
}


