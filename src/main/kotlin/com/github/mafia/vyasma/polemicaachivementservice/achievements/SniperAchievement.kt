package com.github.mafia.vyasma.polemicaachivementservice.achievements

import com.github.mafia.vyasma.polemicaachivementservice.model.game.PolemicaGame
import com.github.mafia.vyasma.polemicaachivementservice.utils.getRealComKiller

object SniperAchievement : Achievement {
    override val id = "sniper"
    override val name = "Снайпер"
    override val description = "Отстрелите шерифа в первую ночь"
    override val levels = listOf(1, 2, 3, 5, 10)
    override fun check(game: PolemicaGame, position: Int): Int = boolToInt(game.getRealComKiller() == position)
}


