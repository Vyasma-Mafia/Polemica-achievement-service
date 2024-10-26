package com.github.mafia.vyasma.polemicaachivementservice.repositories

import com.github.mafia.vyasma.polemicaachivementservice.model.jpa.AchievementGain
import com.github.mafia.vyasma.polemicaachivementservice.model.jpa.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface AchievementGainsRepository : JpaRepository<AchievementGain, Long> {
    fun findOneByAchievementAndUserIs(achievement: String, userId: User): AchievementGain?
    fun findAllByUserUsernameIn(usernames: List<String>): List<AchievementGain>
}
