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

fun PolemicaGame.getFinalVotes(): List<FinalVote> {
    return this.votes.groupBy { it.day }.map { (day, votes) ->
        val votesNumMax = votes.map { it.num }.max()
        votes.dropWhile { it.num < votesNumMax }
        val realVotes = votes.filter { it.num == votesNumMax }
        val votingResult = realVotes.groupBy { it.candidate } // candidate -> [voters]
        if (votingResult.isEmpty()) {
            emptyList()
        } else if (votingResult.values.map { it.size }.toSet().size == 1 && votingResult.size > 1) { // попил
            val convicted = votingResult.keys.toList()
            votes.filter { it.num == 0 }.map { FinalVote(day, it.voter, convicted) }
        } else {
            val convicted = votingResult.maxBy { it.value.size }.key
            realVotes.filter { it.candidate == convicted }.map { FinalVote(day, it.voter, listOf(convicted)) }
        }
    }.flatten()
}

data class KilledPlayer(val position: Int?, val night: Int)
data class FinalVote(val day: Int, val position: Int, val convicted: List<Int>)
