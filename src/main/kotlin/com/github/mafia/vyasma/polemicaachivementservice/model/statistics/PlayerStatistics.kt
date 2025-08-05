package com.github.mafia.vyasma.polemicaachivementservice.model.statistics

import com.github.mafia.vyasma.polemica.library.model.game.Role

data class PlayerStatistics(
    val playerId: Long,
    val username: String,
    val totalGames: Int,
    val totalWins: Int,
    val winRate: Double,
    val roleStatistics: Map<Role, RoleStatistics>,
    val averagePoints: Double,
    val lastGameDate: String?
)

data class RoleStatistics(
    val role: Role,
    val gamesPlayed: Int,
    val gamesWon: Int,
    val winRate: Double,
    val averagePoints: Double,
    val bestGame: GameSummary?,
    val worstGame: GameSummary?
)

data class GameSummary(
    val gameId: Long,
    val date: String,
    val points: Double,
    val result: String
)

data class PlayerSearchResult(
    val playerId: Long,
    val username: String,
    val rating: Double?,
    val gamesPlayed: Int
)