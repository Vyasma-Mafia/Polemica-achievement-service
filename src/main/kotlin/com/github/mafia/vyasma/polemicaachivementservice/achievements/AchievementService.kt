package com.github.mafia.vyasma.polemicaachivementservice.achievements

import com.fasterxml.jackson.annotation.JsonInclude
import com.github.mafia.vyasma.polemicaachivementservice.model.game.PolemicaUser

interface AchievementService {
    fun checkAchievements()
    fun getAchievements(gainsUsernames: List<String>, ids: List<Long>): AchievementsWithGains

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    data class AchievementsWithGains(
        val achievements: List<Achievement>,
        val achievementsGains: List<AchievementGainAnswer>
    )

    data class AchievementGainAnswer(
        val user: PolemicaUser,
        val achievementId: String,
        val achievementLevel: Int,
        val achievementCounter: Long?
    )
}


