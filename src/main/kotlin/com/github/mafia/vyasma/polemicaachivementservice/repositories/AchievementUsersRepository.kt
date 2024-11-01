package com.github.mafia.vyasma.polemicaachivementservice.repositories

import com.github.mafia.vyasma.polemicaachivementservice.model.jpa.AchievementUser
import com.github.mafia.vyasma.polemicaachivementservice.model.jpa.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface AchievementUsersRepository : JpaRepository<AchievementUser, Long> {
    fun findOneByAchievementAndUserIs(achievement: String, userId: User): AchievementUser?
    fun findAllByUserUsernameInOrUserUserIdIn(
        userUsername: Collection<String>,
        user: Collection<Long>
    ): List<AchievementUser>

    @Query(
        value = """
        SELECT * FROM (
            SELECT 
                au.*,
                ROW_NUMBER() OVER (PARTITION BY au.achievement ORDER BY au.achievement_counter DESC) AS rk
            FROM achievement_users au
            WHERE au.user_id IN :userIds
        ) ranked
        WHERE ranked.rk <= :rankLimit
        """,
        nativeQuery = true
    )
    fun findTopByAchievementCounterForEveryAchievementWhereUserIdIn(
        @Param("userIds") userIds: List<Long>,
        @Param("rankLimit") rankLimit: Int
    ): List<AchievementUser>
}
