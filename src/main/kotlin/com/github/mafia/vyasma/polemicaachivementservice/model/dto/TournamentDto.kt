package com.github.mafia.vyasma.polemicaachivementservice.model.dto

data class AddTournamentRequest(
    val id: Long,
    val name: String,
    val gamesPerSeries: Int = 4
)

data class TournamentResponse(
    val id: Long,
    val name: String,
    val gamesPerSeries: Int,
    val active: Boolean
)