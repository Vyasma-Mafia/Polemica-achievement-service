package com.github.mafia.vyasma.polemicaachivementservice.controllers

import com.github.mafia.vyasma.polemicaachivementservice.achievements.AchievementService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("achievements")
class AchievementController(val achievementService: AchievementService) {

    @Operation
    @GetMapping
    fun getAchievements(
        @RequestParam(defaultValue = "")
        @Parameter(
            name = "usernames",
            description = "Пользователи, для которых будут выданы достижения",
            required = false,
            array = ArraySchema(items = Schema(type = "String"))
        ) usernames: List<String>,
        @RequestParam(defaultValue = "")
        @Parameter(
            name = "ids",
            description = "Id пользователей, для которых будут выданы достижения",
            required = false,
            array = ArraySchema(items = Schema(type = "Long"))
        ) ids: List<Long>,
    ): ResponseEntity<AchievementService.AchievementsWithGains> {
        return ResponseEntity.ok().body(achievementService.getAchievements(usernames, ids))
    }

    @Operation(hidden = true)
    @PostMapping
    fun checkAchievements(): ResponseEntity<Void> {
        achievementService.checkAchievements()
        return ResponseEntity.ok().build()
    }

    @Operation(hidden = true)
    @GetMapping("/{achievementId}/games/_explain")
    fun explainGamesForAchievement(
        @PathVariable(name = "achievementId") achievementId: String
    ): ResponseEntity<AchievementService.AchievementGames> {
        return ResponseEntity.ok(achievementService.getAchievementsGames(achievementId))
    }
}
