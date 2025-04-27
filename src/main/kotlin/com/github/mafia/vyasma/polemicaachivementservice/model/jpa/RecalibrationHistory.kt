package com.github.mafia.vyasma.polemicaachivementservice.model.jpa

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "recalibration_history")
data class RecalibrationHistory(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_id", nullable = false)
    val player: User,

    @Column(name = "game_id", nullable = false)
    val gameId: Long,

    @Column(name = "game_number", nullable = false)
    val gameNumber: Int,

    @Column(name = "old_sigma", nullable = false)
    val oldSigma: Double,

    @Column(name = "new_sigma", nullable = false)
    val newSigma: Double,

    @Column(name = "reason", nullable = false)
    val reason: String,

    @Column(name = "timestamp", nullable = false)
    val timestamp: LocalDateTime
)
