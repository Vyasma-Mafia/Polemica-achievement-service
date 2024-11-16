package com.github.mafia.vyasma.polemicaachivementservice.controllers

import com.github.mafia.vyasma.polemicaachivementservice.research.ResearchService
import com.github.mafia.vyasma.polemicaachivementservice.research.ResearchVotedByFourRedVotesAnswer
import io.swagger.v3.oas.annotations.Operation
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("research")
class ResearchController(val researchService: ResearchService) {

    @Operation(hidden = true)
    @GetMapping("/gamesWhereFourRedVotesByPerson")
    fun explainGamesForAchievement(): ResponseEntity<ResearchVotedByFourRedVotesAnswer> {
        return ResponseEntity.ok(researchService.getGamesWhereFourRedVotesByPerson())
    }
}