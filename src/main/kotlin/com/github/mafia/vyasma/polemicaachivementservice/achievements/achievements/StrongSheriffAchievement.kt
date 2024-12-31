package com.github.mafia.vyasma.polemicaachivementservice.achievements.achievements

import com.github.mafia.vyasma.polemica.library.model.game.PolemicaGame
import com.github.mafia.vyasma.polemica.library.model.game.PolemicaGameResult
import com.github.mafia.vyasma.polemica.library.model.game.Position
import com.github.mafia.vyasma.polemica.library.model.game.Role
import com.github.mafia.vyasma.polemica.library.utils.getRole
import com.github.mafia.vyasma.polemicaachivementservice.achievements.Achievement
import com.github.mafia.vyasma.polemicaachivementservice.achievements.AchievementCategory

object StrongSheriffAchievement : Achievement {
    override val id = "strongSheriff"
    override val name = "Сильный шериф"
    override val description = "Выиграйте за шерифа"
    override val category = AchievementCategory.RED
    override val levels = listOf(1, 3, 7, 15, 50)
    override fun check(game: PolemicaGame, position: Position): Int = boolToInt(
        game.getRole(position) == Role.SHERIFF &&
            game.result == PolemicaGameResult.RED_WIN
    )
}


