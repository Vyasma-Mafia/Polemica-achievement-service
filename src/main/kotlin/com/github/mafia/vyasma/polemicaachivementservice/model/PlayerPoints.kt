package com.github.mafia.vyasma.polemicaachivementservice.model

data class PolemicaGamePlayersPoints(
    val success: Boolean,
    val players: List<PlayerPoints>
)

data class PlayerPoints(
    val position: Int,
    val points: Double
)
