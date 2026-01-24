package com.github.mafia.vyasma.polemicaachivementservice.filters.filters

import com.github.mafia.vyasma.polemicaachivementservice.filters.FilterInput
import com.github.mafia.vyasma.polemicaachivementservice.filters.GameFilter
import com.github.mafia.vyasma.polemicaachivementservice.model.jpa.Game
import com.github.mafia.vyasma.polemicaachivementservice.model.jpa.User

object DateRangeFilter : GameFilter {
    override val id = "dateRange"
    override val name = "Диапазон дат"
    override val description = "Фильтр игр по дате проведения"
    override val hasParameters = true

    override fun check(
        filterInput: FilterInput,
        game: Game,
        player: User
    ): Boolean {
        if (filterInput !is FilterInput.DateRangeInput) {
            return true // If filter is not active, don't filter out
        }

        val gameDate = game.started?.toLocalDate() ?: return false

        val from = filterInput.from
        val to = filterInput.to

        return when {
            from != null && to != null -> gameDate.isAfter(from.minusDays(1)) && gameDate.isBefore(to.plusDays(1))
            from != null -> gameDate.isAfter(from.minusDays(1))
            to != null -> gameDate.isBefore(to.plusDays(1))
            else -> true // No date filter applied
        }
    }
}
