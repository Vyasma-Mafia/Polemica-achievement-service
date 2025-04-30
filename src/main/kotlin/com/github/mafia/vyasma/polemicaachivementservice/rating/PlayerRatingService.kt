package com.github.mafia.vyasma.polemicaachivementservice.rating

import com.github.mafia.vyasma.polemica.library.model.game.PolemicaGameResult
import com.github.mafia.vyasma.polemica.library.model.game.PolemicaPlayer
import com.github.mafia.vyasma.polemica.library.model.game.Role
import com.github.mafia.vyasma.polemica.library.utils.isBlack
import com.github.mafia.vyasma.polemica.library.utils.isRed
import com.github.mafia.vyasma.polemicaachivementservice.model.PlayerPoints
import com.github.mafia.vyasma.polemicaachivementservice.model.jpa.Game
import com.github.mafia.vyasma.polemicaachivementservice.model.jpa.PlayerRatingHistory
import com.github.mafia.vyasma.polemicaachivementservice.model.jpa.RecalibrationHistory
import com.github.mafia.vyasma.polemicaachivementservice.model.jpa.User
import com.github.mafia.vyasma.polemicaachivementservice.repositories.PlayerRatingHistoryRepository
import com.github.mafia.vyasma.polemicaachivementservice.repositories.RecalibrationHistoryRepository
import com.github.mafia.vyasma.polemicaachivementservice.repositories.UserRepository
import com.pocketcombats.openskill.Adjudicator
import com.pocketcombats.openskill.QualityEvaluator
import com.pocketcombats.openskill.RatingModelConfig
import com.pocketcombats.openskill.data.SimplePlayerResult
import com.pocketcombats.openskill.data.SimpleTeamResult
import com.pocketcombats.openskill.model.ThurstoneMostellerFull
import jakarta.transaction.Transactional
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import kotlin.math.abs

const val DEFAULT_MU = 250.0
const val DEFAULT_SIGMA = DEFAULT_MU / 3.0

