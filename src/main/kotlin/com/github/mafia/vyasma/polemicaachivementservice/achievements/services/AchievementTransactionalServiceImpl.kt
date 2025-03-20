package com.github.mafia.vyasma.polemicaachivementservice.achievements.services

import com.github.mafia.vyasma.polemicaachivementservice.achievements.Achievement
import com.github.mafia.vyasma.polemicaachivementservice.model.jpa.AchievementGame
import com.github.mafia.vyasma.polemicaachivementservice.model.jpa.AchievementGameUser
import com.github.mafia.vyasma.polemicaachivementservice.model.jpa.AchievementGameUserKey
import com.github.mafia.vyasma.polemicaachivementservice.model.jpa.AchievementUser
import com.github.mafia.vyasma.polemicaachivementservice.model.jpa.Game
import com.github.mafia.vyasma.polemicaachivementservice.model.jpa.User
import com.github.mafia.vyasma.polemicaachivementservice.repositories.AchievementGameRepository
import com.github.mafia.vyasma.polemicaachivementservice.repositories.AchievementGameUserRepository
import com.github.mafia.vyasma.polemicaachivementservice.repositories.AchievementUsersRepository
import com.github.mafia.vyasma.polemicaachivementservice.repositories.GameRepository
import com.github.mafia.vyasma.polemicaachivementservice.repositories.UserRepository
import jakarta.transaction.Transactional
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.EnableTransactionManagement

@Service
@EnableTransactionManagement
class AchievementTransactionalServiceImpl(
    val gameRepository: GameRepository,
    val userRepository: UserRepository,
    val achievementUsersRepository: AchievementUsersRepository,
    val achievementGameRepository: AchievementGameRepository,
    val achievementGameUserRepository: AchievementGameUserRepository
) : AchievementTransactionalService {

    @Transactional
    override fun processAchievementForGame(
        achievement: Achievement,
        game: Game
    ) {
        val achievementGame =
            achievementGameRepository.save(AchievementGame(gameId = game.gameId, achievement = achievement.id))
        for (player in game.data.players!!) {
            val checkResult = achievement.check(game.data, player.position)
            if (player.player == null) continue
            if (checkResult == 0) continue
            val user = userRepository.findByIdOrNull(player.player) ?: continue
            achievementGameUserRepository.save(
                AchievementGameUser(
                    AchievementGameUserKey(
                        achievementGameId = achievementGame.id!!,
                        userId = user.userId
                    ),
                    checkResult.toLong()
                )
            )
            val achievementGain = achievementUsersRepository.findOneByAchievementAndUserIs(achievement.id, user)
            if (achievementGain != null) {
                achievementGain.achievementCounter = achievementGain.achievementCounter?.plus(checkResult)
                achievementUsersRepository.save(achievementGain)
            } else {
                achievementUsersRepository.save(
                    AchievementUser(
                        achievement = achievement.id,
                        user = user,
                        achievementCounter = checkResult.toLong()
                    )
                )
            }
        }
    }

    @Transactional
    override fun saveUsersFromGame(game: Game) {
        game.data.players?.forEach { player ->
            val playerId = player.player
            if (playerId != null) {
                userRepository.save(User(playerId, player.username))
            }
        }
        game.processedVersion += 1
        gameRepository.save(game)
    }
}
