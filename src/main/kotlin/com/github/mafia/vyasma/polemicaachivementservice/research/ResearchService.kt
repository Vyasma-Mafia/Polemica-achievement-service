package com.github.mafia.vyasma.polemicaachivementservice.research

import com.github.mafia.vyasma.polemicaachivementservice.model.game.PolemicaGamePlace
import java.time.LocalDateTime

interface ResearchService {
    fun getGamesWhereFourRedVotesByPerson(): ResearchVotedByFourRedVotesAnswer
}

data class ResearchVotedByFourRedVotesAnswer(
    val toRed: Long,
    val toBlack: Long,
    val games: List<ResearchVotedByFourRedVotesGame>
)

data class ResearchVotedByFourRedVotesGame(
    val gameId: Long,
    val gamePlace: PolemicaGamePlace,
    val gameStarted: LocalDateTime?
)
