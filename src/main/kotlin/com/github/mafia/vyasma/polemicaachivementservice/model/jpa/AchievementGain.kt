package com.github.mafia.vyasma.polemicaachivementservice.model.jpa

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.time.LocalDateTime

@Entity
@Table(
    name = "achievement_gains",
    uniqueConstraints = [UniqueConstraint(columnNames = ["achievement", "userId"])]
)
data class AchievementsGain(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    var id: Long? = null,

    @Column(name = "achievement")
    var achievement: String,

    @ManyToOne(optional = false)
    @JoinColumn(name = "userId", referencedColumnName = "userId", nullable = false)
    var user: User,

    @Column(name = "achievementLevel")
    var achievementLevel: Long? = null,

    @CreationTimestamp
    @Column(name = "createdAt", nullable = false, updatable = false)
    var createdAt: LocalDateTime? = null,

    @UpdateTimestamp
    @Column(name = "updatedAt", nullable = false)
    var updatedAt: LocalDateTime? = null
)
