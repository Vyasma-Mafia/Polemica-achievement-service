package com.github.mafia.vyasma.polemicaachivementservice.achievements.achievements

import com.github.mafia.vyasma.polemica.library.model.game.Position
import org.junit.jupiter.api.Test

class FirstKickedFullGuessAchievementTest {
    @Test
    fun testSimplePositive() {
        Position.entries.forEach {
            testAchievement(FirstKickedFullGuessAchievement, 270561, it, if (it == Position.ONE) 1 else 0)
        }
    }
}
