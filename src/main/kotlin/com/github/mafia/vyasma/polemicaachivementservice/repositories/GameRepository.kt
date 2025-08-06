package com.github.mafia.vyasma.polemicaachivementservice.repositories

import com.github.mafia.vyasma.polemicaachivementservice.model.jpa.Game
import com.github.mafia.vyasma.polemicaachivementservice.model.jpa.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface GameRepository : JpaRepository<Game, Long> {
    fun findAllWhereByProcessedVersionIs(processedVersion: Long): List<Game>
    fun findAllWhereByGamePlace_ClubId(clubId: Long): List<Game>
    fun findByPointsIsNullOrderByGameId(): List<Game>

    @Query(
        """
        select g
        from Game g
        where g.gameId not in (select ag.gameId from AchievementGame ag where ag.achievement = :achievement)
    """
    )
    fun findAllWhereNotAchievement(achievement: String): List<Game>

    fun countGamesByPointsNotNull(): Long

    @Query(
        """
        select g
        from Game g
        join PlayerRatingHistory p on g.gameId = p.gameId
        where p.player = :player
    """
    )
    fun findAllByUserJoinedFromPlayerRatingHistory(@Param("player") player: User): List<Game>
}
