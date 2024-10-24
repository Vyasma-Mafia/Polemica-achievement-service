package com.github.mafia.vyasma.polemicaachivementservice

import com.github.mafia.vyasma.polemicaachivementservice.configurations.ApplicationConfig
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@EnableScheduling
@SpringBootApplication
@EnableConfigurationProperties(ApplicationConfig::class)
class PolemicaAchievementServiceApplication

fun main(args: Array<String>) {
    runApplication<PolemicaAchievementServiceApplication>(*args)
}
