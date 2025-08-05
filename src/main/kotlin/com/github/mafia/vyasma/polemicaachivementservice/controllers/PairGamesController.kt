package com.github.mafia.vyasma.polemicaachivementservice.controllers

import com.github.mafia.vyasma.polemicaachivementservice.statistics.PairStatisticsService
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam

@Controller
@RequestMapping("/rating/players")
class PairGamesController(
    private val pairStatisticsService: PairStatisticsService
) {

    @GetMapping("/{player1Id}/games-with/{player2Id}")
    fun getPairGames(
        @PathVariable player1Id: Long,
        @PathVariable player2Id: Long,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        model: Model
    ): String {
        val pairGames = pairStatisticsService.getCommonGames(player1Id, player2Id, page, size)
            ?: return "error/404"

        val pairStatistics = pairStatisticsService.getPairStatistics(player1Id, player2Id)
            ?: return "error/404"

        model.addAttribute("firstPlayer", pairGames.firstPlayer)
        model.addAttribute("secondPlayer", pairGames.secondPlayer)
        model.addAttribute("games", pairGames.games)
        model.addAttribute("statistics", pairStatistics)
        model.addAttribute("currentPage", page)
        model.addAttribute("totalPages", (pairGames.totalCount + size - 1) / size)
        model.addAttribute("totalGames", pairGames.totalCount)

        return "pair-games-view"
    }
}
