package com.github.mafia.vyasma.polemicaachivementservice.model.statistics

import java.time.LocalDateTime

/**
 * Response containing filtered games for a player
 */
data class FilteredGamesResponse(
    val playerId: Long,
    val username: String,
    val games: List<FilteredGame>,
    val totalCount: Int,
    val filteredCount: Int,
    val activeFilters: Map<String, String> // Filter ID to filter name
)

/**
 * Game information for filtered games list
 */
data class FilteredGame(
    val gameId: Long,
    val date: LocalDateTime?,
    val role: String,
    val points: Double?,
    val isWin: Boolean,
    val result: String,
    val competitive: Boolean,
    val oldRating: Double,
    val ratingChange: Double,
    val newRating: Double,
    val weight: Double
)
