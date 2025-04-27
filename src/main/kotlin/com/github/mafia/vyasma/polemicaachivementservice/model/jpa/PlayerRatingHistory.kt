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
@Table(name = "player_rating_history")
data class PlayerRatingHistory(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_id", nullable = false)
    val player: User,

    @Column(name = "game_id", nullable = false)
    val gameId: Long,

    @Column(name = "old_mu", nullable = false)
    val oldMu: Double,

    @Column(name = "old_sigma", nullable = false)
    val oldSigma: Double,

    @Column(name = "new_mu", nullable = false)
    val newMu: Double,

    @Column(name = "new_sigma", nullable = false)
    val newSigma: Double,

    @Column(name = "points_earned", nullable = false)
    val pointsEarned: Double,

    @Column(name = "is_win", nullable = false)
    val isWin: Boolean,

    @Column(name = "mu_delta", nullable = false)
    val muDelta: Double,

    @Column(name = "weight", nullable = false)
    val weight: Double,

    @Column(name = "match_quality", nullable = false)
    val matchQuality: Double,

    @Column(name = "is_competitive", nullable = false)
    val competitive: Boolean = false,  // Добавлено новое поле

    @Column(name = "timestamp", nullable = false)
    val timestamp: LocalDateTime = LocalDateTime.now()
)
