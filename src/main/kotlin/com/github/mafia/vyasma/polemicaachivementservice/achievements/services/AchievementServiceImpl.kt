package com.github.mafia.vyasma.polemicaachivementservice.achievements.services

import com.github.mafia.vyasma.polemica.library.model.game.PolemicaUser
import com.github.mafia.vyasma.polemicaachivementservice.achievements.Achievement
import com.github.mafia.vyasma.polemicaachivementservice.achievements.achievements.FindAllMafsAchievement
import com.github.mafia.vyasma.polemicaachivementservice.achievements.achievements.FindSheriffAchievement
import com.github.mafia.vyasma.polemicaachivementservice.achievements.achievements.FirstKickedFullGuessAchievement
import com.github.mafia.vyasma.polemicaachivementservice.achievements.achievements.FoulsForWinOnCriticAchievement
import com.github.mafia.vyasma.polemicaachivementservice.achievements.achievements.FullMafsAchievement
import com.github.mafia.vyasma.polemicaachivementservice.achievements.achievements.ManyVicesAchievement
import com.github.mafia.vyasma.polemicaachivementservice.achievements.achievements.PartialMafsGuessAchievement
import com.github.mafia.vyasma.polemicaachivementservice.achievements.achievements.SamuraiPathAchievement
import com.github.mafia.vyasma.polemicaachivementservice.achievements.achievements.SheriffLiveAchievement
import com.github.mafia.vyasma.polemicaachivementservice.achievements.achievements.SheriffViceAchievement
import com.github.mafia.vyasma.polemicaachivementservice.achievements.achievements.SniperAchievement
import com.github.mafia.vyasma.polemicaachivementservice.achievements.achievements.StrongCityAchievement
import com.github.mafia.vyasma.polemicaachivementservice.achievements.achievements.StrongSheriffAchievement
import com.github.mafia.vyasma.polemicaachivementservice.achievements.achievements.VoteForBlackAchievement
import com.github.mafia.vyasma.polemicaachivementservice.achievements.achievements.VotingOnlyForBlackAchievement
import com.github.mafia.vyasma.polemicaachivementservice.achievements.achievements.WinAsBlackAchievement
import com.github.mafia.vyasma.polemicaachivementservice.achievements.achievements.WinAsDonAchievement
import com.github.mafia.vyasma.polemicaachivementservice.achievements.achievements.WinAsLastBlackAchievement
import com.github.mafia.vyasma.polemicaachivementservice.achievements.achievements.WinAsRedInLastAchievement
import com.github.mafia.vyasma.polemicaachivementservice.achievements.achievements.WinThreeToThreeLastAchievement
import com.github.mafia.vyasma.polemicaachivementservice.achievements.achievements.WinWithSelfKillAchievement
import com.github.mafia.vyasma.polemicaachivementservice.achievements.achievements.WinWithoutCriticAchievement
import com.github.mafia.vyasma.polemicaachivementservice.model.jpa.Game
import com.github.mafia.vyasma.polemicaachivementservice.repositories.AchievementGameRepository
import com.github.mafia.vyasma.polemicaachivementservice.repositories.AchievementGameUserRepository
import com.github.mafia.vyasma.polemicaachivementservice.repositories.AchievementSummaryProjection
import com.github.mafia.vyasma.polemicaachivementservice.repositories.GameRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.EnableTransactionManagement
import java.time.LocalDateTime

