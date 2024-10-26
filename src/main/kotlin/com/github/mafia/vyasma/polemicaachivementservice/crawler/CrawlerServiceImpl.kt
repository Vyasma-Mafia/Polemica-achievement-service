package com.github.mafia.vyasma.polemicaachivementservice.crawler

import com.github.mafia.vyasma.polemicaachivementservice.model.game.PolemicaGamePlace
import com.github.mafia.vyasma.polemicaachivementservice.model.jpa.Game
import com.github.mafia.vyasma.polemicaachivementservice.repositories.GameRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.lang.Thread.sleep

private const val GET_LIMIT = 100L

@Service
class CrawlerServiceImpl(
    val polemicaClient: PolemicaClient,
    val gameRepository: GameRepository,
    val crawlClubs: List<Long>
) : CrawlerService {
    private val logger = LoggerFactory.getLogger(CrawlerServiceImpl::class.java.name)


    override fun crawl() {
        crawlClubs.forEach { crawlClub(it) }
    }

    fun crawlClub(clubId: Long) {
        logger.info("Crawl club $clubId started")
        var offset = 0L
        do {
            val games = polemicaClient.getGamesFromClub(clubId, offset, GET_LIMIT)
            val gamesInBd = gameRepository.findAllById(games.map { it.id }).map { it.gameId }.toSet()
            games
                .filter { it.result != null }
                .filter { it.id !in gamesInBd }
                .forEach {
                    try {
                        val res = polemicaClient.getGameFromClub(PolemicaClient.PolemicaClubGameId(clubId, it.id, 4))
                        sleep(300)
                        val game = Game(res.id, 0, res, PolemicaGamePlace(clubId = clubId))
                        gameRepository.save(game)
                    } catch (e: Exception) {
                        logger.warn("Error on get game: ${it.id} from club $clubId", e)
                    }
                }
            offset += GET_LIMIT
        } while (gamesInBd.size < games.size)
        logger.info("Crawl club $clubId finished")
    }
}
