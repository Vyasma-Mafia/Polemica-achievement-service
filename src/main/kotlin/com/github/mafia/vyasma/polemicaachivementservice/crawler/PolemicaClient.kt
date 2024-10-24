package com.github.mafia.vyasma.polemicaachivementservice.crawler

import com.github.mafia.vyasma.polemicaachivementservice.model.game.PolemicaGame

interface PolemicaClient {

    fun getGameFromClub(clubGameId: PolemicaClubGameId): PolemicaGame

    data class PolemicaClubGameId(val clubId: Long, val gameId: Long)
}
