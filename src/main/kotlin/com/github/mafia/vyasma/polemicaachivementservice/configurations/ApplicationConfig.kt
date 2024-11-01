package com.github.mafia.vyasma.polemicaachivementservice.configurations

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.validation.annotation.Validated
import java.time.Duration
import java.time.LocalDateTime

@Validated
@ConfigurationProperties(prefix = "app", ignoreUnknownFields = true)
data class ApplicationConfig(
    val crawlScheduler: Scheduler,
    val achievementCheckScheduler: Scheduler,
    val crawlClubs: List<Long>,
    val achievementCheckGameStartedAfter: LocalDateTime
) {
    @Bean
    fun crawlScheduler() = crawlScheduler
    @Bean
    fun achievementCheckScheduler() = achievementCheckScheduler

    @Bean
    fun crawlClubs() = crawlClubs

    @Bean
    fun achievementCheckGameStartedAfter() = achievementCheckGameStartedAfter

    data class Scheduler(val enable: Boolean, val interval: Duration) {
    }
}
