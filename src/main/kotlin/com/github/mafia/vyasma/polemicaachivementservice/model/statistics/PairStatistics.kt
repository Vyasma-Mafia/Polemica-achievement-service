package com.github.mafia.vyasma.polemicaachivementservice.model.statistics

import java.time.LocalDateTime

data class PairStatistics(
    val firstPlayer: PlayerInfo,
    val secondPlayer: PlayerInfo,
    val totalGamesPlayed: Int,
    val sameTeamStatistics: SameTeamStatistics,
    val oppositeTeamStatistics: OppositeTeamStatistics,
    val lastGameTogether: LocalDateTime?,
    val firstGameTogether: LocalDateTime?
)

data class PlayerInfo(
    val playerId: Long,
    val username: String,
    val rating: Double?
)

data class SameTeamStatistics(
    val totalGames: Int,
    val totalWins: Int,
    val winRate: Double,
    val asRed: TeamStatistics,
    val asBlack: TeamStatistics
)

data class TeamStatistics(
    val games: Int,
    val wins: Int,
    val winRate: Double,
    val averagePointsFirst: Double,
    val averagePointsSecond: Double
)

data class OppositeTeamStatistics(
    val totalGames: Int,
    val firstPlayerWins: Int,
    val secondPlayerWins: Int,
    val firstPlayerWinRate: Double,
    val secondPlayerWinRate: Double,
    val firstRedSecondBlack: VersusStatistics,
    val firstBlackSecondRed: VersusStatistics
)

data class VersusStatistics(
    val games: Int,
    val firstPlayerWins: Int,
    val winRate: Double
)

data class PairGame(
    val gameId: Long,
    val date: LocalDateTime,
    val firstPlayerRole: String,
    val secondPlayerRole: String,
    val firstPlayerPoints: Double,
    val secondPlayerPoints: Double,
    val sameTeam: Boolean,
    val result: String,
    val winner: String?,
    val competitive: Boolean
)

data class PairGamesResponse(
    val firstPlayer: PlayerInfo,
    val secondPlayer: PlayerInfo,
    val games: List<PairGame>,
    val totalCount: Int,
    val page: Int,
    val pageSize: Int
)