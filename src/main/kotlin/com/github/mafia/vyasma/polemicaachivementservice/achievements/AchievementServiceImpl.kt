package com.github.mafia.vyasma.polemicaachivementservice.achievements

import com.github.mafia.vyasma.polemicaachivementservice.model.game.PolemicaUser
import com.github.mafia.vyasma.polemicaachivementservice.model.jpa.AchievementGain
import com.github.mafia.vyasma.polemicaachivementservice.model.jpa.Game
import com.github.mafia.vyasma.polemicaachivementservice.repositories.AchievementGainsRepository
import com.github.mafia.vyasma.polemicaachivementservice.repositories.GameRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.EnableTransactionManagement

@Service
@EnableTransactionManagement
class AchievementServiceImpl(
    val gameRepository: GameRepository,
    val achievementGainsRepository: AchievementGainsRepository,
    val achievementTransactionalService: AchievementTransactionalService
) : AchievementService {
    private val achievements = listOf(
        WinAsDonAchievement,
        SniperAchievement,
        StrongSheriffAchievement,
        FullMafsAchievement,
        SheriffViceAchievement,
        StrongCityAchievement
    )

    private val achievementsMap = achievements.associateBy { it.id }

    override fun checkAchievements() {
        saveUsers()
        achievements.forEachIndexed { version, achievement ->
            processAchievement(achievement, version.toLong() + 1)
        }
    }

    override fun getAchievements(
        gainsUsernames: List<String>,
        ids: List<Long>
    ): AchievementService.AchievementsWithGains {
        return AchievementService.AchievementsWithGains(
            achievements,
            build(achievementGainsRepository.findAllByUserUsernameInOrUserUserIdIn(gainsUsernames, ids))
        )
    }

    private fun build(achievementGains: List<AchievementGain>): List<AchievementService.AchievementGainAnswer> {
        return achievementGains.map {
            AchievementService.AchievementGainAnswer(
                user = PolemicaUser(it.user.userId, it.user.username),
                achievementId = it.achievement,
                achievementCounter = it.achievementCounter,
                achievementLevel = getAchievementLevel(it.achievement, it.achievementCounter)
            )
        }
    }

    private fun getAchievementLevel(achievement: String, achievementCounter: Long?): Int {
        if (achievementCounter == null) {
            return 0
        }
        val levels = achievementsMap[achievement]?.levels ?: return 0
        levels.forEachIndexed { level, nextLevelCounter ->
            if (nextLevelCounter > achievementCounter) {
                return level
            }
        }
        return levels.size
    }

    fun processAchievement(achievement: Achievement, version: Long) {
        gameRepository.findAllWhereByProcessedVersionIs(version).forEach { game ->
            achievementTransactionalService.processAchievementForGame(achievement, game, version)
        }
    }

    fun saveUsers() {
        gameRepository.findAllWhereByProcessedVersionIs(0).forEach { game ->
            achievementTransactionalService.saveUsersFromGame(game)
        }
    }

    override fun getAchievementsGames(achievementId: String): AchievementService.AchievementGames {
        val achievement = achievementsMap[achievementId] ?: throw IllegalArgumentException("Achievement not found")

        return AchievementService.AchievementGames(gameRepository.findAll().flatMap { game ->
            gameWithPositionsForAchievement(game, achievement)
        })
    }

    private fun gameWithPositionsForAchievement(
        game: Game,
        achievement: Achievement
    ): List<AchievementService.AchievementGames.GamePostpositionForAchievement> =
        game.data.players.flatMap { player ->
            val checkResult = achievement.check(game.data, player.position)
            if (checkResult != 0) {
                return listOf(
                    AchievementService.AchievementGames.GamePostpositionForAchievement(
                        game.gameId,
                        game.gamePlace,
                        player.position,
                        checkResult
                    )
                )
            } else {
                emptyList()
            }
        }
}
