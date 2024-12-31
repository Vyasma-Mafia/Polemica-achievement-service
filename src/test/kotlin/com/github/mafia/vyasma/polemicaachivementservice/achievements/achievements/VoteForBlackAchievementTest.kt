package com.github.mafia.vyasma.polemicaachivementservice.achievements.achievements

import com.github.mafia.vyasma.polemica.library.model.game.Position
import kotlin.test.Test

class VoteForBlackAchievementTest {
    @Test
    fun testVoteForBlack() {
        Position.entries.forEach {
            testAchievement(
                VoteForBlackAchievement, 273009, it, when (it) {
                    Position.ONE -> 2
                    Position.FOUR, Position.SEVEN -> 1
                    else -> 0
                }
            )
        }
    }
}
