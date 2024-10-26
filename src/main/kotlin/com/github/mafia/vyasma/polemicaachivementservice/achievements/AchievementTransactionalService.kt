package com.github.mafia.vyasma.polemicaachivementservice.achievements

import com.github.mafia.vyasma.polemicaachivementservice.model.jpa.Game

interface AchievementTransactionalService {
    fun processAchievementForGame(
        achievement: Achievement,
        game: Game,
        version: Long
    )

    fun saveUsersFromGame(game: Game)
}
