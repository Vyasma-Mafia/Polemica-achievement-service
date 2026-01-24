package com.github.mafia.vyasma.polemicaachivementservice.filters.filters

import com.github.mafia.vyasma.polemica.library.utils.isBlack
import com.github.mafia.vyasma.polemica.library.utils.isBlackWin
import com.github.mafia.vyasma.polemica.library.utils.isRed
import com.github.mafia.vyasma.polemica.library.utils.isRedWin
import com.github.mafia.vyasma.polemicaachivementservice.filters.FilterInput
import com.github.mafia.vyasma.polemicaachivementservice.filters.GameFilter
import com.github.mafia.vyasma.polemicaachivementservice.model.jpa.Game
import com.github.mafia.vyasma.polemicaachivementservice.model.jpa.User

object WinLossFilter : GameFilter {
    override val id = "winLoss"
    override val name = "Победа/Поражение"
    override val description = "Фильтр игр по результату (победа или поражение)"
    override val hasParameters = true

    override fun check(
        filterInput: FilterInput,
        game: Game,
        player: User
    ): Boolean {
        if (filterInput !is FilterInput.WinLossInput) {
            return true // If filter is not active, don't filter out
        }

        val isWinFilter = filterInput.isWin ?: return true // null means all games

        val playerData = game.data.players?.find { it.player?.id == player.userId } ?: return false
        val role = playerData.role

        val isWin = when {
            role.isRed() && game.data.isRedWin() -> true
            role.isBlack() && game.data.isBlackWin() -> true
            else -> false
        }

        return isWin == isWinFilter
    }
}
