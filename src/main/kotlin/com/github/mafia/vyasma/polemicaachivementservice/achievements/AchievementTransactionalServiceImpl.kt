package com.github.mafia.vyasma.polemicaachivementservice.achievements

import com.github.mafia.vyasma.polemicaachivementservice.model.jpa.AchievementGain
import com.github.mafia.vyasma.polemicaachivementservice.model.jpa.Game
import com.github.mafia.vyasma.polemicaachivementservice.model.jpa.User
import com.github.mafia.vyasma.polemicaachivementservice.repositories.AchievementGainsRepository
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
    val achievementGainsRepository: AchievementGainsRepository,
) : AchievementTransactionalService {

    @Transactional
    override fun processAchievementForGame(
        achievement: Achievement,
        game: Game,
        version: Long
    ) {
        for (player in game.data.players) {
            val checkResult = achievement.check(game.data, player.position)
            if (checkResult == 0) continue
            if (player.player == null) continue
            val user = userRepository.findByIdOrNull(player.player) ?: continue
            val achievementGain = achievementGainsRepository.findOneByAchievementAndUserIs(achievement.id, user)
            if (achievementGain != null) {
                achievementGain.achievementCounter = achievementGain.achievementCounter?.plus(checkResult)
                achievementGainsRepository.save(achievementGain)
            } else {
                achievementGainsRepository.save(
                    AchievementGain(
                        achievement = achievement.id,
                        user = user,
                        achievementCounter = checkResult.toLong()
                    )
                )
            }
        }
        game.processedVersion = version + 1
        gameRepository.save(game)
    }

    @Transactional
    override fun saveUsersFromGame(game: Game) {
        game.data.players.forEach { player ->
            if (player.player != null) {
                userRepository.save(User(player.player, player.username))
            }
        }
        game.processedVersion = 1
        gameRepository.save(game)
    }
}
