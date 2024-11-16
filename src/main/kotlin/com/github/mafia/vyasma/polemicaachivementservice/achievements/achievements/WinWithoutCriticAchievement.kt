package com.github.mafia.vyasma.polemicaachivementservice.achievements.achievements

import com.github.mafia.vyasma.polemicaachivementservice.achievements.Achievement
import com.github.mafia.vyasma.polemicaachivementservice.model.game.PolemicaGame
import com.github.mafia.vyasma.polemicaachivementservice.model.game.Position
import com.github.mafia.vyasma.polemicaachivementservice.utils.check
import com.github.mafia.vyasma.polemicaachivementservice.utils.getCriticDay
import com.github.mafia.vyasma.polemicaachivementservice.utils.isRed
import com.github.mafia.vyasma.polemicaachivementservice.utils.isRedWin

object WinWithoutCriticAchievement : Achievement {
    override val id = "winWithoutCritic"
    override val name = "Просушили"
    override val description = "Выиграйте игру за красного, не переходя в критику"
    override val levels = listOf(1, 3, 7, 20, 50)
    override fun check(game: PolemicaGame, position: Position): Int = game.check {
        assert { position.role().isRed() }
        assert { game.isRedWin() }
        return boolToInt(
            game.getCriticDay() == null
        )
    }
}


