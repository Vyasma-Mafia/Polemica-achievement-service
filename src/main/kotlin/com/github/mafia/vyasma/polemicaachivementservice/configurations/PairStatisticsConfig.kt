package com.github.mafia.vyasma.polemicaachivementservice.configurations

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties(prefix = "app.pair-statistics")
data class PairStatisticsConfig(
    var minGamesThreshold: Int = 10,
    var topPartnersCount: Int = 5,
    var cacheEnabled: Boolean = true,
    var cacheTtlMinutes: Long = 60
)