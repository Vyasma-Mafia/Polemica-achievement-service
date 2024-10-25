package com.github.mafia.vyasma.polemicaachivementservice.crawler

import com.github.mafia.vyasma.polemicaachivementservice.model.game.PolemicaGame
import org.springframework.web.reactive.function.client.WebClient

class PolemicaClientImpl(private val polemicaWebClient: WebClient) : PolemicaClient {

    override fun getGameFromClub(clubGameId: PolemicaClient.PolemicaClubGameId): PolemicaGame {
        return polemicaWebClient.get()
            .uri(
                "/v1/clubs/${clubGameId.clubId}/games/${clubGameId.gameId}" + getVersionQueryParam(clubGameId)
            )
            .retrieve()
            .bodyToMono(PolemicaGame::class.java)
            .block() ?: throw RuntimeException("Game not found")
    }

    private fun getVersionQueryParam(clubGameId: PolemicaClient.PolemicaClubGameId) =
        if (clubGameId.version != null) {
            "?version=${clubGameId.version}"
        } else {
            ""
        }

    override fun getGamesFromClub(clubId: Long, offset: Long, limit: Long): List<PolemicaClient.PolemicaGameReference> {
        return polemicaWebClient.get()
            .uri("/v1/clubs/${clubId}/games")
            .retrieve()
            .bodyToFlux(PolemicaClient.PolemicaGameReference::class.java)
            .collectList()
            .block() ?: throw RuntimeException("Club not found")
    }
}
