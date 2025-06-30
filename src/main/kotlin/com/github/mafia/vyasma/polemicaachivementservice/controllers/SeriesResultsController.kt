package com.github.mafia.vyasma.polemicaachivementservice.controllers

import com.github.mafia.vyasma.polemica.library.client.PolemicaClient
import com.github.mafia.vyasma.polemicaachivementservice.rating.GamePointsService
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseBody

@Controller
@RequestMapping("/clubs/polemicaspb/leagues")
class SeriesResultsController(
    private val polemicaClient: PolemicaClient,
    private val gamePointsService: GamePointsService
) {
    private val competitions = listOf(
        PolemicaClient.PolemicaCompetition(
            3705,
            "PremierLeague",
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null
        ),
        PolemicaClient.PolemicaCompetition(
            3684,
            "ChampionshipLeague",
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null
        ),
        PolemicaClient.PolemicaCompetition(
            3710,
            "LeagueOne",
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null
        )
    )

    @GetMapping
    fun getSeriesResultsPage(model: Model): String {
        val tournamentsData = mutableListOf<TournamentSeriesData>()

        // Для каждого турнира получаем игры и серии
        for (competition in competitions) {
            val games = polemicaClient.getGamesFromCompetition(competition.id)
            val seriesMap = games.groupBy { (it.num.toInt() - 1) / 4 + 1 }

            // Последняя серия
            val lastSeriesNumber = seriesMap.keys.maxOrNull() ?: 1

            // Получаем результаты последней серии
            val lastSeriesResults = getSeriesResults(competition.id, lastSeriesNumber, seriesMap)

            tournamentsData.add(
                TournamentSeriesData(
                    competition = competition,
                    seriesNumbers = seriesMap.keys.sorted(),
                    currentSeries = lastSeriesNumber,
                    playerPoints = lastSeriesResults
                )
            )
        }

        model.addAttribute("tournamentsData", tournamentsData)
        return "series-results"
    }

    @GetMapping("/competition/{competitionId}/series/{seriesNumber}")
    @ResponseBody
    fun getSeriesResultsJson(
        @PathVariable competitionId: Long,
        @PathVariable seriesNumber: Int
    ): List<Pair<String, Double>> {
        val games = polemicaClient.getGamesFromCompetition(competitionId)
        val seriesMap = games.groupBy { (it.num.toInt() - 1) / 4 + 1 }
        return getSeriesResults(competitionId, seriesNumber, seriesMap)
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
    val playerPoints: List<Pair<String, Double>>
)
