package com.github.mafia.vyasma.polemicaachivementservice.achievements.achievements

import com.github.mafia.vyasma.polemica.library.model.game.Position
import org.junit.jupiter.api.Test

class WinThreeToThreeAchievementTest {
    @Test
    fun testWinThreeToThree() {
        val blackTeam = listOf(Position.THREE, Position.NINE, Position.TEN)
        Position.entries.forEach {
            testAchievement(
                WinThreeToThreeLastAchievement,
                273009,
                it,
                if (it in blackTeam) 1 else 0
            )
        }
    }
}
