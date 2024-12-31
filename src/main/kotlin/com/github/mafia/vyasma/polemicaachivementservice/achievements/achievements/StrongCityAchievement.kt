package com.github.mafia.vyasma.polemicaachivementservice.achievements.achievements

import com.github.mafia.vyasma.polemica.library.model.game.PolemicaGame
import com.github.mafia.vyasma.polemica.library.model.game.PolemicaGameResult
import com.github.mafia.vyasma.polemica.library.model.game.Position
import com.github.mafia.vyasma.polemica.library.model.game.Role
import com.github.mafia.vyasma.polemica.library.utils.getRealComKiller
import com.github.mafia.vyasma.polemica.library.utils.getRole
import com.github.mafia.vyasma.polemicaachivementservice.achievements.Achievement
import com.github.mafia.vyasma.polemicaachivementservice.achievements.AchievementCategory

object StrongCityAchievement : Achievement {
    override val id = "strongCity"
    override val name = "Сильному городу шериф не нужен"
    override val description = "Выиграйте красным, когда шериф умер в первую ночь"
    override val category = AchievementCategory.RED
    override val levels = listOf(1, 2, 5, 15, 30)
    override fun check(game: PolemicaGame, position: Position): Int =
        boolToInt(game.getRealComKiller() != null && game.getRole(position) == Role.PEACE && game.result == PolemicaGameResult.RED_WIN)
}


