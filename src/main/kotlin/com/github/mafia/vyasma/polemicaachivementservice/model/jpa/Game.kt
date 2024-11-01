package com.github.mafia.vyasma.polemicaachivementservice.model.jpa

import com.github.mafia.vyasma.polemicaachivementservice.model.game.PolemicaGame
import com.github.mafia.vyasma.polemicaachivementservice.model.game.PolemicaGamePlace
import jakarta.persistence.Column
import jakarta.persistence.Embedded
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.annotations.UpdateTimestamp
import org.hibernate.type.SqlTypes
import java.time.LocalDateTime

@Entity
@Table(name = "Games")
data class Game(
    @Id
    @Column(name = "game_id", nullable = false)
    var gameId: Long,

    @Column(name = "processed_version", nullable = false)
    var processedVersion: Long = 0,

    @JdbcTypeCode(SqlTypes.JSON)
    val data: PolemicaGame,

    @Embedded
    val gamePlace: PolemicaGamePlace,

    @Column(name = "started", nullable = false, updatable = false)
    val started: LocalDateTime? = null,

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: LocalDateTime? = null,

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime? = null
)
