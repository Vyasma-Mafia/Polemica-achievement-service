package com.github.mafia.vyasma.polemicaachivementservice.model.jpa

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "tournaments")
data class Tournament(
    @Id
    val id: Long,

    @Column(nullable = false)
    val name: String,

    @Column(name = "games_per_series", nullable = false)
    val gamesPerSeries: Int = 4,

    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(nullable = false)
    val active: Boolean = true
)