@Service
class PlayerRatingService(
    private val playerRepository: UserRepository,
    private val ratingHistoryRepository: PlayerRatingHistoryRepository,
    private val recalibrationHistoryRepository: RecalibrationHistoryRepository
) {
    val logger = LoggerFactory.getLogger(this::class.java)

    // Создаем базовую конфигурацию OpenSkill
    private val config = RatingModelConfig.builder()
        .setBeta(DEFAULT_MU / 6.0)   // Стандартное значение
        .setTau(DEFAULT_MU / 300.0)  // Динамический фактор неопределенности
        .setKappa(0.001)      // Коэффициент демпфирования
        .build()

    // Создаем оценщик качества матчей (для аналитики)
    private val qualityEvaluator = QualityEvaluator(config)

    // Создаем арбитра с моделью Thurstone-Mosteller (наиболее близка к TrueSkill)
    private val adjudicator = Adjudicator<Long>(
        config,
        ThurstoneMostellerFull(config)
    )

    /**
     * Обновляет рейтинги игроков с использованием OpenSkill Java
     */
    @Transactional
    fun updateRatings(
        gameId: Long,
        mafiaTeam: List<Pair<Long, Double>>,
        civilianTeam: List<Pair<Long, Double>>,
        isMafiaWin: Boolean,
        gameDate: LocalDateTime,
        isCompetitive: Boolean = false // Параметр для указания типа игры
    ) {
        if (mafiaTeam.isEmpty() || civilianTeam.isEmpty()) {
            return
        }

        // Получаем текущие рейтинги игроков
        val mafiaPlayers = playerRepository.findAllById(mafiaTeam.map { it.first }).associateBy { it.userId }
        val civilianPlayers = playerRepository.findAllById(civilianTeam.map { it.first }).associateBy { it.userId }

        // Выполняем рекалибровку sigma перед расчетом
        val allPlayers = mafiaPlayers.values + civilianPlayers.values
        allPlayers.forEach { player ->
            recalibrateSigmaIfNeeded(player, gameId, gameDate)
        }

        // Ранги команд
        val mafiaRank = if (isMafiaWin) 1 else 2
        val civilianRank = if (isMafiaWin) 2 else 1

        // Создаем списки результатов игроков с весами
        val mafiaPlayerResults = mafiaTeam.mapNotNull { (playerId, points) ->
            mafiaPlayers[playerId]?.let { player ->
                // Вес с учетом типа игры
                // val weight = calculatePlayerWeight(points, isMafiaWin, player, isCompetitive)

                SimplePlayerResult(
                    playerId,
                    player.mu ?: DEFAULT_MU,
                    player.sigma ?: DEFAULT_SIGMA
                )
            }
        }

        val civilianPlayerResults = civilianTeam.mapNotNull { (playerId, points) ->
            civilianPlayers[playerId]?.let { player ->
                // Вес с учетом типа игры
                // val weight = calculatePlayerWeight(points, !isMafiaWin, player, isCompetitive)

                SimplePlayerResult(
                    playerId,
                    player.mu ?: DEFAULT_MU,
                    player.sigma ?: DEFAULT_SIGMA
                )
            }
        }

        // Создаем командные результаты
        val mafiaTeamResult = SimpleTeamResult(
            // Средние параметры команды (будут пересчитаны библиотекой)
            mafiaPlayerResults.map { it.mu() }.average(),
            mafiaPlayerResults.map { it.sigma() }.average(),
            mafiaRank,
            mafiaPlayerResults
        )

        val civilianTeamResult = SimpleTeamResult(
            // Средние параметры команды (будут пересчитаны библиотекой)
            civilianPlayerResults.map { it.mu() }.average(),
            civilianPlayerResults.map { it.sigma() }.average(),
            civilianRank,
            civilianPlayerResults
        )

        // Оцениваем качество матча для аналитики
        val matchQuality = qualityEvaluator.evaluateQuality(mafiaTeamResult, civilianTeamResult)

        // Рассчитываем новые рейтинги
        val adjustments = adjudicator.rate(listOf(mafiaTeamResult, civilianTeamResult))

        // Обновляем рейтинги всех игроков
        adjustments.forEach { adjustment ->
            // Определяем команду и баллы игрока
            val playerPoints = when (adjustment.playerId) {
                in mafiaTeam.map { it.first } -> mafiaTeam.find { it.first == adjustment.playerId }?.second ?: 0.0
                in civilianTeam.map { it.first } -> civilianTeam.find { it.first == adjustment.playerId }?.second ?: 0.0
                else -> 0.0
            }

            // Определяем, победил ли игрок
            val isWinner = when (adjustment.playerId) {
                in mafiaTeam.map { it.first } -> isMafiaWin
                in civilianTeam.map { it.first } -> !isMafiaWin
                else -> false
            }

            // Получаем игрока из репозитория
            val player = when (adjustment.playerId) {
                in mafiaPlayers.keys -> mafiaPlayers[adjustment.playerId]
                in civilianPlayers.keys -> civilianPlayers[adjustment.playerId]
                else -> null
            } ?: return@forEach

            val calculatedWeight = calculatePlayerWeight(playerPoints, isWinner, player, isCompetitive)

            // Сохраняем старые значения для истории
            val oldMu = player.mu ?: DEFAULT_MU
            val oldSigma = player.sigma ?: DEFAULT_SIGMA

            // Обновляем параметры рейтинга
            val newMu = oldMu + (adjustment.mu - oldMu) * calculatedWeight
            val newSigma = adjustment.sigma

            player.mu = newMu
            player.sigma = newSigma
            player.rating = newMu - 3 * newSigma  // Консервативная оценка
            player.gamesPlayed++
            if (isWinner) player.gamesWon++

            // Сохраняем обновленного игрока
            playerRepository.save(player)

            // Вычисляем вес для записи в историю

            // Сохраняем историю изменения рейтинга
            saveRatingHistory(
                player,
                gameId,
                oldMu,
                oldSigma,
                newMu,
                newSigma,
                playerPoints,
                isWinner,
                calculatedWeight,
                matchQuality,
                isCompetitive,
                gameDate
            )
        }
    }

    /**
     * Рекалибрует sigma (неопределенность) игрока при необходимости
     */
    private fun recalibrateSigmaIfNeeded(player: User, gameId: Long, gameDate: LocalDateTime) {
        val currentSigma = player.sigma ?: DEFAULT_SIGMA
        val gamesPlayed = player.gamesPlayed

        // Случай 1: Новый игрок с высоким рейтингом - замедляем его рост
        if (gamesPlayed < 20 && player.rating != null && player.rating!! > DEFAULT_MU) {
            // Не меняем sigma, но ограничиваем последующие изменения через веса
            return
        }

        // Случай 2: Рекалибровка по количеству игр
        val thresholds = mapOf(
            25 to 0.2,    // После 25 игр: +20% к sigma
            50 to 0.25,   // После 50 игр: +25% к sigma
            100 to 0.30,   // После 100 игр: +30% к sigma
            200 to 0.35,  // После 200 игр: +35% к sigma
            500 to 0.4    // После 500 игр: +40% к sigma
        )

        // Определяем, нужна ли рекалибровка по порогу количества игр
        val matchingThreshold = thresholds.entries
            .filter { gamesPlayed % it.key == 0 && gamesPlayed > 0 } // Каждые X игр
            .maxByOrNull { it.key }

        if (matchingThreshold != null) {
            // Проверяем, не была ли уже проведена рекалибровка
            val recentRecalibration = recalibrationHistoryRepository
                .findByPlayerAndGameNumberRange(player, gamesPlayed - 5, gamesPlayed)

            if (recentRecalibration.isEmpty()) {
                // Выполняем рекалибровку
                val recalibrationAmount = currentSigma * matchingThreshold.value.toDouble()
                val newSigma = currentSigma + recalibrationAmount

                // Логируем рекалибровку
                recalibrationHistoryRepository.save(
                    RecalibrationHistory(
                        player = player,
                        gameId = gameId,
                        gameNumber = gamesPlayed,
                        oldSigma = currentSigma,
                        newSigma = newSigma,
                        reason = "Threshold recalibration (${matchingThreshold.key} games)",
                        timestamp = gameDate
                    )
                )

                // Обновляем sigma
                player.sigma = newSigma
                playerRepository.save(player)
            }
        }

        // Случай 3: Рекалибровка по длительному периоду стагнации рейтинга
        if (gamesPlayed > 50) {
            val recentHistory = ratingHistoryRepository
                .findLastNHistoryByPlayer(player, 15)

            if (recentHistory.size >= 10) {
                // Проверяем, насколько менялся рейтинг в последних играх
                val ratingChanges = recentHistory.map { abs(it.muDelta) }
                val averageChange = ratingChanges.average()

                // Если средние изменения очень малы, рекалибруем
                if (averageChange < 2.0) {
                    val lastRecalibration = recalibrationHistoryRepository
                        .findByPlayerOrderByTimestampDesc(player)
                        .firstOrNull()

                    // Не делаем рекалибровку слишком часто
                    val daysSinceLastRecalibration = if (lastRecalibration != null) {
                        ChronoUnit.DAYS.between(
                            lastRecalibration.timestamp.toLocalDate(),
                            LocalDate.now()
                        )
                    } else {
                        Long.MAX_VALUE
                    }

                    if (daysSinceLastRecalibration >= 14) { // Не чаще раза в две недели
                        val recalibrationAmount = currentSigma * 0.3
                        val newSigma = currentSigma + recalibrationAmount

                        // Логируем рекалибровку
                        recalibrationHistoryRepository.save(
                            RecalibrationHistory(
                                player = player,
                                gameId = gameId,
                                gameNumber = gamesPlayed,
                                oldSigma = currentSigma,
                                newSigma = newSigma,
                                reason = "Stagnation recalibration (avg change: ${
                                    String.format(
                                        "%.2f",
                                        averageChange
                                    )
                                })",
                                timestamp = gameDate
                            )
                        )

                        // Обновляем sigma
                        player.sigma = newSigma
                        playerRepository.save(player)
                    }
                }
            }
        }
    }

    /**
     * Рассчитывает вес игрока с учетом рекалибровки
     */
    private fun calculatePlayerWeight(
        points: Double,
        isWinner: Boolean,
        player: User,
        isCompetitive: Boolean
    ): Double {
        // Базовый коэффициент от 0.5 до 1.5 на основе баллов
        val baseWeightFactor = when {
            points < -0.2 -> 0.5
            points < 0 -> 0.75
            points == 0.0 -> 1.0
            points <= 0.3 -> 1.25
            points <= 0.7 -> 1.5
            else -> 2.0
        }

        // Для победителей и проигравших
        val directedWeight = if (isWinner) {
            baseWeightFactor
        } else {
            1.8 - baseWeightFactor
        }

        // Специальные корректировки для новичков и ветеранов
        val adjustedWeight = when {
            // Новичок с высоким рейтингом
            player.gamesPlayed < 50 -> {
                directedWeight * (player.gamesPlayed / 100.0)
            }
            // Ветеран
            player.gamesPlayed > 100 -> {
                val experienceFactor = 1.0 + (player.gamesPlayed / 1000.0)
                directedWeight * experienceFactor
            }
            // Обычный случай
            else -> directedWeight
        }

        // Множитель для турнирных игр
        val competitiveMultiplier = if (isCompetitive) 2.5 else 1.0

        // Применяем множитель для турнирных игр
        return adjustedWeight * competitiveMultiplier
    }

    /**
     * Сохраняет историю изменения рейтинга с учетом типа игры
     */
    private fun saveRatingHistory(
        player: User,
        gameId: Long,
        oldMu: Double,
        oldSigma: Double,
        newMu: Double,
        newSigma: Double,
        points: Double,
        isWin: Boolean,
        weight: Double,
        matchQuality: Double,
        isCompetitive: Boolean,
        gameDate: LocalDateTime
    ) {
        val history = PlayerRatingHistory(
            player = player,
            gameId = gameId,
            oldMu = oldMu,
            oldSigma = oldSigma,
            newMu = newMu,
            newSigma = newSigma,
            pointsEarned = points,
            isWin = isWin,
            muDelta = newMu - oldMu,
            weight = weight,
            matchQuality = matchQuality,
            competitive = isCompetitive, // Сохраняем тип игры
            timestamp = gameDate
        )
        ratingHistoryRepository.save(history)
    }

    @Transactional
    fun processBatch(games: List<Game>) {
        games.forEach { game ->
            try {
                updatePlayerRatings(game)
            } catch (e: Exception) {
                // Логирование ошибки, но продолжение обработки
                logger.info("Error processing game ${game.gameId}", e)
            }
        }
    }

    @Transactional
    fun updatePlayerRatings(game: Game) {
        val points = game.points ?: return

        val redTeam = extractTeamWithPoints(game.data.players, points.players) { it.isRed() }
        val blackTeam = extractTeamWithPoints(game.data.players, points.players) { it.isBlack() }
        val isRedWin = game.data.result == PolemicaGameResult.RED_WIN
        val competitive = game.gamePlace.competitionId != null || game.data.tags?.find { it.contains("League") } != null

        updateRatings(
            game.gameId,
            blackTeam,
            redTeam,
            !isRedWin,
            game.started ?: LocalDateTime.now(),
            competitive
        )
    }

    private fun extractTeamWithPoints(
        players: List<PolemicaPlayer>?,
        points: List<PlayerPoints>,
        roleFilter: (Role) -> Boolean
    ): List<Pair<Long, Double>> {
        return players?.filter { roleFilter(it.role) && it.player != null }
            ?.map { player ->
                val playerPoints = points.find { it.position == player.position.value }?.points
                    ?: 0.0
                Pair(player.player!!.id, playerPoints)
            } ?: emptyList()
    }
}
