package com.github.mafia.vyasma.polemicaachivementservice.statistics

import com.github.mafia.vyasma.polemica.library.model.game.Role
import com.github.mafia.vyasma.polemica.library.utils.isBlack
import com.github.mafia.vyasma.polemica.library.utils.isBlackWin
import com.github.mafia.vyasma.polemica.library.utils.isRed
import com.github.mafia.vyasma.polemica.library.utils.isRedWin
import com.github.mafia.vyasma.polemicaachivementservice.configurations.PairStatisticsConfig
import com.github.mafia.vyasma.polemicaachivementservice.model.jpa.Game
import com.github.mafia.vyasma.polemicaachivementservice.model.jpa.User
import com.github.mafia.vyasma.polemicaachivementservice.model.statistics.OppositeTeamStatistics
import com.github.mafia.vyasma.polemicaachivementservice.model.statistics.PairGame
import com.github.mafia.vyasma.polemicaachivementservice.model.statistics.PairGamesResponse
import com.github.mafia.vyasma.polemicaachivementservice.model.statistics.PairStatistics
import com.github.mafia.vyasma.polemicaachivementservice.model.statistics.PartnerStats
import com.github.mafia.vyasma.polemicaachivementservice.model.statistics.PlayerInfo
import com.github.mafia.vyasma.polemicaachivementservice.model.statistics.SameTeamStatistics
import com.github.mafia.vyasma.polemicaachivementservice.model.statistics.TeamStatistics
import com.github.mafia.vyasma.polemicaachivementservice.model.statistics.TopPartners
import com.github.mafia.vyasma.polemicaachivementservice.model.statistics.VersusStatistics
import com.github.mafia.vyasma.polemicaachivementservice.repositories.GameRepository
import com.github.mafia.vyasma.polemicaachivementservice.repositories.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.cache.annotation.Cacheable
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class PairStatisticsService(
    private val gameRepository: GameRepository,
    private val userRepository: UserRepository,
    private val config: PairStatisticsConfig
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    fun getPairStatistics(firstPlayerId: Long, secondPlayerId: Long): PairStatistics? {
        val firstUser = userRepository.findByIdOrNull(firstPlayerId) ?: return null
        val secondUser = userRepository.findByIdOrNull(secondPlayerId) ?: return null

        // Получаем все совместные игры
        val commonGames = findCommonGames(firstUser, secondUser)

        if (commonGames.isEmpty()) {
            return PairStatistics(
                firstPlayer = PlayerInfo(firstPlayerId, firstUser.username, firstUser.rating),
                secondPlayer = PlayerInfo(secondPlayerId, secondUser.username, secondUser.rating),
                totalGamesPlayed = 0,
                sameTeamStatistics = SameTeamStatistics(
                    totalGames = 0,
                    totalWins = 0,
                    winRate = 0.0,
                    asRed = TeamStatistics(0, 0, 0.0, 0.0, 0.0),
                    asBlack = TeamStatistics(0, 0, 0.0, 0.0, 0.0)
                ),
                oppositeTeamStatistics = OppositeTeamStatistics(
                    totalGames = 0,
                    firstPlayerWins = 0,
                    secondPlayerWins = 0,
                    firstPlayerWinRate = 0.0,
                    secondPlayerWinRate = 0.0,
                    firstRedSecondBlack = VersusStatistics(0, 0, 0.0),
                    firstBlackSecondRed = VersusStatistics(0, 0, 0.0)
                ),
                lastGameTogether = null,
                firstGameTogether = null
            )
        }

        // Анализируем игры
        val sameTeamGames = mutableListOf<GameAnalysis>()
        val oppositeTeamGames = mutableListOf<GameAnalysis>()

        commonGames.forEach { game ->
            val analysis = analyzeGame(game, firstPlayerId, secondPlayerId)
            if (analysis.sameTeam) {
                sameTeamGames.add(analysis)
            } else {
                oppositeTeamGames.add(analysis)
            }
        }

        // Рассчитываем статистику для одной команды
        val sameTeamStats = calculateSameTeamStatistics(sameTeamGames)

        // Рассчитываем статистику для разных команд
        val oppositeTeamStats = calculateOppositeTeamStatistics(oppositeTeamGames)

        val sortedGames = commonGames.sortedBy { it.started ?: it.createdAt }

        return PairStatistics(
            firstPlayer = PlayerInfo(firstPlayerId, firstUser.username, firstUser.rating),
            secondPlayer = PlayerInfo(secondPlayerId, secondUser.username, secondUser.rating),
            totalGamesPlayed = commonGames.size,
            sameTeamStatistics = sameTeamStats,
            oppositeTeamStatistics = oppositeTeamStats,
            lastGameTogether = sortedGames.lastOrNull()?.started,
            firstGameTogether = sortedGames.firstOrNull()?.started
        )
    }

    fun getCommonGames(
        firstPlayerId: Long,
        secondPlayerId: Long,
        page: Int = 0,
        size: Int = 20
    ): PairGamesResponse? {
        val firstUser = userRepository.findByIdOrNull(firstPlayerId) ?: return null
        val secondUser = userRepository.findByIdOrNull(secondPlayerId) ?: return null

        val allCommonGames = findCommonGames(firstUser, secondUser)
        val sortedGames = allCommonGames.sortedByDescending { it.started ?: it.createdAt }

        // Пагинация
        val startIndex = page * size
        val endIndex = minOf(startIndex + size, sortedGames.size)
        val pageGames = if (startIndex < sortedGames.size) {
            sortedGames.subList(startIndex, endIndex)
        } else {
            emptyList()
        }

        val pairGames = pageGames.map { game ->
            val firstPlayer = game.data.players?.find { it.player?.id == firstPlayerId }!!
            val secondPlayer = game.data.players?.find { it.player?.id == secondPlayerId }!!

            val firstPoints = game.points?.players?.find { it.position == firstPlayer.position.value }?.points ?: 0.0
            val secondPoints = game.points?.players?.find { it.position == secondPlayer.position.value }?.points ?: 0.0

            val sameTeam = (firstPlayer.role.isRed() && secondPlayer.role.isRed()) ||
                (firstPlayer.role.isBlack() && secondPlayer.role.isBlack())

            val result = if (game.data.isRedWin()) "Победа красных" else "Победа черных"

            val winner = when {
                sameTeam -> {
                    val teamWon = (firstPlayer.role.isRed() && game.data.isRedWin()) ||
                        (firstPlayer.role.isBlack() && game.data.isBlackWin())
                    if (teamWon) "Оба" else null
                }

                else -> {
                    when {
                        firstPlayer.role.isRed() && game.data.isRedWin() -> firstUser.username
                        firstPlayer.role.isBlack() && game.data.isBlackWin() -> firstUser.username
                        secondPlayer.role.isRed() && game.data.isRedWin() -> secondUser.username
                        secondPlayer.role.isBlack() && game.data.isBlackWin() -> secondUser.username
                        else -> null
                    }
                }
            }

            PairGame(
                gameId = game.gameId,
                date = game.started ?: game.createdAt!!,
                firstPlayerRole = getRoleName(firstPlayer.role),
                secondPlayerRole = getRoleName(secondPlayer.role),
                firstPlayerPoints = firstPoints,
                secondPlayerPoints = secondPoints,
                sameTeam = sameTeam,
                result = result,
                winner = winner,
                competitive = game.gamePlace.competitionId != null
            )
        }

        return PairGamesResponse(
            firstPlayer = PlayerInfo(firstPlayerId, firstUser.username, firstUser.rating),
            secondPlayer = PlayerInfo(secondPlayerId, secondUser.username, secondUser.rating),
            games = pairGames,
            totalCount = allCommonGames.size,
            page = page,
            pageSize = size
        )
    }

    private fun findCommonGames(firstUser: User, secondUser: User): List<Game> {
        return gameRepository.findAllByUserJoinedFromPlayerRatingHistory(firstUser).filter { game ->
            val hasFirst = game.data.players?.any { it.player?.id == firstUser.userId } ?: false
            val hasSecond = game.data.players?.any { it.player?.id == secondUser.userId } ?: false
            hasFirst && hasSecond
        }
    }

    private fun analyzeGame(game: Game, firstPlayerId: Long, secondPlayerId: Long): GameAnalysis {
        val firstPlayer = game.data.players?.find { it.player?.id == firstPlayerId }!!
        val secondPlayer = game.data.players?.find { it.player?.id == secondPlayerId }!!

        val firstRole = firstPlayer.role
        val secondRole = secondPlayer.role

        val sameTeam = (firstRole.isRed() && secondRole.isRed()) ||
            (firstRole.isBlack() && secondRole.isBlack())

        val isWin = when {
            sameTeam && firstRole.isRed() && game.data.isRedWin() -> true
            sameTeam && firstRole.isBlack() && game.data.isBlackWin() -> true
            !sameTeam && firstRole.isRed() && game.data.isRedWin() -> true
            !sameTeam && firstRole.isBlack() && game.data.isBlackWin() -> true
            else -> false
        }

        val firstPoints = game.points?.players?.find { it.position == firstPlayer.position.value }?.points ?: 0.0
        val secondPoints = game.points?.players?.find { it.position == secondPlayer.position.value }?.points ?: 0.0

        return GameAnalysis(
            game = game,
            firstRole = firstRole,
            secondRole = secondRole,
            sameTeam = sameTeam,
            isWin = isWin,
            firstPoints = firstPoints,
            secondPoints = secondPoints,
            hasPoints = game.points != null
        )
    }

    private fun calculateSameTeamStatistics(games: List<GameAnalysis>): SameTeamStatistics {
        val totalWins = games.count { it.isWin }

        val redGames = games.filter { it.firstRole.isRed() }
        val blackGames = games.filter { it.firstRole.isBlack() }

        val asRed = calculateTeamStatistics(redGames)
        val asBlack = calculateTeamStatistics(blackGames)

        return SameTeamStatistics(
            totalGames = games.size,
            totalWins = totalWins,
            winRate = if (games.isNotEmpty()) totalWins.toDouble() / games.size else 0.0,
            asRed = asRed,
            asBlack = asBlack
        )
    }

    private fun calculateTeamStatistics(games: List<GameAnalysis>): TeamStatistics {
        val wins = games.count { it.isWin }
        val gamesWithPoints = games.filter { it.hasPoints }

        val avgPointsFirst = if (gamesWithPoints.isNotEmpty()) {
            gamesWithPoints.sumOf { it.firstPoints } / gamesWithPoints.size
        } else 0.0

        val avgPointsSecond = if (gamesWithPoints.isNotEmpty()) {
            gamesWithPoints.sumOf { it.secondPoints } / gamesWithPoints.size
        } else 0.0

        return TeamStatistics(
            games = games.size,
            wins = wins,
            winRate = if (games.isNotEmpty()) wins.toDouble() / games.size else 0.0,
            averagePointsFirst = avgPointsFirst,
            averagePointsSecond = avgPointsSecond
        )
    }

    private fun calculateOppositeTeamStatistics(games: List<GameAnalysis>): OppositeTeamStatistics {
        val firstPlayerWins = games.count { it.isWin }
        val secondPlayerWins = games.size - firstPlayerWins

        val firstRedGames = games.filter { it.firstRole.isRed() }
        val firstBlackGames = games.filter { it.firstRole.isBlack() }

        val firstRedStats = VersusStatistics(
            games = firstRedGames.size,
            firstPlayerWins = firstRedGames.count { it.isWin },
            winRate = if (firstRedGames.isNotEmpty()) {
                firstRedGames.count { it.isWin }.toDouble() / firstRedGames.size
            } else 0.0
        )

        val firstBlackStats = VersusStatistics(
            games = firstBlackGames.size,
            firstPlayerWins = firstBlackGames.count { it.isWin },
            winRate = if (firstBlackGames.isNotEmpty()) {
                firstBlackGames.count { it.isWin }.toDouble() / firstBlackGames.size
            } else 0.0
        )

        return OppositeTeamStatistics(
            totalGames = games.size,
            firstPlayerWins = firstPlayerWins,
            secondPlayerWins = secondPlayerWins,
            firstPlayerWinRate = if (games.isNotEmpty()) firstPlayerWins.toDouble() / games.size else 0.0,
            secondPlayerWinRate = if (games.isNotEmpty()) secondPlayerWins.toDouble() / games.size else 0.0,
            firstRedSecondBlack = firstRedStats,
            firstBlackSecondRed = firstBlackStats
        )
    }

    private fun getRoleName(role: Role): String {
        return when (role) {
            Role.PEACE -> "Мирный"
            Role.MAFIA -> "Мафия"
            Role.DON -> "Дон"
            Role.SHERIFF -> "Шериф"
        }
    }

    private data class GameAnalysis(
        val game: Game,
        val firstRole: Role,
        val secondRole: Role,
        val sameTeam: Boolean,
        val isWin: Boolean,
        val firstPoints: Double,
        val secondPoints: Double,
        val hasPoints: Boolean
    )

    @Cacheable(
        value = ["topPartners"],
        key = "#playerId + ':' + #minGames + ':' + #topCount",
        condition = "@pairStatisticsConfig.cacheEnabled"
    )
    fun getTopPartners(
        playerId: Long,
        minGames: Int? = null,
        topCount: Int? = null
    ): TopPartners? {
        val effectiveMinGames = minGames ?: config.minGamesThreshold
        val effectiveTopCount = topCount ?: config.topPartnersCount

        val player = userRepository.findByIdOrNull(playerId) ?: return null
        val allGames = findCommonGames(player, player) // Получаем все игры игрока

        if (allGames.isEmpty()) {
            return TopPartners(
                playerId = playerId,
                playerName = player.username,
                bestPartners = emptyList(),
                worstPartners = emptyList(),
                calculatedAt = LocalDateTime.now(),
                minGamesThreshold = effectiveMinGames,
                totalPartnersAnalyzed = 0
            )
        }

        // Мапа для хранения статистики по каждому напарнику
        val partnerStatsMap = mutableMapOf<Long, MutablePartnerStats>()

        // Анализируем каждую игру
        allGames.forEach { game ->
            val playerData = game.data.players?.find { it.player?.id == playerId } ?: return@forEach
            val playerRole = playerData.role
            val isPlayerRed = playerRole.isRed()
            val isWin = (isPlayerRed && game.data.isRedWin()) || (!isPlayerRed && game.data.isBlackWin())

            // Находим всех напарников в той же команде
            game.data.players?.forEach { teammateData ->
                val teammateId = teammateData.player?.id ?: return@forEach
                if (teammateId == playerId) return@forEach

                val teammateRole = teammateData.role
                val isSameTeam = (isPlayerRed && teammateRole.isRed()) || (!isPlayerRed && teammateRole.isBlack())

                if (isSameTeam) {
                    val stats = partnerStatsMap.getOrPut(teammateId) {
                        val user = userRepository.findByIdOrNull(teammateId)
                        MutablePartnerStats(
                            partnerId = teammateId,
                            partnerName = user?.username ?: "Unknown",
                            partnerRating = user?.rating
                        )
                    }

                    stats.totalGames++
                    if (isWin) stats.wins++ else stats.losses++
                    stats.lastGameTogether = game.started ?: game.createdAt

                    // Добавляем очки, если они есть
                    game.points?.players?.find { it.position == teammateData.position.value }?.let {
                        stats.totalPoints += it.points
                    }
                }
            }
        }

        // Фильтруем по минимальному количеству игр и конвертируем в PartnerStats
        val qualifiedPartners = partnerStatsMap.values
            .filter { it.totalGames >= effectiveMinGames }
            .map { mutableStats ->
                PartnerStats(
                    partnerId = mutableStats.partnerId,
                    partnerName = mutableStats.partnerName,
                    partnerRating = mutableStats.partnerRating,
                    totalGames = mutableStats.totalGames,
                    wins = mutableStats.wins,
                    losses = mutableStats.losses,
                    winRate = if (mutableStats.totalGames > 0) {
                        (mutableStats.wins.toDouble() / mutableStats.totalGames) * 100
                    } else 0.0,
                    lastGameTogether = mutableStats.lastGameTogether,
                    averagePointsTogether = if (mutableStats.totalGames > 0) {
                        mutableStats.totalPoints / mutableStats.totalGames
                    } else null
                )
            }

        // Сортируем и выбираем топы
        val sortedByWinRate = qualifiedPartners.sortedByDescending { it.winRate }
        val bestPartners = sortedByWinRate.take(effectiveTopCount)
        val worstPartners = sortedByWinRate.takeLast(effectiveTopCount).reversed()

        return TopPartners(
            playerId = playerId,
            playerName = player.username,
            bestPartners = bestPartners,
            worstPartners = worstPartners,
            calculatedAt = LocalDateTime.now(),
            minGamesThreshold = effectiveMinGames,
            totalPartnersAnalyzed = qualifiedPartners.size
        )
    }

    private data class MutablePartnerStats(
        val partnerId: Long,
        val partnerName: String,
        val partnerRating: Double?,
        var totalGames: Int = 0,
        var wins: Int = 0,
        var losses: Int = 0,
        var lastGameTogether: LocalDateTime? = null,
        var totalPoints: Double = 0.0
    )
}
