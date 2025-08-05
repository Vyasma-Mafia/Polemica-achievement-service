package com.github.mafia.vyasma.polemicaachivementservice.statistics

import com.github.mafia.vyasma.polemicaachivementservice.repositories.GameRepository
import com.github.mafia.vyasma.polemicaachivementservice.statistics.metrics.MetricResult
import com.github.mafia.vyasma.polemicaachivementservice.statistics.metrics.StatisticsMetric
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class MetricsService(
    private val gameRepository: GameRepository,
    private val metrics: List<StatisticsMetric>
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    /**
     * Получить все доступные метрики
     */
    fun getAvailableMetrics(): List<MetricInfo> {
        return metrics.map { metric ->
            MetricInfo(
                id = metric.id,
                displayName = metric.displayName,
                description = metric.description,
                category = metric.category
            )
        }
    }

    /**
     * Вычислить конкретную метрику для игрока
     */
    fun calculateMetric(playerId: Long, metricId: String): MetricResult? {
        val metric = metrics.find { it.id == metricId } ?: return null

        val playerGames = gameRepository.findAll()
            .filter { game ->
                game.data.players?.any { it.player?.id == playerId } ?: false
            }

        return try {
            metric.calculate(playerGames, playerId)
        } catch (e: Exception) {
            logger.error("Error calculating metric $metricId for player $playerId", e)
            null
        }
    }

    /**
     * Вычислить все метрики для игрока
     */
    fun calculateAllMetrics(playerId: Long): Map<String, MetricResult> {
        val playerGames = gameRepository.findAll()
            .filter { game ->
                game.data.players?.any { it.player?.id == playerId } ?: false
            }

        return metrics.mapNotNull { metric ->
            try {
                val result = metric.calculate(playerGames, playerId)
                metric.id to result
            } catch (e: Exception) {
                logger.error("Error calculating metric ${metric.id} for player $playerId", e)
                null
            }
        }.toMap()
    }

    /**
     * Вычислить метрики по категориям
     */
    fun calculateMetricsByCategory(playerId: Long, category: String): Map<String, MetricResult> {
        val categoryMetrics = metrics.filter { it.category == category }
        val playerGames = gameRepository.findAll()
            .filter { game ->
                game.data.players?.any { it.player?.id == playerId } ?: false
            }

        return categoryMetrics.mapNotNull { metric ->
            try {
                val result = metric.calculate(playerGames, playerId)
                metric.id to result
            } catch (e: Exception) {
                logger.error("Error calculating metric ${metric.id} for player $playerId", e)
                null
            }
        }.toMap()
    }
}

data class MetricInfo(
    val id: String,
    val displayName: String,
    val description: String,
    val category: String
)
