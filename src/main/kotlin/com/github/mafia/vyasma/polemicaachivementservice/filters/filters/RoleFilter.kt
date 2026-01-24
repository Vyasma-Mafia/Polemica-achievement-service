package com.github.mafia.vyasma.polemicaachivementservice.filters.filters

import com.github.mafia.vyasma.polemicaachivementservice.filters.FilterInput
import com.github.mafia.vyasma.polemicaachivementservice.filters.GameFilter
import com.github.mafia.vyasma.polemicaachivementservice.model.jpa.Game
import com.github.mafia.vyasma.polemicaachivementservice.model.jpa.User

object RoleFilter : GameFilter {
    override val id = "role"
    override val name = "Роль"
    override val description = "Фильтр игр по роли игрока"
    override val hasParameters = true

    override fun check(
        filterInput: FilterInput,
        game: Game,
        player: User
    ): Boolean {
        if (filterInput !is FilterInput.RoleInput) {
            return true // If filter is not active, don't filter out
        }

        val playerData = game.data.players?.find { it.player?.id == player.userId } ?: return false
        return playerData.role == filterInput.role
    }
}
