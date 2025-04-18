package com.github.mafia.vyasma.polemicaachivementservice.crawler

import com.github.mafia.vyasma.polemica.library.client.PolemicaClient
import com.github.mafia.vyasma.polemicaachivementservice.achievements.services.AchievementService
import com.github.mafia.vyasma.polemicaachivementservice.model.jpa.Game
import com.github.mafia.vyasma.polemicaachivementservice.model.jpa.PolemicaGamePlace
import com.github.mafia.vyasma.polemicaachivementservice.repositories.GameRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

private const val GET_LIMIT = 100L

@Service
class CrawlerServiceImpl(
    val polemicaClient: PolemicaClient,
    val gameRepository: GameRepository,
    val crawlClubs: List<Long>,
    val achievementService: AchievementService
) : CrawlerService {
    private val logger = LoggerFactory.getLogger(CrawlerServiceImpl::class.java.name)

    override fun crawl(withStopOnDb: Boolean) {
        crawlClubs.forEach { crawlClub(it, withStopOnDb) }
        crawlCompetitions(withStopOnDb)
        achievementService.checkAchievements()
    }

    override fun reparseGames(fullDelete: Boolean) {
        if (fullDelete) {
            gameRepository.deleteAll()
        }
        crawl(false)
    }

    fun crawlCompetitions(withStopOnDb: Boolean) {
        logger.info("Crawling competitions started")
        val competitions = polemicaClient.getCompetitions()
        competitions.filter { it.city == "Санкт-Петербург" }.forEach { crawlCompetition(it, withStopOnDb) }
        logger.info("Crawling competitions finished")
    }

    fun crawlCompetition(competition: PolemicaClient.PolemicaCompetition, withStopOnDb: Boolean) {
        val games = polemicaClient.getGamesFromCompetition(competition.id)
        val gamesInBd = gameRepository.findAllById(games.map { it.id }).map { it.gameId }.toSet()
        games
            .filter { it.result != null }
            .filter { it.id !in gamesInBd || !withStopOnDb }
            .forEach {
                try {
                    val res = polemicaClient.getGameFromCompetition(
                        PolemicaClient.PolemicaCompetitionGameId(
                            competition.id,
                            it.id,
                            4
                        )
                    )
                    val game = Game(
                        gameId = res.id,
                        data = res,
                        gamePlace = PolemicaGamePlace(competitionId = competition.id),
                        started = res.started
                    )
                    gameRepository.save(game)
                } catch (e: Exception) {
                    logger.error("Error while crawling game ${it.id}", e)
                }
            }
    }

    fun crawlClub(clubId: Long, withStopOnDb: Boolean) {
        logger.info("Crawl club $clubId started")
        var offset = 0L
        do {
            val games = polemicaClient.getGamesFromClub(clubId, offset, GET_LIMIT)
            val gamesInBd = gameRepository.findAllById(games.map { it.id }).map { it.gameId }.toSet()
            games
                .filter { it.result != null }
                .filter { it.id !in gamesInBd || !withStopOnDb }
                .forEach {
                    try {
                        val res = polemicaClient.getGameFromClub(PolemicaClient.PolemicaClubGameId(clubId, it.id, 4))
                        val game = Game(
                            gameId = res.id,
                            data = res,
                            gamePlace = PolemicaGamePlace(clubId = clubId),
                            started = res.started
                        )
                        gameRepository.save(game)
                    } catch (e: Exception) {
                        logger.warn("Error on get game: ${it.id} from club $clubId", e)
                    }
                }
            offset += GET_LIMIT
        } while ((!withStopOnDb && games.isNotEmpty()) || gamesInBd.size < games.size)
        logger.info("Crawl club $clubId finished")
    }
}
