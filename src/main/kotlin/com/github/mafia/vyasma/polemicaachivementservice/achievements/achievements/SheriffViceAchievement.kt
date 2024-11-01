package com.github.mafia.vyasma.polemicaachivementservice.achievements.achievements

import com.github.mafia.vyasma.polemicaachivementservice.achievements.Achievement
import com.github.mafia.vyasma.polemicaachivementservice.model.game.PolemicaGame
import com.github.mafia.vyasma.polemicaachivementservice.model.game.Role
import com.github.mafia.vyasma.polemicaachivementservice.utils.getPositionRole
import com.github.mafia.vyasma.polemicaachivementservice.utils.isRed

object SheriffViceAchievement : Achievement {
    override val id = "sheriffVice"
    override val name = "Это мой шериф!"
    override val description = "Оставьте руль шерифу, будучи красным"
    override val levels = listOf(1, 3, 7, 15, 30)
    override fun check(game: PolemicaGame, position: Int): Int {
        val lastWordVice =
            game.players.find { it.position == position }?.guess?.vice
        return boolToInt(lastWordVice?.let { game.getPositionRole(it) } == Role.SHERIFF
            && game.getPositionRole(position).isRed()
        )
    }
}


