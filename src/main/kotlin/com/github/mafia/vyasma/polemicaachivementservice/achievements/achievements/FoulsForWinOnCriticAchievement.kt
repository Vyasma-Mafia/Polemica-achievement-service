package com.github.mafia.vyasma.polemicaachivementservice.achievements.achievements

import com.github.mafia.vyasma.polemicaachivementservice.achievements.Achievement
import com.github.mafia.vyasma.polemicaachivementservice.model.game.PolemicaGame
import com.github.mafia.vyasma.polemicaachivementservice.model.game.Position
import com.github.mafia.vyasma.polemicaachivementservice.utils.check
import com.github.mafia.vyasma.polemicaachivementservice.utils.getCriticDay
import com.github.mafia.vyasma.polemicaachivementservice.utils.isBlack
import com.github.mafia.vyasma.polemicaachivementservice.utils.isBlackWin
import com.github.mafia.vyasma.polemicaachivementservice.utils.isRed
import com.github.mafia.vyasma.polemicaachivementservice.utils.isRedWin

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


