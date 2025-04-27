package com.github.mafia.vyasma.polemicaachivementservice.controllers

import com.github.mafia.vyasma.polemica.library.model.game.PolemicaGameResult
import com.github.mafia.vyasma.polemica.library.utils.isBlack
import com.github.mafia.vyasma.polemica.library.utils.isRed
import com.github.mafia.vyasma.polemicaachivementservice.model.jpa.PlayerRatingHistory
import com.github.mafia.vyasma.polemicaachivementservice.model.jpa.RecalibrationHistory
import com.github.mafia.vyasma.polemicaachivementservice.repositories.GameRepository
import com.github.mafia.vyasma.polemicaachivementservice.repositories.PlayerRatingHistoryRepository
import com.github.mafia.vyasma.polemicaachivementservice.repositories.RecalibrationHistoryRepository
import com.github.mafia.vyasma.polemicaachivementservice.repositories.UserRepository
import jakarta.persistence.EntityNotFoundException
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Controller
@RequestMapping("/rating")
class RatingController(
    private val userRepository: UserRepository,
    private val ratingHistoryRepository: PlayerRatingHistoryRepository,
    private val gameRepository: GameRepository,
    private val playerRatingHistoryRepository: PlayerRatingHistoryRepository,
    private val recalibrationHistoryRepository: RecalibrationHistoryRepository
) {

    val logger = LoggerFactory.getLogger(this::class.java)

    @GetMapping
    fun getRatings(
        model: Model,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @RequestParam(required = false) search: String?
    ): String {
        val pageable = PageRequest.of(page, size, Sort.by("rating").descending())

        // Добавляем поддержку поиска
        val players = if (search.isNullOrBlank()) {
            userRepository.findAll(pageable)
        } else {
            userRepository.findByUsernameContainingIgnoreCase(search, pageable)
        }

        // Расчет общей статистики
        val totalPlayers = userRepository.count()
        val totalGames = gameRepository.count()
        val averageRating = userRepository.averageRating()  // Можно рассчитать среднее значение рейтингов, если нужно
        val topPlayer = userRepository.findTopByOrderByRatingDesc()!!

        // Распределение рейтинга
        val ratingRanges = listOf("800-900", "900-950", "950-1000", "1000-1050", "1050-1100", "1100-1200", ">1200")
        val ratingDistribution = listOf(5, 10, 15, 25, 20, 15, 10)

        model.addAttribute("players", players.content)
        model.addAttribute("currentPage", players.number)
        model.addAttribute("totalPages", players.totalPages)
        model.addAttribute("totalElements", players.totalElements)
        model.addAttribute("totalPlayers", totalPlayers)
        model.addAttribute("totalGames", totalGames)
        model.addAttribute("averageRating", averageRating)
        model.addAttribute("topPlayer", topPlayer)
        model.addAttribute("ratingLabels", ratingRanges)
        model.addAttribute("ratingDistribution", ratingDistribution)

        return "ratings-view"
    }

    @GetMapping("/player/{userId}")
    fun getPlayerHistory(
        @PathVariable userId: Long,
        model: Model,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @RequestParam(required = false, defaultValue = "all") timeRange: String
    ): String {
        try {
            val player = userRepository.findById(userId)
                .orElseThrow { EntityNotFoundException("Игрок не найден") }

            // Получаем историю рейтинга для пагинации
            val pageable = PageRequest.of(page, size, Sort.by("timestamp").descending())
            val history = ratingHistoryRepository.findByPlayer(player, pageable)

            // Получаем данные для графика с ограничением по времени
            val fromDate = when (timeRange) {
                "week" -> LocalDateTime.now().minusWeeks(1)
                "month" -> LocalDateTime.now().minusMonths(1)
                "year" -> LocalDateTime.now().minusYears(1)
                else -> null // Без ограничений
            }

            val chartHistory = if (fromDate != null) {
                try {
                    ratingHistoryRepository.findByPlayerAndTimestampAfterOrderByTimestampAsc(player, fromDate)
                } catch (e: Exception) {
                    // Если метод не существует или выбросил исключение, запрашиваем все данные
                    println("Error fetching filtered history: ${e.message}")
                    ratingHistoryRepository.findByPlayerOrderByTimestampAsc(player)
                }
            } else {
                ratingHistoryRepository.findByPlayerOrderByTimestampAsc(player)
            }

            // Подготавливаем данные для графика
            val chartData = prepareChartData(chartHistory)

            // Добавляем все необходимые атрибуты в модель
            model.addAttribute("player", player)
            model.addAttribute("ratingHistory", history.content)
            model.addAttribute("currentPage", history.number)
            model.addAttribute("totalPages", history.totalPages)
            model.addAttribute("timeRange", timeRange)

            // Логируем данные для отладки
            logger.debug("Chart data size: ${chartData.first.size}, ${chartData.second.size}, ${chartData.third.size}")

            // Данные для графика
            model.addAttribute("ratingLabels", chartData.first)  // Даты
            model.addAttribute("ratingValues", chartData.second) // Значения рейтинга
            model.addAttribute("pointsValues", chartData.third)  // Значения очков (для второй линии)

            // Собираем данные для аннотаций с обработкой возможных ошибок
            try {
                // Получаем рекалибровки и другие данные для аннотаций
                val recalibrations = recalibrationHistoryRepository.findByPlayerOrderByTimestampDesc(player)

                // Находим турнирные игры (с обработкой возможных ошибок)
                val competitiveGames = try {
                    ratingHistoryRepository.findByPlayerAndCompetitive(
                        player, true,
                        Sort.by("timestamp").ascending()
                    )
                } catch (e: Exception) {
                    emptyList<PlayerRatingHistory>()
                }

                // Находим лучшие и худшие игры (с обработкой возможных ошибок)
                val bestGames = try {
                    ratingHistoryRepository.findByPlayerOrderByPointsEarnedDesc(player)
                        .take(3)
                } catch (e: Exception) {
                    emptyList<PlayerRatingHistory>()
                }

                val worstGames = try {
                    ratingHistoryRepository.findByPlayerOrderByPointsEarnedAsc(player)
                        .take(3)
                } catch (e: Exception) {
                    emptyList<PlayerRatingHistory>()
                }

                // Готовим аннотации с проверкой наличия данных
                if (chartData.first.isNotEmpty()) {
                    val chartAnnotations = prepareChartAnnotations(
                        chartData.first,    // метки времени
                        recalibrations,     // рекалибровки
                        competitiveGames,   // турнирные игры
                        bestGames,          // лучшие игры
                        worstGames          // худшие игры
                    )
                    model.addAttribute("chartAnnotations", chartAnnotations)
                }
            } catch (e: Exception) {
                println("Error preparing annotations: ${e.message}")
                // В случае ошибки, добавляем пустые аннотации
                model.addAttribute("chartAnnotations", emptyMap<String, Any>())
            }

            return "player-history-view"
        } catch (e: Exception) {
            println("Error in player history: ${e.message}")
            e.printStackTrace()
            throw e
        }
    }

    /**
     * Подготавливает аннотации для графика
     */
    private fun prepareChartAnnotations(
        timestamps: List<String>,
        recalibrations: List<RecalibrationHistory>,
        competitiveGames: List<PlayerRatingHistory>,
        bestGames: List<PlayerRatingHistory>,
        worstGames: List<PlayerRatingHistory>
    ): Map<String, Any> {
        val annotations = mutableMapOf<String, Any>()

        // Находим индексы для аннотаций
        val dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yy")

        // Добавляем аннотации для рекалибровок
        recalibrations.forEachIndexed { idx, recal ->
            val dateStr = recal.timestamp.format(dateFormatter)
            val index = timestamps.indexOf(dateStr)
            if (index >= 0) {
                annotations["recal$idx"] = mapOf(
                    "type" to "line",
                    "xMin" to index,
                    "xMax" to index,
                    "borderColor" to "rgba(255, 159, 64, 0.8)",
                    "borderWidth" to 2,
                    "label" to mapOf(
                        "content" to "Рекалибровка",
                        "enabled" to true,
                        "position" to "top"
                    )
                )
            }
        }

        // Добавляем аннотации для турнирных игр
        // competitiveGames.forEachIndexed { idx, game ->
        //     val dateStr = game.timestamp.format(dateFormatter)
        //     val index = timestamps.indexOf(dateStr)
        //     if (index >= 0) {
        //         annotations["tournament$idx"] = mapOf(
        //             "type" to "point",
        //             "xValue" to index,
        //             "yValue" to (game.newMu - 3 * game.newSigma),
        //             "backgroundColor" to "rgba(255, 99, 132, 1)",
        //             "radius" to 5,
        //             "label" to mapOf(
        //                 "content" to "Турнир",
        //                 "enabled" to true,
        //                 "position" to "top"
        //             )
        //         )
        //     }
        // }

        // Добавляем аннотации для лучших игр
        // bestGames.forEachIndexed { idx, game ->
        //     val dateStr = game.timestamp.format(dateFormatter)
        //     val index = timestamps.indexOf(dateStr)
        //     if (index >= 0) {
        //         annotations["best$idx"] = mapOf(
        //             "type" to "point",
        //             "xValue" to index,
        //             "yValue" to (game.newMu - 3 * game.newSigma),
        //             "backgroundColor" to "rgba(75, 192, 192, 1)",
        //             "radius" to 6,
        //             "label" to mapOf(
        //                 "content" to "Лучшая игра",
        //                 "enabled" to (idx == 0), // Показываем метку только для самой лучшей игры
        //                 "position" to "top"
        //             )
        //         )
        //     }
        // }

        return annotations
    }

    /**
     * Подготавливает данные для графика истории рейтинга
     * @return Triple с метками дат, значениями рейтинга и значениями очков
     */
    private fun prepareChartData(history: List<PlayerRatingHistory>): Triple<List<String>, List<Double>, List<Double>> {
        // Пустые данные, если история пуста
        if (history.isEmpty()) {
            return Triple(emptyList(), emptyList(), emptyList())
        }

        // Группируем записи по дате и берем последнюю запись за каждый день
        // (чтобы не перегружать график большим количеством точек)
        val groupedByDay = history.groupBy {
            it.timestamp.toLocalDate().toString()
        }.mapValues { (_, records) ->
            records.maxByOrNull { it.timestamp }!!
        }

        // Если записей слишком много (>100), делаем дополнительное прореживание
        val chartData = if (groupedByDay.size > 100) {
            val step = groupedByDay.size / 100 + 1
            groupedByDay.values.filterIndexed { index, _ -> index % step == 0 }
        } else {
            groupedByDay.values.toList()
        }

        // Сортируем по времени
        val sortedData = chartData.sortedBy { it.timestamp }

        // Форматируем даты и извлекаем значения рейтинга и очков
        val dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yy")

        val labels = sortedData.map {
            it.timestamp.format(dateFormatter)
        }

        val ratings = sortedData.map {
            it.newMu - 3 * it.newSigma
        }

        val points = sortedData.map {
            it.pointsEarned
        }

        return Triple(labels, ratings, points)
    }

    @GetMapping("/games/{gameId}")
    fun getGameResults(@PathVariable gameId: Long, model: Model): String {
        // Получаем данные об игре
        val game = gameRepository.findById(gameId)
            .orElseThrow { EntityNotFoundException("Игра с ID $gameId не найдена") }

        // Получаем историю изменения рейтинга для этой игры
        val ratingChanges = playerRatingHistoryRepository.findByGameId(gameId)

        // Сортируем по позиции игрока в игре
        val sortedRatingChanges = ratingChanges.sortedBy {
            game.points?.players?.find { pts -> pts.position == it.player.userId.toInt() }?.position ?: Int.MAX_VALUE
        }

        // Определяем команды
        val mafiaTeam = sortedRatingChanges.filter { history ->
            game.data.players?.find { history.player.userId == it.player }?.role?.isBlack() == true
        }

        val civilianTeam = sortedRatingChanges.filter { history ->
            game.data.players?.find { history.player.userId == it.player }?.role?.isRed() == true
        }

        // Определяем победителя
        val winnerTeam = if (game.data.result == PolemicaGameResult.BLACK_WIN) "Мафия" else "Мирные"

        // Статистика игры
        val gameStats = mapOf(
            "winnerTeam" to winnerTeam,
            "totalPlayers" to sortedRatingChanges.size,
            "avgRatingChange" to sortedRatingChanges.map { it.muDelta }.average(),
            "competitive" to (sortedRatingChanges.firstOrNull()?.competitive ?: false),
            "gameDate" to game.started
        )

        // Лучший игрок (по очкам)
        val bestPlayer = sortedRatingChanges.maxByOrNull { it.pointsEarned }

        model.addAttribute("game", game)
        model.addAttribute("gameStats", gameStats)
        model.addAttribute("mafiaTeam", mafiaTeam)
        model.addAttribute("civilianTeam", civilianTeam)
        model.addAttribute("bestPlayer", bestPlayer)
        model.addAttribute("allPlayers", sortedRatingChanges)

        return "game-results"
    }
}
