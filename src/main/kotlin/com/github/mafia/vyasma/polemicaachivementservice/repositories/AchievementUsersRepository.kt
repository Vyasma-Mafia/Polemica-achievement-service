package com.github.mafia.vyasma.polemicaachivementservice.repositories

import com.github.mafia.vyasma.polemicaachivementservice.model.jpa.AchievementUser
import com.github.mafia.vyasma.polemicaachivementservice.model.jpa.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface AchievementUsersRepository : JpaRepository<AchievementUser, Long> {
    fun findOneByAchievementAndUserIs(achievement: String, userId: User): AchievementUser?
    fun findAllByUserUsernameInOrUserUserIdIn(
        userUsername: Collection<String>,
        user: Collection<Long>
    ): List<AchievementUser>
}
