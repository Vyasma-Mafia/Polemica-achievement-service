package com.github.mafia.vyasma.polemicaachivementservice.filters

import com.github.mafia.vyasma.polemicaachivementservice.model.jpa.Game
import com.github.mafia.vyasma.polemicaachivementservice.model.jpa.User

/**
 * Interface for game filters, similar to Achievement interface
 * Filters are functions that take filter input, game, and player, and return boolean
 */
interface GameFilter {
    /**
     * Unique identifier for the filter
     */
    val id: String

    /**
     * Display name for the filter
     */
    val name: String

    /**
     * Description of what the filter does
     */
    val description: String

    /**
     * Whether this filter requires parameters
     */
    val hasParameters: Boolean
        get() = false

    /**
     * Check if a game matches the filter criteria
     * @param filterInput The input parameters for the filter
     * @param game The game to check
     * @param player The player to check against
     * @return true if the game matches the filter, false otherwise
     */
    fun check(
        filterInput: FilterInput,
        game: Game,
        player: User
    ): Boolean
}
