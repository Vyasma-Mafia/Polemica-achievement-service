package com.github.mafia.vyasma.polemicaachivementservice.filters

import com.github.mafia.vyasma.polemica.library.model.game.Role
import java.time.LocalDate

/**
 * Sealed class representing different types of filter inputs
 */
sealed class FilterInput {
    /**
     * No parameters needed for this filter
     */
    object NoInput : FilterInput()

    /**
     * Date range filter input
     */
    data class DateRangeInput(
        val from: LocalDate?,
        val to: LocalDate?
    ) : FilterInput()

    /**
     * Role filter input
     */
    data class RoleInput(
        val role: Role
    ) : FilterInput()

    /**
     * Win/Loss filter input
     */
    data class WinLossInput(
        val isWin: Boolean?
    ) : FilterInput() // null means all games

    /**
     * Points range filter input
     */
    data class PointsRangeInput(
        val min: Double?,
        val max: Double?
    ) : FilterInput()
}
