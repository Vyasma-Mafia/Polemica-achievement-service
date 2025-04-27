package com.github.mafia.vyasma.polemicaachivementservice.model.jpa

import com.github.mafia.vyasma.polemicaachivementservice.rating.DEFAULT_MU
import com.github.mafia.vyasma.polemicaachivementservice.rating.DEFAULT_SIGMA
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.time.LocalDateTime
@Entity
@Table(name = "Users")
data class User(
    @Id
    @Column(name = "user_id", nullable = false)
    var userId: Long,

    @Column(name = "username", nullable = false, length = 50, unique = true)
    var username: String,

    @Column(name = "mu")
    var mu: Double? = DEFAULT_MU,

    @Column(name = "sigma")
    var sigma: Double? = DEFAULT_SIGMA / 3.0,

    @Column(name = "rating")
    var rating: Double? = 0.0,

    @Column(name = "games_played")
    var gamesPlayed: Int = 0,

    @Column(name = "games_won")
    var gamesWon: Int = 0,

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: LocalDateTime? = null,

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime? = null
) {
    // Вычисляемое свойство для получения процента побед
    @Transient
    var winRate: Double =
        if (gamesPlayed > 0) {
            gamesWon * 100.0 / gamesPlayed
        } else {
            0.0
        }

    // Метод для получения консервативной оценки рейтинга
    @Transient
    var conservativeRating = {
        val currentMu = mu ?: DEFAULT_MU
        val currentSigma = sigma ?: DEFAULT_SIGMA
        currentMu - 3 * currentSigma
    }
}
