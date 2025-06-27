package com.github.mafia.vyasma.polemicaachivementservice.rating

import com.github.mafia.vyasma.polemica.library.client.PolemicaClient
import com.github.mafia.vyasma.polemica.library.utils.compare
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class PolemicaTournamentService(
    val polemicaClient: PolemicaClient
) {
    val logger = LoggerFactory.getLogger(this::class.java)

    fun getPolemicaTournamentResults(tournamentId: Long): List<PolemicaClient.CompetitionPlayerResult> {
        return polemicaClient.getCompetitionResultMetrics(tournamentId, null).sortedWith { o1, o2 -> compare(o1, o2) }
            .reversed()
    }
}
