package com.github.mafia.vyasma.polemicaachivementservice.crawler

interface CrawlerService {
    fun crawl(withStopOnDb: Boolean)
    fun reparseGames(fullDelete: Boolean)
}
