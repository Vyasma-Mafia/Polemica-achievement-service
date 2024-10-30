package com.github.mafia.vyasma.polemicaachivementservice.model.jpa

import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import jakarta.persistence.EmbeddedId
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.io.Serializable

@Entity
@Table(name = "achievement_game_user")
data class AchievementGameUser(

    // Composite key of (achievement_game_id, user_id)
    @EmbeddedId
    var id: AchievementGameUserKey,

    @Column(name = "achievement_counter")
    var achievementCounter: Long? = null,

    ) {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "achievement_game_id",
        insertable = false,
        updatable = false,
        nullable = false
    )
    var achievementGame: AchievementGame? = null
}

// Composite key class for AchievementGameUser
@Embeddable
data class AchievementGameUserKey(
    @Column(name = "achievement_game_id", nullable = false)
    var achievementGameId: Long,

    @Column(name = "user_id", nullable = false)
    var userId: Long
) : Serializable
