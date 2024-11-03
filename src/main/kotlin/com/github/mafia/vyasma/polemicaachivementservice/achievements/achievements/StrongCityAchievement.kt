package com.github.mafia.vyasma.polemicaachivementservice.achievements.achievements

import com.github.mafia.vyasma.polemicaachivementservice.achievements.Achievement
import com.github.mafia.vyasma.polemicaachivementservice.model.game.PolemicaGame
import com.github.mafia.vyasma.polemicaachivementservice.model.game.PolemicaGameResult
import com.github.mafia.vyasma.polemicaachivementservice.model.game.Role
import com.github.mafia.vyasma.polemicaachivementservice.utils.getPositionRole
import com.github.mafia.vyasma.polemicaachivementservice.utils.getRealComKiller

object StrongCityAchievement : Achievement {
    override val id = "strongCity"
    override val name = "Сильному городу шериф не нужен"
    override val description = "Выиграйте красным, когда шериф умер в первую ночь"
    override val levels = listOf(1, 2, 5, 15, 30)
    override fun check(game: PolemicaGame, position: Int): Int =
        boolToInt(game.getRealComKiller() != null && game.getPositionRole(position) == Role.PEACE && game.result == PolemicaGameResult.RED_WIN)
}


