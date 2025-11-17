package com.github.mafia.vyasma.polemicaachivementservice.achievements.achievements

import com.github.mafia.vyasma.polemica.library.model.game.Position
import org.junit.jupiter.api.Test

class VotingOnlyForBlackAchievementTest {
    @Test
    fun testVotingOnlyForBlack() {
        testAchievement(VotingOnlyForBlackAchievement, 273009, Position.ONE, 1)
    }
}
