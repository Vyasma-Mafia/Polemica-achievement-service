package com.github.mafia.vyasma.polemicaachivementservice.statistics.metrics

import com.github.mafia.vyasma.polemicaachivementservice.model.jpa.Game

/**
 * Интерфейс для создания пользовательских метрик статистики
 */
interface StatisticsMetric {
    /**
     * Уникальный идентификатор метрики
     */
    val id: String

    /**
     * Отображаемое название метрики
     */
    val displayName: String

    /**
     * Описание метрики
     */
    val description: String

    /**
     * Категория метрики (например, "Общие", "Роли", "Достижения")
     */
    val category: String

    /**
     * Вычисляет значение метрики для игрока
     * @param games Список игр игрока
     * @param playerId ID игрока
     * @return Результат вычисления метрики
     */
    fun calculate(games: List<Game>, playerId: Long): MetricResult
}

/**
 * Результат вычисления метрики
 */
data class MetricResult(
    val value: Any,
    val displayValue: String,
    val additionalData: Map<String, Any> = emptyMap()
)

/**
 * Базовый класс для числовых метрик
 */
abstract class NumericMetric : StatisticsMetric {
    protected fun formatNumber(value: Number, decimals: Int = 2): String {
        return when (value) {
            is Double -> "%.${decimals}f".format(value)
            is Float -> "%.${decimals}f".format(value)
            else -> value.toString()
        }
    }
}

/**
 * Базовый класс для процентных метрик
 */
abstract class PercentageMetric : NumericMetric() {
    protected fun calculatePercentage(part: Int, total: Int): Double {
        return if (total > 0) (part.toDouble() / total) * 100 else 0.0
    }

    protected fun formatPercentage(value: Double): String {
        return "${formatNumber(value, 1)}%"
    }
}
