package com.github.mafia.vyasma.polemicaachivementservice.crawler

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class CrawlerServiceImpl(val polemicaClient: PolemicaClient) : CrawlerService {
    private val logger = LoggerFactory.getLogger(CrawlerServiceImpl::class.java.name)


    override fun crawl() {
        val res = polemicaClient.getGameFromClub(PolemicaClient.PolemicaClubGameId(289, 261573))
        logger.info(res.toString())
    }
}
