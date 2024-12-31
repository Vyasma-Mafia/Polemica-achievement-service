package com.github.mafia.vyasma.polemicaachivementservice.achievements.achievements

import com.github.mafia.vyasma.polemica.library.model.game.PolemicaGame
import com.github.mafia.vyasma.polemica.library.model.game.Position
import com.github.mafia.vyasma.polemica.library.utils.getFinalVotes
import com.github.mafia.vyasma.polemica.library.utils.getRole
import com.github.mafia.vyasma.polemica.library.utils.isBlack
import com.github.mafia.vyasma.polemica.library.utils.isRed
import com.github.mafia.vyasma.polemicaachivementservice.achievements.Achievement
import com.github.mafia.vyasma.polemicaachivementservice.achievements.AchievementCategory

object VoteForBlackAchievement : Achievement {
    override val id = "voteForBlack"
    override val name = "Изгнать этого черныша!"
    override val description =
        "На красном проголосуйте за уход черного (учитывается последнее голосование на круге, при попиле рука за подъем)"
    override val category = AchievementCategory.RED
    override val levels = listOf(1, 5, 25, 100, 500)
    override fun check(game: PolemicaGame, position: Position): Int = if (game.getRole(position).isRed()) {
        game.getFinalVotes()
            .filter { it.position == position }
            .sumOf { finalVote -> finalVote.convicted.map { game.getRole(it) }.filter { it.isBlack() }.size }
    } else {
        0
    }
}


