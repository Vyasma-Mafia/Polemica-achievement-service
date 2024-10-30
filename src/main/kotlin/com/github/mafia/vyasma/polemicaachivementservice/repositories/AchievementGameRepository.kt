package com.github.mafia.vyasma.polemicaachivementservice.repositories

import com.github.mafia.vyasma.polemicaachivementservice.model.jpa.AchievementGame
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface AchievementGameRepository : JpaRepository<AchievementGame, Long> {
}
