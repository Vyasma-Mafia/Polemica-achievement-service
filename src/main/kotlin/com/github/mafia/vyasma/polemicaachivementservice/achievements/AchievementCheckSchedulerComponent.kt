package com.github.mafia.vyasma.polemicaachivementservice.achievements

import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty(value = ["app.achievementCheckScheduler.enable"], havingValue = "true", matchIfMissing = true)
@EnableScheduling
class AchievementCheckSchedulerComponent(val achievementService: AchievementService) {
    private val logger = LoggerFactory.getLogger(AchievementCheckSchedulerComponent::class.java.name)

    @Scheduled(fixedDelayString = "#{@achievementCheckScheduler.interval.toMillis()}")
    private fun update() {
        try {
            logger.info("Achievements check start")
            achievementService.checkAchievements()
        } catch (e: Exception) {
            logger.error("Error on check achievements: " + e.message, e)
        }
    }
}
