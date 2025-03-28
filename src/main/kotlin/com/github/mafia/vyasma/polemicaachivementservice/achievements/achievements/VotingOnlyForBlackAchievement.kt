package com.github.mafia.vyasma.polemicaachivementservice.achievements.achievements

import com.github.mafia.vyasma.polemica.library.model.game.PolemicaGame
import com.github.mafia.vyasma.polemica.library.model.game.Position
import com.github.mafia.vyasma.polemica.library.model.game.Role
import com.github.mafia.vyasma.polemica.library.utils.check
import com.github.mafia.vyasma.polemica.library.utils.getFinalVotes
import com.github.mafia.vyasma.polemica.library.utils.isBlack
import com.github.mafia.vyasma.polemicaachivementservice.achievements.Achievement
import com.github.mafia.vyasma.polemicaachivementservice.achievements.AchievementCategory

object VotingOnlyForBlackAchievement : Achievement {
    override val id = "votingOnlyForBlack"
    override val name = "Сильная рука"
    override val description = "Будучи мирным, голосуйте только за черных (кроме попилов)"
    override val category = AchievementCategory.RED
    override val levels = listOf(1, 3, 7, 20, 50)
    override fun check(game: PolemicaGame, position: Position): Int = game.check {
        assert { position.role() == Role.PEACE }
        val votes = game.getFinalVotes(null)
        return boolToInt(
            votes.filter { it.position == position }
                .all { candidates -> candidates.convicted.any { it.role().isBlack() } }
        )
    }
}


