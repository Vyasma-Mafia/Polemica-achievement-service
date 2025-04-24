package com.github.mafia.vyasma.polemicaachivementservice.controllers

import com.github.mafia.vyasma.polemicaachivementservice.achievements.services.AchievementService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDateTime

@RestController
@RequestMapping("achievements")
class AchievementController(val achievementService: AchievementService) {

    @Operation
    @GetMapping
    fun getAchievements(
        @RequestParam(defaultValue = "[]")
        @Parameter(
            name = "usernames",
            description = "Пользователи, для которых будут выданы достижения",
            required = false,
            array = ArraySchema(items = Schema(type = "String"))
        ) usernames: List<String>,

        @RequestParam(defaultValue = "[]")
        @Parameter(
            name = "ids",
            description = "Id пользователей, для которых будут выданы достижения",
            required = false,
            array = ArraySchema(items = Schema(type = "Long"))
        ) ids: List<Long>,

        @RequestParam(required = false)
        @Parameter(
            name = "startDate",
            description = "Дата, начиная с которой учитываются достижения (формат ISO-8601: yyyy-MM-ddTHH:mm:ss)",
            required = false,
            schema = Schema(type = "string", format = "date-time")
        )
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        startDate: LocalDateTime?
    ): ResponseEntity<AchievementService.AchievementsWithGains> {
        return ResponseEntity.ok().body(achievementService.getAchievements(usernames, ids, startDate))
    }

    @GetMapping("/_top")
    fun getTop5AchievementsByUserIds(
        @RequestParam
        @Parameter(
            name = "userIds",
            description = "Id пользователей, для которых нужно получить топ достижений",
            required = true,
            array = ArraySchema(items = Schema(type = "Long"))
        ) userIds: List<Long>,

        @RequestParam(defaultValue = "5")
        @Parameter(
            name = "limit",
            description = "Максимальное количество возвращаемых достижений для каждой категории",
            required = false
        ) limit: Int,

        @RequestParam(required = false)
        @Parameter(
            name = "startDate",
            description = "Дата, начиная с которой учитываются достижения (формат ISO-8601: yyyy-MM-ddTHH:mm:ss)",
            required = false,
            schema = Schema(type = "string", format = "date-time")
        )
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        startDate: LocalDateTime?
    ): ResponseEntity<AchievementService.AchievementsWithGains> {
        return ResponseEntity.ok(achievementService.getTopAchievementUsers(userIds, limit, startDate))
    }
}
