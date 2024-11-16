package com.github.mafia.vyasma.polemicaachivementservice.achievements.achievements

import com.github.mafia.vyasma.polemicaachivementservice.achievements.Achievement
import com.github.mafia.vyasma.polemicaachivementservice.model.game.PolemicaGame
import com.github.mafia.vyasma.polemicaachivementservice.model.game.Position
import com.github.mafia.vyasma.polemicaachivementservice.model.game.Role
import com.github.mafia.vyasma.polemicaachivementservice.utils.check
import com.github.mafia.vyasma.polemicaachivementservice.utils.getRole
import com.github.mafia.vyasma.polemicaachivementservice.utils.isBlack

object FindAllMafsAchievement : Achievement {
    override val id = "findAllMafs"
    override val name = "Не скрыться"
    override val description = "Шерифом проверить всех чёрных за первые три ночи"
    override val levels = listOf(1, 2, 5, 12, 30)
    override fun check(game: PolemicaGame, position: Position): Int = game.check {
        if (position.role() != Role.SHERIFF) {
            return 0
        }
        return boolToInt(
            game.checks.filter { it.role == Role.SHERIFF && it.night <= 3 }
                .map { it.player }
                .toSet()
                .map { game.getRole(it) }
                .filter { it.isBlack() }
                .size == 3
        )
    }
}



