package com.github.mafia.vyasma.polemicaachivementservice.research

import com.github.mafia.vyasma.polemicaachivementservice.model.game.PolemicaGamePlace
import java.time.LocalDateTime

interface ResearchService {
    fun getGamesWhereFourRedVotesByPerson(): ResearchVotedByFourRedVotesAnswer
    fun getMajorPairs(ids: List<Long>): String
    fun getBlackMoveTeamWinStat(): String
    fun getBlackMoveRefereeStat(): String
    fun getTwoTwoTwoTwoDivInNinth(): Map<Int, Int>
}

data class ResearchMajorPairsAnswer(
    val value: String
)

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
