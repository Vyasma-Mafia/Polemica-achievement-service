package com.github.mafia.vyasma.polemicaachivementservice.repositories

import com.github.mafia.vyasma.polemicaachivementservice.model.jpa.Game
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface GameRepository : JpaRepository<Game, Long> {
    fun findAllWhereByProcessedVersionIs(processedVersion: Long): List<Game>

    @Query(
        """
        select g
        from Game g
        where g.gameId not in (select ag.gameId from AchievementGame ag where ag.achievement = :achievement) 
    """
    )
    fun findAllWhereNotAchievement(achievement: String): List<Game>
}
