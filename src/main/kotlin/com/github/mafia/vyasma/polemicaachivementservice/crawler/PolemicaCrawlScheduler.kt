package com.github.mafia.vyasma.polemicaachivementservice.crawler

import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty(value = ["app.scheduler.enable"], havingValue = "true", matchIfMissing = true)
@EnableScheduling
class PolemicaCrawlScheduler(val crawlerService: CrawlerService) {
    private val logger = LoggerFactory.getLogger(PolemicaCrawlScheduler::class.java.name)

    @Scheduled(fixedDelayString = "#{@scheduler.interval.toMillis()}")
    private fun update() {
        try {
            logger.info("Crawl start")
            crawlerService.crawl()
        } catch (e: Exception) {
            logger.error("Error on updating links: " + e.message, e)
        }
    }
}
