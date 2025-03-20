package com.github.mafia.vyasma.polemicaachivementservice.achievements.achievements

import com.github.mafia.vyasma.polemica.library.model.game.PolemicaGame
import com.github.mafia.vyasma.polemica.library.model.game.Position
import com.github.mafia.vyasma.polemica.library.model.game.Role
import com.github.mafia.vyasma.polemica.library.utils.getRole
import com.github.mafia.vyasma.polemica.library.utils.isRed
import com.github.mafia.vyasma.polemicaachivementservice.achievements.Achievement
import com.github.mafia.vyasma.polemicaachivementservice.achievements.AchievementCategory

object SheriffViceAchievement : Achievement {
    override val id = "sheriffVice"
    override val name = "Это мой шериф!"
    override val description = "Оставьте руль шерифу, будучи красным"
    override val category = AchievementCategory.RED
    override val levels = listOf(1, 2, 5, 12, 30)
    override fun check(game: PolemicaGame, position: Position): Int {
        val lastWordVice =
            game.players?.find { it.position == position }?.guess?.vice
        return boolToInt(lastWordVice?.let { game.getRole(it) } == Role.SHERIFF
            && game.getRole(position).isRed()
        )
    }
}


