package com.github.mafia.vyasma.polemicaachivementservice.achievements.achievements

import com.github.mafia.vyasma.polemica.library.model.game.PolemicaGame
import com.github.mafia.vyasma.polemica.library.model.game.Position
import com.github.mafia.vyasma.polemica.library.utils.check
import com.github.mafia.vyasma.polemica.library.utils.getCriticDay
import com.github.mafia.vyasma.polemica.library.utils.isBlack
import com.github.mafia.vyasma.polemica.library.utils.isBlackWin
import com.github.mafia.vyasma.polemica.library.utils.isRed
import com.github.mafia.vyasma.polemica.library.utils.isRedWin
import com.github.mafia.vyasma.polemicaachivementservice.achievements.Achievement

object FoulsForWinOnCriticAchievement : Achievement {
    override val id = "foulsForWinOnCritic"
    override val name = "Не на жизнь, а насмерть"
    override val description = "Получите фол на критике, обеспечив победу своей команде"
    override val levels = listOf(1, 2, 5, 12, 30)
    override fun check(game: PolemicaGame, position: Position): Int = game.check {
        assert { position.role().isRed() && game.isRedWin() || position.role().isBlack() && game.isBlackWin() }
        val criticDay = game.getCriticDay()
        assert { criticDay != null }
        return position.player().fouls.count { it.stage.day >= criticDay!! }
    }
}


