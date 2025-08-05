package com.github.mafia.vyasma.polemicaachivementservice.statistics

import com.github.mafia.vyasma.polemica.library.model.game.Role
import com.github.mafia.vyasma.polemica.library.utils.isBlack
import com.github.mafia.vyasma.polemica.library.utils.isBlackWin
import com.github.mafia.vyasma.polemica.library.utils.isRed
import com.github.mafia.vyasma.polemica.library.utils.isRedWin
import com.github.mafia.vyasma.polemicaachivementservice.model.jpa.Game
import com.github.mafia.vyasma.polemicaachivementservice.model.statistics.GameSummary
import com.github.mafia.vyasma.polemicaachivementservice.model.statistics.PlayerSearchResult
import com.github.mafia.vyasma.polemicaachivementservice.model.statistics.PlayerStatistics
import com.github.mafia.vyasma.polemicaachivementservice.model.statistics.RoleStatistics
import com.github.mafia.vyasma.polemicaachivementservice.repositories.GameRepository
import com.github.mafia.vyasma.polemicaachivementservice.repositories.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import java.time.format.DateTimeFormatter

@Service
class PlayerStatisticsService(
    private val gameRepository: GameRepository,
    private val userRepository: UserRepository
) {
    private val logger = LoggerFactory.getLogger(this::class.java)
    private val dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")

    fun getPlayerStatistics(playerId: Long): PlayerStatistics? {
        val user = userRepository.findByIdOrNull(playerId) ?: return null

        // Получаем все игры игрока
        val playerGames = gameRepository.findAll()
            .filter { game ->
                game.data.players?.any { it.player?.id == playerId } ?: false
            }

        if (playerGames.isEmpty()) {
            return PlayerStatistics(
                playerId = playerId,
                username = user.username,
                totalGames = 0,
                totalWins = 0,
                winRate = 0.0,
                roleStatistics = emptyMap(),
                averagePoints = 0.0,
                lastGameDate = null
            )
        }

        // Группируем игры по ролям
        val gamesByRole = mutableMapOf<Role, MutableList<GameData>>()
        var totalWins = 0
        var totalPoints = 0.0
        var pointsCount = 0

        playerGames.forEach { game ->
            val player = game.data.players?.find { it.player?.id == playerId } ?: return@forEach
            val role = player.role
            val isWin = when {
                role.isRed() && game.data.isRedWin() -> true
                role.isBlack() && game.data.isBlackWin() -> true
                else -> false
            }

            if (isWin) totalWins++

            // Получаем очки игрока
            val points = game.points?.players?.find { it.position == player.position.value }?.points ?: 0.0
            if (game.points != null) {
                totalPoints += points
                pointsCount++
            }

            val gameData = GameData(
                game = game,
                isWin = isWin,
                points = points,
                role = role
            )

            gamesByRole.getOrPut(role) { mutableListOf() }.add(gameData)
        }

        // Рассчитываем статистику по ролям
        val roleStatistics = Role.entries.associateWith { role ->
            val roleGames = gamesByRole[role] ?: emptyList()
            calculateRoleStatistics(role, roleGames)
        }

        val lastGame = playerGames.maxByOrNull { it.started ?: it.createdAt!! }

        return PlayerStatistics(
            playerId = playerId,
            username = user.username,
            totalGames = playerGames.size,
            totalWins = totalWins,
            winRate = if (playerGames.isNotEmpty()) totalWins.toDouble() / playerGames.size else 0.0,
            roleStatistics = roleStatistics,
            averagePoints = if (pointsCount > 0) totalPoints / pointsCount else 0.0,
            lastGameDate = lastGame?.started?.format(dateFormatter)
        )
    }

    private fun calculateRoleStatistics(role: Role, games: List<GameData>): RoleStatistics {
        if (games.isEmpty()) {
            return RoleStatistics(
                role = role,
                gamesPlayed = 0,
                gamesWon = 0,
                winRate = 0.0,
                averagePoints = 0.0,
                bestGame = null,
                worstGame = null
            )
        }

        val wins = games.count { it.isWin }
        val gamesWithPoints = games.filter { it.game.points != null }
        val averagePoints = if (gamesWithPoints.isNotEmpty()) {
            gamesWithPoints.sumOf { it.points } / gamesWithPoints.size
        } else 0.0

        val bestGame = gamesWithPoints.maxByOrNull { it.points }?.let {
            GameSummary(
                gameId = it.game.gameId,
                date = it.game.started?.format(dateFormatter) ?: "",
                points = it.points,
                result = if (it.isWin) "Победа" else "Поражение"
            )
        }

        val worstGame = gamesWithPoints.minByOrNull { it.points }?.let {
            GameSummary(
                gameId = it.game.gameId,
                date = it.game.started?.format(dateFormatter) ?: "",
                points = it.points,
                result = if (it.isWin) "Победа" else "Поражение"
            )
        }

        return RoleStatistics(
            role = role,
            gamesPlayed = games.size,
            gamesWon = wins,
            winRate = wins.toDouble() / games.size,
            averagePoints = averagePoints,
            bestGame = bestGame,
            worstGame = worstGame
        )
    }

    fun searchPlayers(query: String): List<PlayerSearchResult> {
        if (query.length < 2) return emptyList()

        return userRepository.findByUsernameContainingIgnoreCase(query)
            .take(10) // Ограничиваем количество результатов
            .map { user ->
                PlayerSearchResult(
                    playerId = user.userId,
                    username = user.username,
                    rating = user.rating,
                    gamesPlayed = user.gamesPlayed
                )
            }
    }

    private data class GameData(
        val game: Game,
        val isWin: Boolean,
        val points: Double,
        val role: Role
    )
}
