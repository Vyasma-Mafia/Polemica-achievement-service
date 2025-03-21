package com.github.mafia.vyasma.polemicaachivementservice.achievements.achievements

import com.github.mafia.vyasma.polemica.library.model.game.PolemicaGame
import com.github.mafia.vyasma.polemica.library.model.game.Position
import com.github.mafia.vyasma.polemicaachivementservice.achievements.Achievement
import com.github.mafia.vyasma.polemicaachivementservice.achievements.AchievementCategory

object ManyVicesAchievement : Achievement {
    override val id = "manyVices"
    override val name = "Капитан Джек Воробей"
    override val description = "Получите два руля или больше"
    override val category = AchievementCategory.COMMON
    override val levels = listOf(1, 2, 5, 12, 30)
    override fun check(game: PolemicaGame, position: Position): Int {
        return boolToInt((game.players?.count { it.guess?.vice == position } ?: 0) >= 2)
    }
}


