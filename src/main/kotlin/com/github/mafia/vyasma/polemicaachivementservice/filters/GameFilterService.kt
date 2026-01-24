package com.github.mafia.vyasma.polemicaachivementservice.filters

import com.github.mafia.vyasma.polemicaachivementservice.filters.filters.DateRangeFilter
import com.github.mafia.vyasma.polemicaachivementservice.filters.filters.FirstKickedFilter
import com.github.mafia.vyasma.polemicaachivementservice.filters.filters.PointsRangeFilter
import com.github.mafia.vyasma.polemicaachivementservice.filters.filters.RoleFilter
import com.github.mafia.vyasma.polemicaachivementservice.filters.filters.WinLossFilter
import com.github.mafia.vyasma.polemicaachivementservice.model.jpa.Game
import com.github.mafia.vyasma.polemicaachivementservice.model.jpa.User
import org.springframework.stereotype.Service

@Service
class GameFilterService {

    /**
     * Get all available filters
     */
    fun getAllFilters(): List<GameFilter> {
        return listOf(
            DateRangeFilter,
            FirstKickedFilter,
            RoleFilter,
            WinLossFilter,
            PointsRangeFilter
        )
    }

    /**
     * Get a filter by its ID
     */
    fun getFilterById(id: String): GameFilter? {
        return getAllFilters().find { it.id == id }
    }
    
    /**
     * Apply all active filters to the games list
     * A game must pass ALL active filters (AND logic)
     *
     * @param games The list of games to filter
     * @param player The player to filter games for
     * @param activeFilters Map of filter ID to FilterInput
     * @return Filtered list of games
     */
    fun applyFilters(
        games: List<Game>,
        player: User,
        activeFilters: Map<String, FilterInput>
    ): List<Game> {
        if (activeFilters.isEmpty()) {
            return games
        }

        return games.filter { game ->
            activeFilters.all { (filterId, filterInput) ->
                val filter = getFilterById(filterId)
                filter?.check(filterInput, game, player) ?: true // Unknown filter, don't filter out
            }
        }
    }
}
