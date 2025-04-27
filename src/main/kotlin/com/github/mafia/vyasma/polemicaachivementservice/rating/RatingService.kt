package com.github.mafia.vyasma.polemicaachivementservice.rating

import com.github.mafia.vyasma.polemica.library.model.game.PolemicaGameResult
import com.github.mafia.vyasma.polemica.library.model.game.Position
import com.github.mafia.vyasma.polemica.library.utils.getRole
import com.github.mafia.vyasma.polemica.library.utils.isRed
import com.github.mafia.vyasma.polemicaachivementservice.model.PlayerPoints
import com.github.mafia.vyasma.polemicaachivementservice.model.PolemicaGamePlayersPoints
import com.github.mafia.vyasma.polemicaachivementservice.model.jpa.Game
import com.github.mafia.vyasma.polemicaachivementservice.repositories.GameRepository
import com.github.mafia.vyasma.polemicaachivementservice.repositories.PlayerRatingHistoryRepository
import com.github.mafia.vyasma.polemicaachivementservice.repositories.RecalibrationHistoryRepository
import com.github.mafia.vyasma.polemicaachivementservice.repositories.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service

@Service
class RatingService(
    val gameRepository: GameRepository,
    private val gamePointsService: GamePointsService,
    private val playerRatingService: PlayerRatingService,
    private val playerRatingHistoryRepository: PlayerRatingHistoryRepository,
    private val userRepository: UserRepository,
    private val recalibrationHistoryRepository: RecalibrationHistoryRepository
) {
    val logger = LoggerFactory.getLogger(this::class.java)

    fun crawlGames() {
        gameRepository.findByPointsIsNullOrderByGameId()
            .forEach { game ->
                updateGamePoints(game)
                playerRatingService.updatePlayerRatings(game)
            }
    }

    private fun updateGamePoints(game: Game) {
        val points =
            try {
                PolemicaGamePlayersPoints(
                    true,
                    gamePointsService.fetchPlayerStats(game.gameId)
                        .map { adjustPointsBasedOnGameResult(it, game) }
                )
            } catch (e: Exception) {
                // logger.error("Error fetching points for game ${game.gameId}", e)
                PolemicaGamePlayersPoints(false, Position.entries.map { PlayerPoints(it.value, 0.0) })
            }

        game.points = points
        gameRepository.save(game)
    }

    private fun adjustPointsBasedOnGameResult(playerPoints: PlayerPoints, game: Game): PlayerPoints {
        val position = Position.fromInt(playerPoints.position) ?: return playerPoints
        val isCorrectPrediction =
            game.data.getRole(position).isRed() == (game.data.result == PolemicaGameResult.RED_WIN)

        return if (isCorrectPrediction) {
            playerPoints.copy(points = playerPoints.points - 1)
        } else {
            playerPoints
        }
    }

    companion object {
        private const val BATCH_SIZE = 50 // Размер порции обрабатываемых игр
    }

    fun clearRatingData() {
        // Очистка данных в отдельной транзакции
        playerRatingHistoryRepository.deleteAll()
        userRepository.clearRatingData()
        recalibrationHistoryRepository.deleteAll()
    }

    fun recalculateRatingBatched() {
        // Очищаем данные
        clearRatingData()

        // Получаем общее количество игр
        val totalGames = gameRepository.count()
        var processedGames = 0L

        // Обрабатываем игры порциями
        var page = 0
        val pageSize = BATCH_SIZE

        while (processedGames < totalGames) {
            val games = gameRepository.findAll(
                PageRequest.of(page, pageSize, Sort.by("gameId"))
            ).content

            playerRatingService.processBatch(games)

            processedGames += games.size
            page++

            // Логирование прогресса
            logger.info("Processed ${processedGames}/${totalGames} games (${processedGames * 100 / totalGames}%)")
        }
    }
}
