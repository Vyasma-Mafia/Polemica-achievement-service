package com.github.mafia.vyasma.polemicaachivementservice.achievements

import com.fasterxml.jackson.annotation.JsonInclude
import com.github.mafia.vyasma.polemicaachivementservice.model.game.PolemicaGamePlace
import com.github.mafia.vyasma.polemicaachivementservice.model.game.PolemicaUser

interface AchievementService {
    fun checkAchievements()
    fun getAchievements(gainsUsernames: List<String>, ids: List<Long>): AchievementsWithGains
    fun getAchievementsGames(achievementId: String): AchievementGames

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    data class AchievementGames(
        val games: List<GamePostpositionForAchievement>
    ) {
        data class GamePostpositionForAchievement(
            val gameId: Long,
            val gamePlace: PolemicaGamePlace?,
            val position: Int,
            val checkResult: Int
        )
    }

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


