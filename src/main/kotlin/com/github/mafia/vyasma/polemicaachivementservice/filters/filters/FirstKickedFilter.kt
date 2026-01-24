package com.github.mafia.vyasma.polemicaachivementservice.filters.filters

import com.github.mafia.vyasma.polemica.library.utils.getKickedFromTable
import com.github.mafia.vyasma.polemicaachivementservice.filters.FilterInput
import com.github.mafia.vyasma.polemicaachivementservice.filters.GameFilter
import com.github.mafia.vyasma.polemicaachivementservice.model.jpa.Game
import com.github.mafia.vyasma.polemicaachivementservice.model.jpa.User

object FirstKickedFilter : GameFilter {
    override val id = "firstKilled"
    override val name = "Был первым убитым"
    override val description = "Показать игры, где игрок был первым покинувшим стол"
    override val hasParameters = false

    override fun check(
        filterInput: FilterInput,
        game: Game,
        player: User
    ): Boolean {
        val playerData = game.data.players?.find { it.player?.id == player.userId } ?: return false
        val playerPosition = playerData.position

        val kicked = game.data.getKickedFromTable()
        if (kicked.isEmpty()) {
            return false
        }

        val firstKicked = kicked.first()
        // Check if this player was the first kicked in the first game phase
        val firstPhaseKicked = kicked.filter { it.gamePhase == firstKicked.gamePhase }

        return firstPhaseKicked.size == 1 && firstPhaseKicked.first().position == playerPosition
    }
}
