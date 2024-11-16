package com.github.mafia.vyasma.polemicaachivementservice.achievements.achievements

import com.github.mafia.vyasma.polemicaachivementservice.achievements.Achievement
import com.github.mafia.vyasma.polemicaachivementservice.model.game.PolemicaGame
import com.github.mafia.vyasma.polemicaachivementservice.model.game.Position
import com.github.mafia.vyasma.polemicaachivementservice.model.game.Role
import com.github.mafia.vyasma.polemicaachivementservice.utils.check

object FindSheriffAchievement : Achievement {
    override val id = "findSheriff"
    override val name = "Я нашел тебя!"
    override val description = "Найдите шерифа за дона в первую ночь"
    override val levels = listOf(1, 2, 5, 12, 30)
    override fun check(game: PolemicaGame, position: Position): Int = game.check {
        if (position.role() != Role.DON) {
            return 0
        }
        return boolToInt(game.checks.any { it.role == Role.DON && it.night == 1 && it.player.role() == Role.SHERIFF })
    }
}


