package com.github.mafia.vyasma.polemicaachivementservice.achievements.achievements

import com.github.mafia.vyasma.polemica.library.model.game.PolemicaGame
import com.github.mafia.vyasma.polemica.library.model.game.Position
import com.github.mafia.vyasma.polemica.library.model.game.Role
import com.github.mafia.vyasma.polemica.library.utils.check
import com.github.mafia.vyasma.polemica.library.utils.getKickedFromTable
import com.github.mafia.vyasma.polemica.library.utils.isRedWin
import com.github.mafia.vyasma.polemicaachivementservice.achievements.Achievement
import com.github.mafia.vyasma.polemicaachivementservice.achievements.AchievementCategory

object SheriffLiveAchievement : Achievement {
    override val id = "sheriffLive"
    override val name = "Жизнь шерифская"
    override val description = "Выиграйте игру за шерифа, оставшись за столом до конца"
    override val category = AchievementCategory.RED
    override val levels = listOf(1, 2, 5, 12, 30)
    override fun check(game: PolemicaGame, position: Position): Int = game.check {
        assert { position.role() == Role.SHERIFF }
        assert { game.isRedWin() }
        val kicked = game.getKickedFromTable()
        return boolToInt(
            kicked.none { it.position == position }
        )
    }
}


