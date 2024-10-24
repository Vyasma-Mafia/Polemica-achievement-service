package com.github.mafia.vyasma.polemicaachivementservice.model.jpa

import com.github.mafia.vyasma.polemicaachivementservice.model.game.PolemicaGame
import jakarta.persistence.Column
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
    @Column(name = "gameId", nullable = false)
    var gameId: Long,

    @Column(name = "processedVersion", nullable = false)
    var processedVersion: Long,

    // @JdbcTypeCode(SqlTypes.JSON)
    // val data: PolemicaGame,

    @CreationTimestamp
    @Column(name = "createdAt", nullable = false, updatable = false)
    var createdAt: LocalDateTime? = null,

    @UpdateTimestamp
    @Column(name = "updatedAt", nullable = false)
    var updatedAt: LocalDateTime? = null
)
