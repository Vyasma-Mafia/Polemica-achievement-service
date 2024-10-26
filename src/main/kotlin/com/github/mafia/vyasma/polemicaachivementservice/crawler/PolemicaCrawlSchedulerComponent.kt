package com.github.mafia.vyasma.polemicaachivementservice.crawler

import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty(value = ["app.crawlScheduler.enable"], havingValue = "true", matchIfMissing = true)
@EnableScheduling
class PolemicaCrawlSchedulerComponent(val crawlerService: CrawlerService) {
    private val logger = LoggerFactory.getLogger(PolemicaCrawlSchedulerComponent::class.java.name)

    @Scheduled(fixedDelayString = "#{@crawlScheduler.interval.toMillis()}")
    private fun update() {
        try {
            logger.info("Crawl start")
            crawlerService.crawl()
        } catch (e: Exception) {
            logger.error("Error on crawling: " + e.message, e)
        }
    }
}
