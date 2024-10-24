package com.github.mafia.vyasma.polemicaachivementservice.crawler

import com.github.mafia.vyasma.polemicaachivementservice.model.game.PolemicaGame
import org.springframework.web.reactive.function.client.WebClient

class PolemicaClientImpl(private val polemicaWebClient: WebClient) : PolemicaClient {

    override fun getGameFromClub(clubGameId: PolemicaClient.PolemicaClubGameId): PolemicaGame {
        return polemicaWebClient.get()
            .uri("/v1/clubs/${clubGameId.clubId}/games/${clubGameId.gameId}")
            .retrieve()
            .bodyToMono(PolemicaGame::class.java)
            .block() ?: throw RuntimeException("Game not found")
    }
}
