package com.github.mafia.vyasma.polemicaachivementservice.model.jpa

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "processed_tournament_ids")
data class ProcessedTournamentId(
    @Id
    val id: Long,

    @Column(name = "processed_at", nullable = false)
    val processedAt: LocalDateTime = LocalDateTime.now()
)
