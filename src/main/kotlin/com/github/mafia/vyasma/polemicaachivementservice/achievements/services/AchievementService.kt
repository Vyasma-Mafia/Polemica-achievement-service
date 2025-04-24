package com.github.mafia.vyasma.polemicaachivementservice.achievements.services

import com.github.mafia.vyasma.polemica.library.model.game.PolemicaUser
import com.github.mafia.vyasma.polemicaachivementservice.achievements.Achievement
import com.github.mafia.vyasma.polemicaachivementservice.model.jpa.PolemicaGamePlace
import java.time.LocalDateTime

interface AchievementService {
    // Существующие методы
    fun checkAchievements()
    fun recheckAchievements()
    fun getAchievementsGames(achievementId: String, gameId: Long?): AchievementGames

    // Обновленные методы с параметром даты
    fun getAchievements(
        gainsUsernames: List<String>,
        ids: List<Long>,
        startDate: LocalDateTime?
    ): AchievementsWithGains

    fun getTopAchievementUsers(
        userIds: List<Long>,
        rankLimit: Int,
        startDate: LocalDateTime?
    ): AchievementsWithGains

    // Существующие классы и интерфейсы...
    data class AchievementsWithGains(val achievements: List<Achievement>, val gains: List<AchievementGainAnswer>)
    data class AchievementGainAnswer(
        val user: PolemicaUser,
        val achievementId: String,
        val achievementCounter: Long?,
        val achievementLevel: Int
    )

    data class AchievementGames(val games: List<GamePostpositionForAchievement>) {
        data class GamePostpositionForAchievement(
            val gameId: Long,
            val gamePlace: PolemicaGamePlace,
            val position: Int,
            val count: Int
        )
    }
}



