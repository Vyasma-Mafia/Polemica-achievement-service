package com.github.mafia.vyasma.polemicaachivementservice.configurations

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.validation.annotation.Validated
import java.time.Duration

@Validated
@ConfigurationProperties(prefix = "app", ignoreUnknownFields = false)
data class ApplicationConfig(
    val scheduler: Scheduler,
    val linkCheckProperties: LinkCheckProperties,
) {
    @Bean
    fun scheduler() = scheduler
    @Bean
    fun linkCheckProperties() = linkCheckProperties

    data class Scheduler(val enable: Boolean, val interval: Duration, val forceCheckDelay: Duration) {
    }

    data class LinkCheckProperties(val linkCheckInterval: Duration) {
    }
}
