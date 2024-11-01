package com.github.mafia.vyasma.polemicaachivementservice.achievements.services

import com.github.mafia.vyasma.polemicaachivementservice.achievements.Achievement
import com.github.mafia.vyasma.polemicaachivementservice.model.jpa.Game

interface AchievementTransactionalService {
    fun processAchievementForGame(
        achievement: Achievement,
        game: Game
    )

    fun saveUsersFromGame(game: Game)
}
