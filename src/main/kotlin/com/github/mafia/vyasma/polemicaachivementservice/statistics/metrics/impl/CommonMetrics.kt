package com.github.mafia.vyasma.polemicaachivementservice.statistics.metrics.impl

import com.github.mafia.vyasma.polemica.library.model.game.Role
import com.github.mafia.vyasma.polemica.library.utils.isBlack
import com.github.mafia.vyasma.polemica.library.utils.isBlackWin
import com.github.mafia.vyasma.polemica.library.utils.isRed
import com.github.mafia.vyasma.polemica.library.utils.isRedWin
import com.github.mafia.vyasma.polemicaachivementservice.model.jpa.Game
import com.github.mafia.vyasma.polemicaachivementservice.statistics.metrics.MetricResult
import com.github.mafia.vyasma.polemicaachivementservice.statistics.metrics.NumericMetric
import com.github.mafia.vyasma.polemicaachivementservice.statistics.metrics.PercentageMetric
import org.springframework.stereotype.Component

/**
 * Метрика для подсчета серии побед
 */
@Component
class WinStreakMetric : NumericMetric() {
    override val id = "win_streak"
    override val displayName = "Серия побед"
    override val description = "Максимальная серия побед подряд"
    override val category = "Общие"

    override fun calculate(games: List<Game>, playerId: Long): MetricResult {
        var maxStreak = 0
        var currentStreak = 0

        games.sortedBy { it.started ?: it.createdAt }.forEach { game ->
            val player = game.data.players?.find { it.player?.id == playerId } ?: return@forEach
            val isWin = when {
                player.role.isRed() && game.data.isRedWin() -> true
                player.role.isBlack() && game.data.isBlackWin() -> true
                else -> false
            }

            if (isWin) {
                currentStreak++
                maxStreak = maxOf(maxStreak, currentStreak)
            } else {
                currentStreak = 0
            }
        }

        return MetricResult(
            value = maxStreak,
            displayValue = "$maxStreak игр",
            additionalData = mapOf("currentStreak" to currentStreak)
        )
    }
}

/**
 * Метрика для подсчета процента игр за красных
 */
@Component
class RedTeamPercentageMetric : PercentageMetric() {
    override val id = "red_team_percentage"
    override val displayName = "Процент игр за красных"
    override val description = "Процент игр, сыгранных за мирных жителей"
    override val category = "Команды"

    override fun calculate(games: List<Game>, playerId: Long): MetricResult {
        val redGames = games.count { game ->
            game.data.players?.find { it.player?.id == playerId }?.role?.isRed() ?: false
        }

        val percentage = calculatePercentage(redGames, games.size)

        return MetricResult(
            value = percentage,
            displayValue = formatPercentage(percentage),
            additionalData = mapOf(
                "redGames" to redGames,
                "totalGames" to games.size
            )
        )
    }
}

/**
 * Метрика для подсчета среднего места в рейтинге после игры
 */
@Component
class AverageRatingPositionMetric : NumericMetric() {
    override val id = "avg_rating_position"
    override val displayName = "Среднее место в рейтинге"
    override val description = "Среднее место в рейтинге после каждой игры"
    override val category = "Рейтинг"

    override fun calculate(games: List<Game>, playerId: Long): MetricResult {
        // Эта метрика требует дополнительных данных о рейтинге
        // Здесь приведен упрощенный пример
        val avgPosition = 0.0 // Нужно вычислить на основе истории рейтинга

        return MetricResult(
            value = avgPosition,
            displayValue = formatNumber(avgPosition, 1),
            additionalData = emptyMap()
        )
    }
}

/**
 * Метрика для подсчета процента игр с положительными очками
 */
@Component
class PositivePointsPercentageMetric : PercentageMetric() {
    override val id = "positive_points_percentage"
    override val displayName = "Процент игр с положительными очками"
    override val description = "Процент игр, в которых игрок получил положительные очки"
    override val category = "Очки"

    override fun calculate(games: List<Game>, playerId: Long): MetricResult {
        val gamesWithPoints = games.filter { it.points != null }
        val positivePointsGames = gamesWithPoints.count { game ->
            val player = game.data.players?.find { it.player?.id == playerId } ?: return@count false
            val points = game.points?.players?.find { it.position == player.position.value }?.points ?: 0.0
            points > 0
        }

        val percentage = calculatePercentage(positivePointsGames, gamesWithPoints.size)

        return MetricResult(
            value = percentage,
            displayValue = formatPercentage(percentage),
            additionalData = mapOf(
                "positiveGames" to positivePointsGames,
                "totalGamesWithPoints" to gamesWithPoints.size
            )
        )
    }
}

/**
 * Метрика для подсчета любимой роли
 */
@Component
class FavoriteRoleMetric : NumericMetric() {
    override val id = "favorite_role"
    override val displayName = "Любимая роль"
    override val description = "Роль, за которую игрок играл чаще всего"
    override val category = "Роли"

    override fun calculate(games: List<Game>, playerId: Long): MetricResult {
        val roleCount = mutableMapOf<Role, Int>()

        games.forEach { game ->
            val player = game.data.players?.find { it.player?.id == playerId } ?: return@forEach
            roleCount[player.role] = roleCount.getOrDefault(player.role, 0) + 1
        }

        val favoriteRole = roleCount.maxByOrNull { it.value }
        val roleName = when (favoriteRole?.key) {
            Role.PEACE -> "Мирный"
            Role.MAFIA -> "Мафия"
            Role.DON -> "Дон"
            Role.SHERIFF -> "Шериф"
            else -> "Неизвестно"
        }

        return MetricResult(
            value = favoriteRole?.key ?: Role.PEACE,
            displayValue = "$roleName (${favoriteRole?.value ?: 0} игр)",
            additionalData = roleCount.mapKeys { it.key.name }
        )
    }
}
