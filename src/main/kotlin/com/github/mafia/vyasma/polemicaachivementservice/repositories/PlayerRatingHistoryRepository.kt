package com.github.mafia.vyasma.polemicaachivementservice.repositories

import com.github.mafia.vyasma.polemicaachivementservice.model.jpa.PlayerRatingHistory
import com.github.mafia.vyasma.polemicaachivementservice.model.jpa.User
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface PlayerRatingHistoryRepository : JpaRepository<PlayerRatingHistory, Long> {
    fun findByPlayer(player: User, pageable: PageRequest): Page<PlayerRatingHistory>
    fun findByPlayerOrderByTimestampAsc(player: User): List<PlayerRatingHistory>
    fun findByGameId(gameId: Long): List<PlayerRatingHistory>
    fun findByPlayerAndTimestampAfterOrderByTimestampAsc(
        player: User,
        timestamp: LocalDateTime
    ): List<PlayerRatingHistory>

    @Query("SELECT h FROM PlayerRatingHistory h WHERE h.player = :player ORDER BY h.timestamp DESC LIMIT :limit")
    fun findLastNHistoryByPlayer(@Param("player") player: User, @Param("limit") limit: Int): List<PlayerRatingHistory>
    fun findByPlayerOrderByPointsEarnedDesc(player: User): List<PlayerRatingHistory>
    fun findByPlayerOrderByPointsEarnedAsc(player: User): List<PlayerRatingHistory>
    fun findByPlayerAndCompetitive(player: User?, competitive: Boolean, ascending: Sort): List<PlayerRatingHistory>
    fun findAllByPlayer(playerId: User): List<PlayerRatingHistory>
}
