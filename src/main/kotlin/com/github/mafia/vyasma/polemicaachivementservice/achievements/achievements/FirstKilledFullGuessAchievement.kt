package com.github.mafia.vyasma.polemicaachivementservice.achievements.achievements

import com.github.mafia.vyasma.polemicaachivementservice.achievements.Achievement
import com.github.mafia.vyasma.polemicaachivementservice.model.game.PolemicaGame
import com.github.mafia.vyasma.polemicaachivementservice.model.game.Position
import com.github.mafia.vyasma.polemicaachivementservice.utils.check
import com.github.mafia.vyasma.polemicaachivementservice.utils.getKickedFromTable
import com.github.mafia.vyasma.polemicaachivementservice.utils.isBlack
import com.github.mafia.vyasma.polemicaachivementservice.utils.isRed

object FirstKilledFullGuessAchievement : Achievement {
    override val id = "firstKickedFullGuess"
    override val name = "Цветопопадатель"
    override val description = "Будучи первым покинувшим стол красным, оставьте три верных цвета в лучший ход"
    override val levels = listOf(1, 2, 5, 12, 30)
    override fun check(game: PolemicaGame, position: Position): Int = game.check {
        assert { position.role().isRed() }
        val kicked = game.getKickedFromTable()
        assert { kicked.isNotEmpty() }
        val firstKicked = kicked.first()
        assert { kicked.filter { it.gamePhase == firstKicked.gamePhase }.size == 1 }
        assert { firstKicked.position == position }

        val guess = position.guess() ?: return 0
        assert { guess.mafs?.size?.plus(guess.civs?.size ?: 0) == 3 }
        return boolToInt(
            guess.mafs?.all { it.role().isBlack() } == true &&
                guess.civs?.all { it.role().isRed() } == true
        )
    }
}


