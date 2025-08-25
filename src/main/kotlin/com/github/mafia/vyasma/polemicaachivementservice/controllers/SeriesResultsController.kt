package com.github.mafia.vyasma.polemicaachivementservice.controllers

import com.github.mafia.vyasma.polemica.library.client.PolemicaClient
import com.github.mafia.vyasma.polemicaachivementservice.model.dto.AddTournamentRequest
import com.github.mafia.vyasma.polemicaachivementservice.model.dto.TournamentResponse
import com.github.mafia.vyasma.polemicaachivementservice.rating.GamePointsService
import com.github.mafia.vyasma.polemicaachivementservice.services.TournamentService
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseBody

@Controller
@RequestMapping("/clubs/polemicaspb/leagues")
class SeriesResultsController(
    private val polemicaClient: PolemicaClient,
    private val gamePointsService: GamePointsService,
    private val tournamentService: TournamentService
) {
    private val logger = LoggerFactory.getLogger(SeriesResultsController::class.java)

    @GetMapping
    fun getSeriesResultsPage(model: Model): String {
        val tournamentsData = mutableListOf<TournamentSeriesData>()
        val tournaments = tournamentService.getActiveTournaments()

        // Для каждого турнира получаем игры и серии
        for (tournament in tournaments) {
            try {
                val games = polemicaClient.getGamesFromCompetition(tournament.id)
                val seriesMap = games.groupBy { (it.num.toInt() - 1) / tournament.gamesPerSeries + 1 }

                // Последняя серия
                val lastSeriesNumber = seriesMap.keys.maxOrNull() ?: 1

                // Получаем результаты последней серии
                val lastSeriesResults = getSeriesResults(tournament.id, lastSeriesNumber, seriesMap)

                // Создаем PolemicaCompetition для совместимости с существующим шаблоном
                val competition = PolemicaClient.PolemicaCompetition(
                    tournament.id,
                    tournament.name,
                    null, null, null, null, null, null, null, null, null, null, null, null, null, null
                )

                tournamentsData.add(
                    TournamentSeriesData(
                        competition = competition,
                        seriesNumbers = seriesMap.keys.sorted(),
                        currentSeries = lastSeriesNumber,
                        playerPoints = lastSeriesResults,
                        gamesPerSeries = tournament.gamesPerSeries
                    )
                )
            } catch (e: Exception) {
                logger.error("Ошибка при загрузке данных турнира ${tournament.name} (ID: ${tournament.id}): ${e.message}")
                // Пропускаем турнир с ошибкой, но продолжаем обработку остальных
            }
        }

        model.addAttribute("tournamentsData", tournamentsData)
        model.addAttribute("activeTournaments", tournamentService.getActiveTournamentsAsResponse())
        return "series-results"
    }

    @GetMapping("/competition/{competitionId}/series/{seriesNumber}")
    @ResponseBody
    fun getSeriesResultsJson(
        @PathVariable competitionId: Long,
        @PathVariable seriesNumber: Int
    ): List<Pair<String, Double>> {
        val tournament = tournamentService.getTournamentById(competitionId)
            ?: throw IllegalArgumentException("Турнир с ID $competitionId не найден")
        
        val games = polemicaClient.getGamesFromCompetition(competitionId)
        val seriesMap = games.groupBy { (it.num.toInt() - 1) / tournament.gamesPerSeries + 1 }
        return getSeriesResults(competitionId, seriesNumber, seriesMap)
    }

    @PostMapping("/add-tournament")
    @ResponseBody
    fun addTournament(@RequestBody request: AddTournamentRequest): ResponseEntity<TournamentResponse> {
        return try {
            val tournament = tournamentService.addTournament(request)
            ResponseEntity.ok(tournament)
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().build()
        } catch (e: Exception) {
            logger.error("Ошибка при добавлении турнира: ${e.message}", e)
            ResponseEntity.internalServerError().build()
        }
    }

    @DeleteMapping("/remove-tournament/{id}")
    @ResponseBody
    fun removeTournament(@PathVariable id: Long): ResponseEntity<Map<String, String>> {
        return try {
            tournamentService.removeTournament(id)
            ResponseEntity.ok(mapOf("message" to "Турнир успешно удален"))
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(mapOf("error" to e.message!!))
        } catch (e: Exception) {
            logger.error("Ошибка при удалении турнира: ${e.message}", e)
            ResponseEntity.internalServerError().body(mapOf("error" to "Внутренняя ошибка сервера"))
        }
    }

    @GetMapping("/tournaments")
    @ResponseBody
    fun getActiveTournaments(): List<TournamentResponse> {
        return tournamentService.getActiveTournamentsAsResponse()
    }

    private fun getSeriesResults(
        competitionId: Long,
        seriesNumber: Int,
        seriesMap: Map<Int, List<PolemicaClient.PolemicaTournamentGameReference>>
    ): List<Pair<String, Double>> {
        val seriesGames = seriesMap[seriesNumber] ?: emptyList()
        val playerPointsMap = mutableMapOf<String, Double>()

        for (gameRef in seriesGames) {
            val game = polemicaClient.getGameFromCompetition(
                PolemicaClient.PolemicaCompetitionGameId(
                    competitionId,
                    gameRef.id,
                    gameRef.version
                )
            )
            val gameId = game.id ?: continue
            val playerPoints = gamePointsService.fetchPlayerStats(gameId)

            game.players?.forEachIndexed { index, player ->
                val points = playerPoints.find { it.position == index + 1 }?.points ?: 0.0
                // Округление до 3 знаков без лишних нулей в конце
                playerPointsMap[player.username] = playerPointsMap.getOrDefault(player.username, 0.0) + points
            }
        }

        return playerPointsMap.entries
            .sortedByDescending { it.value }
            .map {
                // Округляем итоговые суммы до 3 знаков
                val roundedTotal = Math.round(it.value * 1000) / 1000.0
                Pair(it.key, roundedTotal)
            }
    }
}

// Класс для передачи данных в представление
data class TournamentSeriesData(
    val competition: PolemicaClient.PolemicaCompetition,
    val seriesNumbers: List<Int>,
    val currentSeries: Int,
    val playerPoints: List<Pair<String, Double>>,
    val gamesPerSeries: Int = 4
)