@Service
@EnableTransactionManagement
class AchievementServiceImpl(
    val gameRepository: GameRepository,
    val achievementTransactionalService: AchievementTransactionalService,
    val achievementCheckGameStartedAfter: LocalDateTime,
    private val achievementGameRepository: AchievementGameRepository,
    private val achievementGameUserRepository: AchievementGameUserRepository
) : AchievementService {
    private val logger = LoggerFactory.getLogger(AchievementServiceImpl::class.java.name)
    private val achievements = listOf(
        SamuraiPathAchievement,
        WinAsDonAchievement,
        SniperAchievement,
        StrongSheriffAchievement,
        FullMafsAchievement,
        SheriffViceAchievement,
        StrongCityAchievement,
        VoteForBlackAchievement,
        WinAsBlackAchievement,
        WinAsLastBlackAchievement,
        WinAsRedInLastAchievement,
        WinThreeToThreeLastAchievement,
        PartialMafsGuessAchievement,
        FindSheriffAchievement,
        FindAllMafsAchievement,
        FirstKickedFullGuessAchievement,
        WinWithSelfKillAchievement,
        SheriffLiveAchievement,
        VotingOnlyForBlackAchievement,
        WinWithoutCriticAchievement,
        FoulsForWinOnCriticAchievement,
        ManyVicesAchievement
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

    override fun recheckAchievements() {
        logger.info("Start deleting gotten achievements")
        achievementGameUserRepository.deleteAll()
        achievementGameRepository.deleteAll()
        logger.info("End deleting gotten achievements")
        checkAchievements()
    }

    // Обновленный метод с поддержкой фильтрации по дате
    override fun getAchievements(
        gainsUsernames: List<String>,
        ids: List<Long>,
        startDate: LocalDateTime?
    ): AchievementService.AchievementsWithGains {
        // Используем новый метод из репозитория
        val achievementSummaries = achievementGameUserRepository
            .findAchievementSummaryByUsernamesOrUserIdsAndAfterDate(
                gainsUsernames,
                ids,
                startDate ?: achievementCheckGameStartedAfter // Используем default дату если не указано
            )

        val gains = buildFromProjections(achievementSummaries)

        return AchievementService.AchievementsWithGains(achievements, gains)
    }

    // Новый метод для создания ответов из проекций базы данных
    private fun buildFromProjections(
        projections: List<AchievementSummaryProjection>
    ): List<AchievementService.AchievementGainAnswer> {
        return projections.map { projection ->
            AchievementService.AchievementGainAnswer(
                user = PolemicaUser(projection.getUserId(), projection.getUsername()),
                achievementId = projection.getAchievementId(),
                achievementCounter = projection.getCounter(),
                achievementLevel = getAchievementLevel(projection.getAchievementId(), projection.getCounter())
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
        gameRepository.findAllWhereNotAchievement(achievement.id)
            .forEach { game ->
                achievementTransactionalService.processAchievementForGame(achievement, game)
            }
    }

    fun saveUsers() {
        gameRepository.findAllWhereByProcessedVersionIs(0).forEach { game ->
            achievementTransactionalService.saveUsersFromGame(game)
        }
    }

    override fun getAchievementsGames(achievementId: String, gameId: Long?): AchievementService.AchievementGames {
        val achievement = achievementsMap[achievementId] ?: throw IllegalArgumentException("Achievement not found")

        val games = if (gameId != null) {
            gameRepository.findAllById(listOf(gameId))
        } else {
            gameRepository.findAll()
        }
        return AchievementService.AchievementGames(games.flatMap { game ->
            gameWithPositionsForAchievement(game, achievement)
        })
    }

    private fun gameWithPositionsForAchievement(
        game: Game,
        achievement: Achievement
    ): List<AchievementService.AchievementGames.GamePostpositionForAchievement> =
        game.data.players?.flatMap { player ->
            val checkResult = achievement.check(game.data, player.position)
            if (checkResult != 0) {
                return@flatMap listOf(
                    AchievementService.AchievementGames.GamePostpositionForAchievement(
                        game.gameId,
                        game.gamePlace,
                        player.position.value,
                        checkResult
                    )
                )
            } else {
                emptyList()
            }
        } ?: emptyList()

    // Обновленный метод с поддержкой фильтрации по дате
    override fun getTopAchievementUsers(
        userIds: List<Long>,
        rankLimit: Int,
        startDate: LocalDateTime?
    ): AchievementService.AchievementsWithGains {
        // Используем новый метод из репозитория
        val topAchievements = achievementGameUserRepository
            .findTopAchievementSummaryByUserIdsAndAfterDate(
                userIds,
                startDate ?: achievementCheckGameStartedAfter,
                rankLimit
            )

        val gains = buildFromProjections(topAchievements)

        return AchievementService.AchievementsWithGains(achievements, gains)
    }
}
