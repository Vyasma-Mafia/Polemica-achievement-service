package com.github.mafia.vyasma.polemicaachivementservice.achievements.achievements

import com.github.mafia.vyasma.polemica.library.model.game.Position
import kotlin.test.Test

class WinAsLastBlackAchievementTest {
    @Test
    fun testWinBlackNotOne() {
        Position.entries.forEach {
            testAchievement(WinAsLastBlackAchievement, 272910, it, 0)
        }
    }
}
