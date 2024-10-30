package com.github.mafia.vyasma.polemicaachivementservice.achievements

import com.github.mafia.vyasma.polemicaachivementservice.model.game.PolemicaUser
import com.github.mafia.vyasma.polemicaachivementservice.model.jpa.AchievementUser
import com.github.mafia.vyasma.polemicaachivementservice.model.jpa.Game
import com.github.mafia.vyasma.polemicaachivementservice.repositories.AchievementUsersRepository
import com.github.mafia.vyasma.polemicaachivementservice.repositories.GameRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.EnableTransactionManagement

@Service
@EnableTransactionManagement
class AchievementServiceImpl(
    val gameRepository: GameRepository,
    val achievementUsersRepository: AchievementUsersRepository,
    val achievementTransactionalService: AchievementTransactionalService
) : AchievementService {
    private val logger = LoggerFactory.getLogger(AchievementServiceImpl::class.java.name)
    private val achievements = listOf(
        SamuraiPathGamerAchievement,
        WinAsDonAchievement,
        SniperAchievement,
        StrongSheriffAchievement,
        FullMafsAchievement,
        SheriffViceAchievement,
        StrongCityAchievement,
        VoteForBlackAchievement
    )

    private val achievementsMap = achievements.associateBy { it.id }

    override fun checkAchievements() {
        logger.info("Start check achievements")
        saveUsers()

        achievements.forEach { achievement ->
            processAchievement(achievement)
        }
        logger.info("End check achievements")
    }

    override fun getAchievements(
        gainsUsernames: List<String>,
        ids: List<Long>
    ): AchievementService.AchievementsWithGains {
        return AchievementService.AchievementsWithGains(
            achievements,
            build(achievementUsersRepository.findAllByUserUsernameInOrUserUserIdIn(gainsUsernames, ids))
        )
    }

    private fun build(achievementUsers: List<AchievementUser>): List<AchievementService.AchievementGainAnswer> {
        return achievementUsers.map {
            AchievementService.AchievementGainAnswer(
                user = PolemicaUser(it.user.userId, it.user.username),
                achievementId = it.achievement,
                achievementCounter = it.achievementCounter,
                achievementLevel = getAchievementLevel(it.achievement, it.achievementCounter)
            )
        }.sortedBy { achievementsMap[it.achievementId]?.order }
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

    fun processAchievement(achievement: Achievement) {
        gameRepository.findAllWhereNotAchievement(achievement.id).forEach { game ->
            achievementTransactionalService.processAchievementForGame(achievement, game)
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
