package com.github.mafia.vyasma.polemicaachivementservice.crawler

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.github.mafia.vyasma.polemicaachivementservice.model.game.PolemicaGame
import com.github.mafia.vyasma.polemicaachivementservice.model.game.PolemicaGameResult
import com.github.mafia.vyasma.polemicaachivementservice.model.game.PolemicaUser
import java.time.LocalDateTime

interface PolemicaClient {

    fun getGameFromClub(clubGameId: PolemicaClubGameId): PolemicaGame
    fun getGamesFromClub(clubId: Long, offset: Long, limit: Long): List<PolemicaGameReference>

    data class PolemicaClubGameId(val clubId: Long, val gameId: Long, val version: Long? = null)

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class PolemicaGameReference(
        val id: Long,
        val started: LocalDateTime,
        val result: PolemicaGameResult?,
        val referee: PolemicaUser,
        val version: Long?
    )
}
