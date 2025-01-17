package com.github.mafia.vyasma.polemicaachivementservice.research

import com.github.mafia.vyasma.polemica.library.model.game.PolemicaUser
import com.github.mafia.vyasma.polemicaachivementservice.model.jpa.PolemicaGamePlace
import java.time.LocalDateTime

interface ResearchService {
    fun getGamesWhereFourRedVotesByPerson(): ResearchVotedByFourRedVotesAnswer
    fun getMajorPairs(ids: List<Long>): String
    fun getBlackMoveTeamWinStat(): String
    fun getBlackMoveRefereeStat(): String
    fun getTwoTwoTwoTwoDivInNinth(): Map<Int, Int>
    fun getPairStat(firstId: Long, secondId: Long): ResearchPairStat
    fun getCompetitionsForUserCsv(userId: Long): String
    fun blank()
}

data class ResearchPairStat(
    val firstUser: PolemicaUser?,
    val secondUser: PolemicaUser?,
    val firstRedSecondRedWin: Long,
    val firstRedSecondRedTotal: Long,
    val firstRedSecondBlackWin: Long,
    val firstRedSecondBlackTotal: Long,
    val firstBlackSecondRedWin: Long,
    val firstBlackSecondRedTotal: Long,
    val firstBlackSecondBlackWin: Long,
    val firstBlackSecondBlackTotal: Long
)

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
