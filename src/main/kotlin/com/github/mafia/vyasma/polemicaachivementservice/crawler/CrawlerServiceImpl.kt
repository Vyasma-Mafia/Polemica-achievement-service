package com.github.mafia.vyasma.polemicaachivementservice.crawler

import com.github.mafia.vyasma.polemica.library.client.PolemicaClient
import com.github.mafia.vyasma.polemicaachivementservice.achievements.services.AchievementService
import com.github.mafia.vyasma.polemicaachivementservice.model.jpa.Game
import com.github.mafia.vyasma.polemicaachivementservice.model.jpa.PolemicaGamePlace
import com.github.mafia.vyasma.polemicaachivementservice.model.jpa.ProcessedTournamentId
import com.github.mafia.vyasma.polemicaachivementservice.rating.RatingService
import com.github.mafia.vyasma.polemicaachivementservice.repositories.GameRepository
import com.github.mafia.vyasma.polemicaachivementservice.repositories.ProcessedTournamentIdRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.time.LocalDateTime

private const val GET_LIMIT = 100L

private const val SPB = "Санкт-Петербург"

@Service
class CrawlerServiceImpl(
    val polemicaClient: PolemicaClient,
    val gameRepository: GameRepository,
    val crawlClubs: MutableList<Long>,
    val achievementService: AchievementService,
    val ratingService: RatingService,
    val processedTournamentIdRepository: ProcessedTournamentIdRepository,
    @Value("\${app.crawl-competitions-by-id-range.enable:false}")
    val crawlCompetitionsByIdRangeEnabled: Boolean
) : CrawlerService {
    private val logger = LoggerFactory.getLogger(CrawlerServiceImpl::class.java.name)

    override fun crawl(withStopOnDb: Boolean) {
        crawlClubs.forEach { crawlClub(it, withStopOnDb) }
        crawlCompetitions(withStopOnDb)
        if (crawlCompetitionsByIdRangeEnabled) {
            crawlCompetitionsByIdRange(withStopOnDb)
        }
        achievementService.checkAchievements()
        ratingService.crawlGames()
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
        competitions.filter { it.city == SPB }.forEach { crawlCompetition(it, withStopOnDb) }
        logger.info("Crawling competitions finished")
    }

    fun crawlCompetition(competition: PolemicaClient.PolemicaCompetition, withStopOnDb: Boolean) {
        val games = polemicaClient.getGamesFromCompetition(competition.id)
        val gamesInBd = gameRepository.findAllById(games.map { it.id }).map { it.gameId }.toSet()
        games
            .filter { it.result != null }
            .filter { it.id !in gamesInBd || !withStopOnDb }
            .map {
                polemicaClient.getGameFromCompetition(
                    PolemicaClient.PolemicaCompetitionGameId(
                        competition.id,
                        it.id,
                        4
                    )
                )
            }
            .filter { it.scoringType == 1 }
            .forEach {
                try {
                    val id = it.id ?: return@forEach
                    val game = Game(
                        gameId = id,
                        data = it,
                        gamePlace = PolemicaGamePlace(competitionId = competition.id),
                        started = it.started
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
                        val id = res.id ?: return@forEach
                        val game = Game(
                            gameId = id,
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

    override fun crawlCompetitionsByIdRange(withStopOnDb: Boolean) {
        logger.info("Crawling competitions by ID range started")
        try {
            // Get minimum ID from currently visible competitions
            val competitions = polemicaClient.getCompetitions()
            val minCompetitionId = competitions.minOfOrNull { it.id } ?: Long.MAX_VALUE

            if (minCompetitionId == Long.MAX_VALUE) {
                logger.warn("No competitions found, skipping ID range crawl")
                return
            }

            logger.info("Processing tournament IDs from 1 to $minCompetitionId")
            var processedCount = 0
            var skippedCount = 0
            var errorCount = 0

            for (tournamentId in 1L until minCompetitionId) {
                try {
                    // Check if already processed
                    if (processedTournamentIdRepository.existsById(tournamentId)) {
                        skippedCount++
                        continue
                    }
                    
                    // Try to fetch games from this tournament ID
                    val games = try {
                        val competition = polemicaClient.getCompetition(tournamentId)
                        if (competition?.city != SPB) {
                            throw IllegalArgumentException()
                        }
                        polemicaClient.getGamesFromCompetition(tournamentId)
                    } catch (e: Exception) {
                        // Tournament doesn't exist or is not accessible
                        logger.debug("Tournament ID $tournamentId does not exist or is not accessible: ${e.message}")
                        // Mark as processed even if it doesn't exist to avoid retrying
                        processedTournamentIdRepository.save(ProcessedTournamentId(tournamentId, LocalDateTime.now()))
                        errorCount++
                        continue
                    }

                    // If games exist, process the competition
                    if (games.isNotEmpty()) {
                        logger.info("Found tournament ID $tournamentId with ${games.size} games")
                        // Create a minimal competition object for compatibility with crawlCompetition
                        val competition = PolemicaClient.PolemicaCompetition(
                            tournamentId,
                            "Tournament $tournamentId",
                            null, null, null, null, null, null, null, null, null, null, null, null, null, null
                        )
                        crawlCompetition(competition, withStopOnDb)
                        processedCount++
                    }

                    // Mark as processed
                    processedTournamentIdRepository.save(ProcessedTournamentId(tournamentId, LocalDateTime.now()))
                } catch (e: Exception) {
                    logger.error("Error processing tournament ID $tournamentId", e)
                    errorCount++
                    // Mark as processed to avoid infinite retries on persistent errors
                    try {
                        if (!processedTournamentIdRepository.existsById(tournamentId)) {
                            processedTournamentIdRepository.save(
                                ProcessedTournamentId(
                                    tournamentId,
                                    LocalDateTime.now()
                                )
                            )
                        }
                    } catch (saveException: Exception) {
                        logger.error("Error saving processed tournament ID $tournamentId", saveException)
                    }
                }
            }

            logger.info("Crawling competitions by ID range finished. Processed: $processedCount, Skipped: $skippedCount, Errors: $errorCount")
        } catch (e: Exception) {
            logger.error("Error during ID range crawling", e)
        }
    }
}
