package com.github.mafia.vyasma.polemicaachivementservice.repositories

import com.github.mafia.vyasma.polemicaachivementservice.model.jpa.RecalibrationHistory
import com.github.mafia.vyasma.polemicaachivementservice.model.jpa.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface RecalibrationHistoryRepository : JpaRepository<RecalibrationHistory, Long> {

    fun findByPlayerOrderByTimestampDesc(player: User): List<RecalibrationHistory>

    @Query("SELECT r FROM RecalibrationHistory r WHERE r.player = :player AND r.gameNumber BETWEEN :fromGameNumber AND :toGameNumber")
    fun findByPlayerAndGameNumberRange(
        @Param("player") player: User,
        @Param("fromGameNumber") fromGameNumber: Int,
        @Param("toGameNumber") toGameNumber: Int
    ): List<RecalibrationHistory>
}
