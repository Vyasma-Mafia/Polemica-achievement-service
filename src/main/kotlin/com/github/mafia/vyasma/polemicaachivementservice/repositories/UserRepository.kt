package com.github.mafia.vyasma.polemicaachivementservice.repositories

import com.github.mafia.vyasma.polemicaachivementservice.model.jpa.User
import jakarta.transaction.Transactional
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface UserRepository : JpaRepository<User, Long> {
    fun findTopByOrderByRatingDesc(): User?

    @Modifying(clearAutomatically = true)
    @Transactional
    @Query(
        "update users set mu = null, sigma = null, rating = null, games_played = 0, games_won = 0",
        nativeQuery = true
    )
    fun clearRatingData()

    fun findByUsernameContainingIgnoreCase(username: String, pageable: Pageable): Page<User>
}
