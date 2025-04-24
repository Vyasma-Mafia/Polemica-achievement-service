package com.github.mafia.vyasma.polemicaachivementservice.repositories

import com.github.mafia.vyasma.polemicaachivementservice.model.jpa.AchievementGameUser
import com.github.mafia.vyasma.polemicaachivementservice.model.jpa.AchievementGameUserKey
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface AchievementGameUserRepository : JpaRepository<AchievementGameUser, AchievementGameUserKey> {

    // Существующие методы

    // Метод для подсчета достижений с фильтрацией по дате
    @Query(
        value = """
        SELECT 
            u.user_id as userId,
            u.username as username,
            ag.achievement as achievementId,
            SUM(agu.achievement_counter) as counter
        FROM 
            achievement_game_user agu
            JOIN achievement_game ag ON agu.achievement_game_id = ag.achievement_game_id
            JOIN games g ON ag.game_id = g.game_id
            JOIN users u ON agu.user_id = u.user_id
        WHERE 
            (u.username IN :usernames OR u.user_id IN :userIds)
            AND (g.started >= :startDate)
        GROUP BY 
            u.user_id, u.username, ag.achievement
        """,
        nativeQuery = true
    )
    fun findAchievementSummaryByUsernamesOrUserIdsAndAfterDate(
        @Param("usernames") usernames: List<String>,
        @Param("userIds") userIds: List<Long>,
        @Param("startDate") startDate: LocalDateTime?
    ): List<AchievementSummaryProjection>

    // Метод для получения топ пользователей с фильтрацией по дате
    @Query(
        value = """
        WITH AchievementSummary AS (
            SELECT 
                u.user_id as userId,
                u.username as username,
                ag.achievement as achievementId,
                SUM(agu.achievement_counter) as counter
            FROM 
                achievement_game_user agu
                JOIN achievement_game ag ON agu.achievement_game_id = ag.achievement_game_id
                JOIN games g ON ag.game_id = g.game_id
                JOIN users u ON agu.user_id = u.user_id
            WHERE 
                u.user_id IN :userIds
                AND (g.started >= :startDate)
            GROUP BY 
                u.user_id, u.username, ag.achievement
        ),
        RankedAchievements AS (
            SELECT 
                *,
                ROW_NUMBER() OVER (PARTITION BY achievementId ORDER BY counter DESC) AS rank
            FROM 
                AchievementSummary
        )
        SELECT 
            userId, username, achievementId, counter
        FROM 
            RankedAchievements
        WHERE 
            rank <= :rankLimit
        """,
        nativeQuery = true
    )
    fun findTopAchievementSummaryByUserIdsAndAfterDate(
        @Param("userIds") userIds: List<Long>,
        @Param("startDate") startDate: LocalDateTime,
        @Param("rankLimit") rankLimit: Int
    ): List<AchievementSummaryProjection>
}

// Проекция для результатов запросов
interface AchievementSummaryProjection {
    fun getUserId(): Long
    fun getUsername(): String
    fun getAchievementId(): String
    fun getCounter(): Long
}
