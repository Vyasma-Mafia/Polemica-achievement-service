package com.github.mafia.vyasma.polemicaachivementservice.achievements

import com.github.mafia.vyasma.polemicaachivementservice.model.game.PolemicaGame
import com.github.mafia.vyasma.polemicaachivementservice.utils.getFinalVotes
import com.github.mafia.vyasma.polemicaachivementservice.utils.getPositionRole
import com.github.mafia.vyasma.polemicaachivementservice.utils.isBlack
import com.github.mafia.vyasma.polemicaachivementservice.utils.isRed

object VoteForBlackAchievement : Achievement {
    override val id = "voteForBlack"
    override val name = "Изгнать этого черныша!"
    override val description =
        "На красном проголосуйте за уход черного (учитывается последнее голосование на круге, при попиле рука за подъем)"
    override val levels = listOf(1, 10, 50, 150, 500)
    override fun check(game: PolemicaGame, position: Int): Int = if (game.getPositionRole(position).isRed()) {
        game.getFinalVotes()
            .filter { it.position == position }
            .sumOf { finalVote -> finalVote.convicted.map { game.getPositionRole(it) }.filter { it.isBlack() }.size }
    } else {
        0
    }
}


