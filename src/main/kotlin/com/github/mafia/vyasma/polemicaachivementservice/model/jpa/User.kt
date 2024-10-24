package com.github.mafia.vyasma.polemicaachivementservice.model.jpa

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.time.LocalDateTime

@Entity
@Table(name = "Users", uniqueConstraints = [UniqueConstraint(columnNames = ["username"])])
data class User(
    @Id
    @Column(name = "userId", nullable = false)
    var userId: Long,

    @Column(name = "username", nullable = false, length = 50, unique = true)
    var username: String,

    @CreationTimestamp
    @Column(name = "createdAt", nullable = false, updatable = false)
    var createdAt: LocalDateTime? = null,

    @UpdateTimestamp
    @Column(name = "updatedAt", nullable = false)
    var updatedAt: LocalDateTime? = null
)
