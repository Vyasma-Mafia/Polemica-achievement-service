package com.github.mafia.vyasma.polemicaachivementservice.filters.filters

import com.github.mafia.vyasma.polemicaachivementservice.filters.FilterInput
import com.github.mafia.vyasma.polemicaachivementservice.filters.GameFilter
import com.github.mafia.vyasma.polemicaachivementservice.model.jpa.Game
import com.github.mafia.vyasma.polemicaachivementservice.model.jpa.User

object PointsRangeFilter : GameFilter {
    override val id = "pointsRange"
    override val name = "Диапазон очков"
    override val description = "Фильтр игр по количеству заработанных очков"
    override val hasParameters = true

    override fun check(
        filterInput: FilterInput,
        game: Game,
        player: User
    ): Boolean {
        if (filterInput !is FilterInput.PointsRangeInput) {
            return true // If filter is not active, don't filter out
        }

        val playerData = game.data.players?.find { it.player?.id == player.userId } ?: return false
        val points = game.points?.players?.find { it.position == playerData.position.value }?.points ?: return false

        val min = filterInput.min
        val max = filterInput.max

        return when {
            min != null && max != null -> points >= min && points <= max
            min != null -> points >= min
            max != null -> points <= max
            else -> true // No points filter applied
        }
    }
}
