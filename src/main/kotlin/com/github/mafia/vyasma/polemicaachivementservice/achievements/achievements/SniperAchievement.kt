package com.github.mafia.vyasma.polemicaachivementservice.achievements.achievements

import com.github.mafia.vyasma.polemica.library.model.game.PolemicaGame
import com.github.mafia.vyasma.polemica.library.model.game.Position
import com.github.mafia.vyasma.polemica.library.utils.getRealComKiller
import com.github.mafia.vyasma.polemicaachivementservice.achievements.Achievement
import com.github.mafia.vyasma.polemicaachivementservice.achievements.AchievementCategory

object SniperAchievement : Achievement {
    override val id = "sniper"
    override val name = "Снайпер"
    override val description = "Отстрелите шерифа в первую ночь"
    override val category = AchievementCategory.BLACK
    override val levels = listOf(1, 2, 4, 7, 15)
    override fun check(game: PolemicaGame, position: Position): Int = boolToInt(game.getRealComKiller() == position)
}


