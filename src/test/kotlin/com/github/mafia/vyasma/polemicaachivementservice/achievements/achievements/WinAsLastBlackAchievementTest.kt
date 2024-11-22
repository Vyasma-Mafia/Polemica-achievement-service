package com.github.mafia.vyasma.polemicaachivementservice.achievements.achievements

import com.github.mafia.vyasma.polemicaachivementservice.model.game.Position
import kotlin.test.Test

class WinAsLastBlackAchievementTest {
    @Test
    fun testWinBlackNotOne() {
        Position.entries.forEach {
            testAchievement(WinAsLastBlackAchievement, 270910, it, 0)
        }
    }
}
