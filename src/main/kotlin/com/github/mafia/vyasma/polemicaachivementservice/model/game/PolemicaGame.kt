package com.github.mafia.vyasma.polemicaachivementservice.model.game

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.time.LocalDateTime

@JsonIgnoreProperties(ignoreUnknown = true)
data class PolemicaGame(
    val id: Long,
    val master: Long,
    val referee: PolemicaUser,
    val scoringVersion: String?,
    val scoringType: Int,
    val version: Int,
    val tags: List<String>,
    val players: List<PolemicaPlayer>,
    val shots: List<PolemicaShot>,
    val stage: Stage,
    val votes: List<PolemicaVote>,
    val comKiller: Int?,
    val bonuses: List<PolemicaBonus>,
    val started: LocalDateTime,
    val stop: Stage?,
    val result: PolemicaGameResult?
)
