package com.github.mafia.vyasma.polemicaachivementservice.services

import com.github.mafia.vyasma.polemica.library.client.PolemicaClient
import com.github.mafia.vyasma.polemicaachivementservice.model.dto.AddTournamentRequest
import com.github.mafia.vyasma.polemicaachivementservice.model.dto.TournamentResponse
import com.github.mafia.vyasma.polemicaachivementservice.model.jpa.Tournament
import com.github.mafia.vyasma.polemicaachivementservice.repositories.TournamentRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
@Transactional
class TournamentService(
    private val tournamentRepository: TournamentRepository,
    private val polemicaClient: PolemicaClient
) {
    private val logger = LoggerFactory.getLogger(TournamentService::class.java)

    fun getActiveTournaments(): List<Tournament> {
        return tournamentRepository.findByActiveTrue()
    }

    fun getActiveTournamentsAsResponse(): List<TournamentResponse> {
        return getActiveTournaments().map { tournament ->
            TournamentResponse(
                id = tournament.id,
                name = tournament.name,
                gamesPerSeries = tournament.gamesPerSeries,
                active = tournament.active
            )
        }
    }

    fun addTournament(request: AddTournamentRequest): TournamentResponse {
        // Валидируем входные данные
        if (request.name.isBlank()) {
            throw IllegalArgumentException("Название турнира не может быть пустым")
        }
        if (request.gamesPerSeries < 1) {
            throw IllegalArgumentException("Количество игр в серии должно быть больше 0")
        }

        // Проверяем, существует ли турнир с таким ID
        val existingTournament = tournamentRepository.findById(request.id).orElse(null)

        if (existingTournament != null) {
            // Если турнир уже активен, выбрасываем ошибку
            if (existingTournament.active) {
                throw IllegalArgumentException("Турнир с ID ${request.id} уже существует")
            }

            // Если турнир существует, но неактивен - реактивируем его
            val reactivatedTournament = existingTournament.copy(
                name = request.name,
                gamesPerSeries = request.gamesPerSeries,
                active = true
            )

            val savedTournament = tournamentRepository.save(reactivatedTournament)
            logger.info("Турнир ${savedTournament.name} (ID: ${savedTournament.id}) реактивирован")

            return TournamentResponse(
                id = savedTournament.id,
                name = savedTournament.name,
                gamesPerSeries = savedTournament.gamesPerSeries,
                active = savedTournament.active
            )
        }

        // Валидируем, что турнир существует в Polemica (опционально)
        try {
            val games = polemicaClient.getGamesFromCompetition(request.id)
            logger.info("Турнир ${request.id} найден в Polemica, содержит ${games.size} игр")
        } catch (e: Exception) {
            logger.warn("Не удалось проверить турнир ${request.id} в Polemica: ${e.message}")
            // Не блокируем добавление, так как турнир может быть новым
        }

        // Создаем новый турнир
        val tournament = Tournament(
            id = request.id,
            name = request.name,
            gamesPerSeries = request.gamesPerSeries,
            createdAt = LocalDateTime.now(),
            active = true
        )

        val savedTournament = tournamentRepository.save(tournament)
        logger.info("Добавлен новый турнир: ${savedTournament.name} (ID: ${savedTournament.id})")

        return TournamentResponse(
            id = savedTournament.id,
            name = savedTournament.name,
            gamesPerSeries = savedTournament.gamesPerSeries,
            active = savedTournament.active
        )
    }

    fun removeTournament(id: Long) {
        val tournament = tournamentRepository.findByIdAndActiveTrue(id)
            ?: throw IllegalArgumentException("Активный турнир с ID $id не найден")

        // Мягкое удаление - помечаем как неактивный
        val updatedTournament = tournament.copy(active = false)
        tournamentRepository.save(updatedTournament)

        logger.info("Турнир ${tournament.name} (ID: $id) помечен как неактивный")
    }

    fun getTournamentById(id: Long): Tournament? {
        return tournamentRepository.findByIdAndActiveTrue(id)
    }

    fun validateTournamentExists(id: Long): Boolean {
        return try {
            val games = polemicaClient.getGamesFromCompetition(id)
            games.isNotEmpty()
        } catch (e: Exception) {
            logger.warn("Ошибка при проверке турнира $id: ${e.message}")
            false
        }
    }
}
