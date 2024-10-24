package com.github.mafia.vyasma.polemicaachivementservice.controllers

import io.swagger.v3.oas.annotations.Operation
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("achievements")
class AchievementController {

    @Operation
    @GetMapping
    fun getAchievements(): ResponseEntity<Void> {
        return ResponseEntity.ok().build()
    }
}
