package com.github.mafia.vyasma.polemicaachivementservice.crawler

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class CrawlerServiceImpl(val polemicaClient: PolemicaClient) : CrawlerService {
    private val logger = LoggerFactory.getLogger(CrawlerServiceImpl::class.java.name)


    override fun crawl() {
        val games = polemicaClient.getGamesFromClub(289, 0, 50)
        games.filter { it.result != null }.forEach {
            try {
                val res = polemicaClient.getGameFromClub(PolemicaClient.PolemicaClubGameId(289, it.id, 4))
                logger.info(res.toString())
            } catch (e: Exception) {
                logger.warn("Error on get game: ${it.id}", e)
            }
        }
    }
}
