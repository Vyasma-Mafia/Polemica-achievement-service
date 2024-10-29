package com.github.mafia.vyasma.polemicaachivementservice.utils

import com.github.mafia.vyasma.polemicaachivementservice.model.game.PolemicaGame
import com.github.mafia.vyasma.polemicaachivementservice.model.game.Role

fun PolemicaGame.getRealComKiller(): Int? {
    if (getFirstKilled()?.let { this.getPositionRole(it) } != Role.SHERIFF) {
        return null
    }
    return this.comKiller ?: this.getDon()
}

fun PolemicaGame.getFirstKilled() = this.getKilled().find { it.night == 1 }?.position

fun PolemicaGame.getKilled(): List<KilledPlayer> {
    // all shots victims in nights equal
    return this.shots.groupBy { it.night }.map { (night, shots) ->
        val candidates = shots.map { it.victim }.toSet()
        if (candidates.size == 1) {
            KilledPlayer(candidates.first(), night)
        } else {
            KilledPlayer(null, night)
        }
    }
}

fun PolemicaGame.getDon(): Int {
    return this.players.find { it.role == Role.DON }!!.position
}

fun PolemicaGame.getPositionRole(position: Int): Role {
    return this.players.find { it.position == position }!!.role
}

data class KilledPlayer(val position: Int?, val night: Int)
