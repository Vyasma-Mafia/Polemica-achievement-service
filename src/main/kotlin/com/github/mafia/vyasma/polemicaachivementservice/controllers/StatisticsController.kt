package com.github.mafia.vyasma.polemicaachivementservice.controllers

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.mafia.vyasma.polemica.library.model.game.Role
import com.github.mafia.vyasma.polemica.library.utils.isBlack
import com.github.mafia.vyasma.polemica.library.utils.isBlackWin
import com.github.mafia.vyasma.polemica.library.utils.isRed
import com.github.mafia.vyasma.polemica.library.utils.isRedWin
import com.github.mafia.vyasma.polemicaachivementservice.filters.FilterInput
import com.github.mafia.vyasma.polemicaachivementservice.filters.GameFilterService
import com.github.mafia.vyasma.polemicaachivementservice.model.statistics.FilteredGame
import com.github.mafia.vyasma.polemicaachivementservice.model.statistics.FilteredGamesResponse
import com.github.mafia.vyasma.polemicaachivementservice.model.statistics.PairGamesResponse
import com.github.mafia.vyasma.polemicaachivementservice.model.statistics.PairStatistics
import com.github.mafia.vyasma.polemicaachivementservice.model.statistics.PlayerSearchResult
import com.github.mafia.vyasma.polemicaachivementservice.model.statistics.PlayerStatistics
import com.github.mafia.vyasma.polemicaachivementservice.model.statistics.TopPartners
import com.github.mafia.vyasma.polemicaachivementservice.repositories.GameRepository
import com.github.mafia.vyasma.polemicaachivementservice.repositories.PlayerRatingHistoryRepository
import com.github.mafia.vyasma.polemicaachivementservice.repositories.UserRepository
import com.github.mafia.vyasma.polemicaachivementservice.statistics.MetricsService
import com.github.mafia.vyasma.polemicaachivementservice.statistics.PairStatisticsService
import com.github.mafia.vyasma.polemicaachivementservice.statistics.PlayerStatisticsService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@RestController
@RequestMapping("/api")
@Tag(name = "Statistics", description = "API для получения статистики игроков")
class StatisticsController(
    private val playerStatisticsService: PlayerStatisticsService,
    private val pairStatisticsService: PairStatisticsService,
    private val metricsService: MetricsService,
    private val gameFilterService: GameFilterService,
    private val gameRepository: GameRepository,
    private val userRepository: UserRepository,
    private val playerRatingHistoryRepository: PlayerRatingHistoryRepository,
    private val objectMapper: ObjectMapper
) {

    @GetMapping("/players/search")
    @Operation(
        summary = "Поиск игроков по нику",
        description = "Возвращает список игроков, чьи ники содержат указанную строку"
    )
    @ApiResponses(
        ApiResponse(
            responseCode = "200",
            description = "Список найденных игроков",
            content = [Content(schema = Schema(implementation = PlayerSearchResult::class))]
        )
    )
    fun searchPlayers(
        @Parameter(description = "Строка для поиска (минимум 2 символа)")
        @RequestParam query: String
    ): ResponseEntity<List<PlayerSearchResult>> {
        if (query.length < 2) {
            return ResponseEntity.ok(emptyList())
        }

        val results = playerStatisticsService.searchPlayers(query)
        return ResponseEntity.ok(results)
    }

    @GetMapping("/players/{playerId}/statistics")
    @Operation(
        summary = "Получить статистику игрока",
        description = "Возвращает подробную статистику игрока по ролям"
    )
    @ApiResponses(
        ApiResponse(
            responseCode = "200",
            description = "Статистика игрока",
            content = [Content(schema = Schema(implementation = PlayerStatistics::class))]
        ),
        ApiResponse(
            responseCode = "404",
            description = "Игрок не найден"
        )
    )
    fun getPlayerStatistics(
        @Parameter(description = "ID игрока")
        @PathVariable playerId: Long
    ): ResponseEntity<PlayerStatistics> {
        val statistics = playerStatisticsService.getPlayerStatistics(playerId)
            ?: return ResponseEntity.notFound().build()

        return ResponseEntity.ok(statistics)
    }

    @GetMapping("/players/{player1Id}/pair-statistics/{player2Id}")
    @Operation(
        summary = "Получить парную статистику",
        description = "Возвращает статистику совместных игр двух игроков"
    )
    @ApiResponses(
        ApiResponse(
            responseCode = "200",
            description = "Парная статистика",
            content = [Content(schema = Schema(implementation = PairStatistics::class))]
        ),
        ApiResponse(
            responseCode = "404",
            description = "Один из игроков не найден"
        )
    )
    fun getPairStatistics(
        @Parameter(description = "ID первого игрока")
        @PathVariable player1Id: Long,
        @Parameter(description = "ID второго игрока")
        @PathVariable player2Id: Long
    ): ResponseEntity<PairStatistics> {
        val statistics = pairStatisticsService.getPairStatistics(player1Id, player2Id)
            ?: return ResponseEntity.notFound().build()

        return ResponseEntity.ok(statistics)
    }

    @GetMapping("/players/{player1Id}/games-with/{player2Id}")
    @Operation(
        summary = "Получить список совместных игр",
        description = "Возвращает список всех игр, в которых участвовали оба игрока"
    )
    @ApiResponses(
        ApiResponse(
            responseCode = "200",
            description = "Список совместных игр",
            content = [Content(schema = Schema(implementation = PairGamesResponse::class))]
        ),
        ApiResponse(
            responseCode = "404",
            description = "Один из игроков не найден"
        )
    )
    fun getCommonGames(
        @Parameter(description = "ID первого игрока")
        @PathVariable player1Id: Long,
        @Parameter(description = "ID второго игрока")
        @PathVariable player2Id: Long,
        @Parameter(description = "Номер страницы (начиная с 0)")
        @RequestParam(defaultValue = "0") page: Int,
        @Parameter(description = "Размер страницы")
        @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<PairGamesResponse> {
        val games = pairStatisticsService.getCommonGames(player1Id, player2Id, page, size)
            ?: return ResponseEntity.notFound().build()

        return ResponseEntity.ok(games)
    }

    @GetMapping("/filters")
    @Operation(
        summary = "Получить список доступных фильтров",
        description = "Возвращает список всех доступных фильтров для игр"
    )
    fun getAvailableFilters(): ResponseEntity<List<Map<String, Any>>> {
        val filters = gameFilterService.getAllFilters().map { filter ->
            mapOf(
                "id" to filter.id,
                "name" to filter.name,
                "description" to filter.description,
                "hasParameters" to filter.hasParameters
            )
        }
        return ResponseEntity.ok(filters)
    }

    @GetMapping("/metrics")
    @Operation(
        summary = "Получить список доступных метрик",
        description = "Возвращает список всех доступных метрик для расчета статистики"
    )
    fun getAvailableMetrics(): ResponseEntity<List<Any>> {
        val metrics = metricsService.getAvailableMetrics()
        return ResponseEntity.ok(metrics)
    }

    @GetMapping("/players/{playerId}/metrics")
    @Operation(
        summary = "Получить все метрики игрока",
        description = "Возвращает значения всех метрик для указанного игрока"
    )
    fun getPlayerMetrics(
        @Parameter(description = "ID игрока")
        @PathVariable playerId: Long
    ): ResponseEntity<Map<String, Any>> {
        val metrics = metricsService.calculateAllMetrics(playerId)
        return ResponseEntity.ok(metrics)
    }

    @GetMapping("/players/{playerId}/metrics/{metricId}")
    @Operation(
        summary = "Получить конкретную метрику игрока",
        description = "Возвращает значение указанной метрики для игрока"
    )
    fun getPlayerMetric(
        @Parameter(description = "ID игрока")
        @PathVariable playerId: Long,
        @Parameter(description = "ID метрики")
        @PathVariable metricId: String
    ): ResponseEntity<Any> {
        val metric = metricsService.calculateMetric(playerId, metricId)
            ?: return ResponseEntity.notFound().build()

        return ResponseEntity.ok(metric)
    }

    @GetMapping("/players/{playerId}/top-partners")
    @Operation(
        summary = "Получить топ напарников игрока",
        description = "Возвращает списки лучших и худших напарников игрока по проценту побед"
    )
    @ApiResponses(
        ApiResponse(
            responseCode = "200",
            description = "Топ напарников",
            content = [Content(schema = Schema(implementation = TopPartners::class))]
        ),
        ApiResponse(
            responseCode = "404",
            description = "Игрок не найден"
        )
    )
    fun getTopPartners(
        @Parameter(description = "ID игрока")
        @PathVariable playerId: Long,
        @Parameter(description = "Минимальное количество совместных игр")
        @RequestParam(required = false) minGames: Int?,
        @Parameter(description = "Количество напарников в каждой категории")
        @RequestParam(required = false) topCount: Int?
    ): ResponseEntity<TopPartners> {
        val topPartners = pairStatisticsService.getTopPartners(playerId, minGames, topCount)
            ?: return ResponseEntity.notFound().build()

        return ResponseEntity.ok(topPartners)
    }

    @GetMapping("/players/{playerId}/games")
    @Operation(
        summary = "Получить отфильтрованные игры игрока",
        description = "Возвращает список игр игрока с применением указанных фильтров"
    )
    @ApiResponses(
        ApiResponse(
            responseCode = "200",
            description = "Отфильтрованные игры",
            content = [Content(schema = Schema(implementation = FilteredGamesResponse::class))]
        ),
        ApiResponse(
            responseCode = "404",
            description = "Игрок не найден"
        )
    )
    fun getFilteredGames(
        @Parameter(description = "ID игрока")
        @PathVariable playerId: Long,
        @Parameter(description = "JSON строка с фильтрами")
        @RequestParam(required = false) filters: String?
    ): ResponseEntity<FilteredGamesResponse> {
        val player = userRepository.findByIdOrNull(playerId) ?: return ResponseEntity.notFound().build()

        // Get all player games
        val allGames = gameRepository.findAllByUserJoinedFromPlayerRatingHistory(player)
            .filter { game ->
                game.data.players?.any { it.player?.id == playerId } ?: false
            }

        // Parse filters from JSON string
        val activeFilters = parseFilters(filters)

        // Apply filters
        val filteredGames = gameFilterService.applyFilters(allGames, player, activeFilters)

        // Get rating history for each game
        val ratingHistoryMap = playerRatingHistoryRepository.findAllByPlayer(player)
            .associateBy { it.gameId }

        // Convert to FilteredGame
        val filteredGameList = filteredGames.map { game ->
            val playerData = game.data.players?.find { it.player?.id == playerId }
            val role = playerData?.role ?: Role.PEACE
            val isWin = when {
                role.isRed() && game.data.isRedWin() -> true
                role.isBlack() && game.data.isBlackWin() -> true
                else -> false
            }
            val points = game.points?.players?.find { it.position == playerData?.position?.value }?.points
            val history = ratingHistoryMap[game.gameId]

            FilteredGame(
                gameId = game.gameId,
                date = game.started,
                role = when (role) {
                    Role.PEACE -> "Мирный"
                    Role.MAFIA -> "Мафия"
                    Role.DON -> "Дон"
                    Role.SHERIFF -> "Шериф"
                },
                points = points,
                isWin = isWin,
                result = if (isWin) "Победа" else "Поражение",
                competitive = history?.competitive ?: false,
                oldRating = (history?.oldMu ?: 0.0) - 3 * (history?.oldSigma ?: 0.0),
                ratingChange = history?.muDelta ?: 0.0,
                newRating = (history?.newMu ?: 0.0) - 3 * (history?.newSigma ?: 0.0),
                weight = history?.weight ?: 0.0
            )
        }.sortedByDescending { it.date }

        // Build active filters map for response (filter ID to filter name)
        val activeFiltersMap = activeFilters.keys.associateWith { filterId ->
            gameFilterService.getFilterById(filterId)?.name ?: filterId
        }

        val response = FilteredGamesResponse(
            playerId = playerId,
            username = player.username,
            games = filteredGameList,
            totalCount = allGames.size,
            filteredCount = filteredGameList.size,
            activeFilters = activeFiltersMap
        )

        return ResponseEntity.ok(response)
    }

    private fun parseFilters(filtersJson: String?): Map<String, FilterInput> {
        if (filtersJson.isNullOrBlank()) {
            return emptyMap()
        }

        return try {
            val filtersMap = objectMapper.readValue(filtersJson, Map::class.java) as Map<*, *>
            filtersMap.mapNotNull { (filterId, filterData) ->
                val id = filterId as? String ?: return@mapNotNull null

                val input = when (id) {
                    "dateRange" -> {
                        val data = filterData as? Map<*, *> ?: return@mapNotNull null
                        val fromStr = data["from"] as? String
                        val toStr = data["to"] as? String
                        FilterInput.DateRangeInput(
                            from = fromStr?.let { LocalDate.parse(it) },
                            to = toStr?.let { LocalDate.parse(it) }
                        )
                    }

                    "role" -> {
                        val roleStr = filterData as? String ?: return@mapNotNull null
                        val role = try {
                            Role.valueOf(roleStr.uppercase())
                        } catch (e: IllegalArgumentException) {
                            return@mapNotNull null
                        }
                        FilterInput.RoleInput(role)
                    }

                    "winLoss" -> {
                        val isWin = filterData as? Boolean?
                        FilterInput.WinLossInput(isWin)
                    }

                    "pointsRange" -> {
                        val data = filterData as? Map<*, *> ?: return@mapNotNull null
                        val min = (data["min"] as? Number)?.toDouble()
                        val max = (data["max"] as? Number)?.toDouble()
                        FilterInput.PointsRangeInput(min, max)
                    }

                    "firstKilled" -> {
                        FilterInput.NoInput
                    }

                    else -> return@mapNotNull null
                }

                id to input
            }.toMap()
        } catch (e: Exception) {
            emptyMap()
        }
    }

    @ExceptionHandler(Exception::class)
    fun handleException(e: Exception): ResponseEntity<Map<String, String>> {
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(mapOf("error" to (e.message ?: "Произошла ошибка")))
    }
}
