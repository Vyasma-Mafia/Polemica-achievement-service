package com.github.mafia.vyasma.polemicaachivementservice.repositories

import com.github.mafia.vyasma.polemicaachivementservice.model.jpa.AchievementGameUser
import com.github.mafia.vyasma.polemicaachivementservice.model.jpa.AchievementGameUserKey
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface AchievementGameUserRepository : JpaRepository<AchievementGameUser, AchievementGameUserKey> {
}
