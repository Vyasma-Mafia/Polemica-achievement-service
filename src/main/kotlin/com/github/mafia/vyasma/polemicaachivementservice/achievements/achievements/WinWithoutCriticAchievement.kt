package com.github.mafia.vyasma.polemicaachivementservice.achievements.achievements

import com.github.mafia.vyasma.polemica.library.model.game.PolemicaGame
import com.github.mafia.vyasma.polemica.library.model.game.Position
import com.github.mafia.vyasma.polemica.library.utils.check
import com.github.mafia.vyasma.polemica.library.utils.getCriticDay
import com.github.mafia.vyasma.polemica.library.utils.isRed
import com.github.mafia.vyasma.polemica.library.utils.isRedWin
import com.github.mafia.vyasma.polemicaachivementservice.achievements.Achievement
import com.github.mafia.vyasma.polemicaachivementservice.achievements.AchievementCategory

object WinWithoutCriticAchievement : Achievement {
    override val id = "winWithoutCritic"
    override val name = "Просушили"
    override val description = "Выиграйте игру за красного, не переходя в критику"
    override val category = AchievementCategory.RED
    override val levels = listOf(1, 2, 5, 15, 30)
    override fun check(game: PolemicaGame, position: Position): Int = game.check {
        assert { position.role().isRed() }
        assert { game.isRedWin() }
        return boolToInt(
            game.getCriticDay() == null
        )
    }
}


