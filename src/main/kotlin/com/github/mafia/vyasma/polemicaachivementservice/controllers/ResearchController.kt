package com.github.mafia.vyasma.polemicaachivementservice.controllers

import com.github.mafia.vyasma.polemica.library.utils.firstNightKills
import com.github.mafia.vyasma.polemica.library.utils.sumAward
import com.github.mafia.vyasma.polemica.library.utils.sumQuessScore
import com.github.mafia.vyasma.polemica.library.utils.sumScore
import com.github.mafia.vyasma.polemica.library.utils.winAsDonOrSher
import com.github.mafia.vyasma.polemicaachivementservice.rating.PolemicaTournamentService
import com.github.mafia.vyasma.polemicaachivementservice.research.ResearchPairStat
import com.github.mafia.vyasma.polemicaachivementservice.research.ResearchService
import com.github.mafia.vyasma.polemicaachivementservice.research.ResearchVotedByFourRedVotesAnswer
import java.nio.charset.StandardCharsets.UTF_8
import io.swagger.v3.oas.annotations.Operation
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("research")
class ResearchController(
    val researchService: ResearchService,
    private val polemicaTournamentService: PolemicaTournamentService
) {

    @Operation(hidden = true)
    @GetMapping("/blank")
    fun blank(): ResponseEntity<Void> {
        researchService.blank()
        return ResponseEntity.ok().build()
    }

    @Operation(hidden = true)
    @GetMapping("/gamesWhereFourRedVotesByPerson")
    fun gamesWhereFourRedVotesByPerson(): ResponseEntity<ResearchVotedByFourRedVotesAnswer> {
        return ResponseEntity.ok(researchService.getGamesWhereFourRedVotesByPerson())
    }

    @GetMapping("/pairStat")
    fun getPairStat(
        @RequestParam("firstId") firstId: Long,
        @RequestParam("secondId") secondId: Long
    ): ResponseEntity<ResearchPairStat> {
        return ResponseEntity.ok(
            researchService.getPairStat(firstId, secondId)
        )
    }

    @Operation(hidden = true)
    @GetMapping("/majorPairs")
    fun majorPairs(
        @RequestParam userIds: List<Long>,
    ): ResponseEntity<String> {
        return ResponseEntity.ok(researchService.getMajorPairs(userIds))
    }

    @GetMapping("/competitionsForUser")
    fun competitionsForUser(
        @RequestParam userId: Long,
    ): ResponseEntity<String> {
        return ResponseEntity.ok(researchService.getCompetitionsForUserCsv(userId))
    }

    @Operation(hidden = true)
    @GetMapping("/blackMoveTeamWinStat")
    fun getBlackMoveTeamWinStat(): ResponseEntity<String> {
        return ResponseEntity.ok(researchService.getBlackMoveTeamWinStat())
    }

    @Operation(hidden = true)
    @GetMapping("/blackMoveRefereeStat")
    fun getBlackRefereeStat(): ResponseEntity<String> {
        return ResponseEntity.ok(researchService.getBlackMoveRefereeStat())
    }

    @Operation(hidden = true)
    @GetMapping("/twoTwoTwoTwoDivInNinth")
    fun getTwoTwoTwoTwoDivInNinth(): ResponseEntity<Any> {
        return ResponseEntity.ok(researchService.getTwoTwoTwoTwoDivInNinth())
    }

    @GetMapping("/competitions/{tournamentId}/csv")
    fun getPolemicaRatingCsv(
        @PathVariable("tournamentId") tournamentId: Long
    ): ResponseEntity<String> {
        val csvContent = "Место,Id,Ник игрока в приложении,score,award,winAsDonOrSher,firstNightKills,quessScore\n" +
            polemicaTournamentService.getPolemicaTournamentResults(tournamentId).withIndex()
                .joinToString(separator = "\n")
                {
                    "${it.index + 1},${it.value.id},${it.value.username},${
                        String.format(
                            "%.3f",
                            it.value.sumScore()
                        )
                    },${
                        String.format(
                            "%.3f",
                            it.value.sumAward()
                        )
                    },${it.value.winAsDonOrSher()},${it.value.firstNightKills()},${
                        String.format(
                            "%.3f",
                            it.value.sumQuessScore()
                        )
                    }"
                }

        val headers = HttpHeaders()
        headers.contentType = MediaType("text", "csv", Charsets.UTF_8)
        headers.set("Content-Disposition", "inline; filename=\"results.csv\"")

        return ResponseEntity.ok()
            .headers(headers)
            .body(csvContent)
    }
}
