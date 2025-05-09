package com.github.mafia.vyasma.polemicaachivementservice.controllers

import com.github.mafia.vyasma.polemicaachivementservice.achievements.services.AchievementService
import com.github.mafia.vyasma.polemicaachivementservice.crawler.CrawlerService
import com.github.mafia.vyasma.polemicaachivementservice.rating.GamePointsService
import com.github.mafia.vyasma.polemicaachivementservice.rating.RatingService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.concurrent.CompletableFuture

@RestController
@RequestMapping("admin")
class AdminController(
    val achievementService: AchievementService,
    val crawlerService: CrawlerService,
    val gamePointsService: GamePointsService,
    private val ratingService: RatingService
) {
    @PostMapping("/achievements/_check")
    fun checkAchievements(): ResponseEntity<Void> {
        achievementService.checkAchievements()
        return ResponseEntity.ok().build()
    }

    @PostMapping("/achievements/_recheck")
    fun recheckAchievements(): ResponseEntity<Void> {
        achievementService.recheckAchievements()
        return ResponseEntity.ok().build()
    }

    @GetMapping("/achievements/{achievementId}/games/_explain")
    fun explainGamesForAchievement(
        @PathVariable(name = "achievementId") achievementId: String,
        @RequestParam(name = "gameId", defaultValue = "0") gameId: Long?
    ): ResponseEntity<AchievementService.AchievementGames> {
        return ResponseEntity.ok(achievementService.getAchievementsGames(achievementId, gameId))
    }

    @PostMapping("/games/_reparse")
    fun reparseGames(
        @RequestParam(name = "fullDelete", defaultValue = "false") fullDelete: Boolean
    ): ResponseEntity<Void> {
        crawlerService.reparseGames(fullDelete)
        return ResponseEntity.ok().build()
    }

    @PostMapping("/rating/_recalculate")
    fun recalculateRatings(): ResponseEntity<String> {
        // Запускаем пересчет в отдельном потоке, чтобы не блокировать запрос
        CompletableFuture.runAsync {
            ratingService.recalculateRatingBatched()
        }

        return ResponseEntity.accepted().body("Rating recalculation started")
    }
}
