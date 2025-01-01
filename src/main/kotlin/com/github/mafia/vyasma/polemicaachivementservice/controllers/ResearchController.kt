package com.github.mafia.vyasma.polemicaachivementservice.controllers

import com.github.mafia.vyasma.polemicaachivementservice.research.ResearchPairStat
import com.github.mafia.vyasma.polemicaachivementservice.research.ResearchService
import com.github.mafia.vyasma.polemicaachivementservice.research.ResearchVotedByFourRedVotesAnswer
import io.swagger.v3.oas.annotations.Operation
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("research")
class ResearchController(val researchService: ResearchService) {

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
}
