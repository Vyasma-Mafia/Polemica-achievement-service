package com.github.mafia.vyasma.polemicaachivementservice.research

import com.github.mafia.vyasma.polemica.library.model.game.Role
import com.github.mafia.vyasma.polemica.library.utils.getFinalVotes
import com.github.mafia.vyasma.polemica.library.utils.getRole
import com.github.mafia.vyasma.polemica.library.utils.isBlack
import com.github.mafia.vyasma.polemica.library.utils.isBlackWin
import com.github.mafia.vyasma.polemicaachivementservice.model.jpa.Game
import com.github.mafia.vyasma.polemicaachivementservice.repositories.GameRepository
import org.springframework.stereotype.Service

@Service
class ResearchServiceImpl(
    val gameRepository: GameRepository
) : ResearchService {
    override fun getGamesWhereFourRedVotesByPerson(): ResearchVotedByFourRedVotesAnswer {
        var toRed = 0L
        var toBlack = 0L
        val games = arrayListOf<ResearchVotedByFourRedVotesGame>()
        gameRepository.findAll()
            .forEach { game ->
                val data = game.data
                val ninesVotes = data.getFinalVotes()
                    .groupBy { it.day }
                    .filter { it.value.size == 9 }
                    .filter { votes -> votes.value.all { it.convicted.size == 1 } }
                if (ninesVotes.isNotEmpty()) {
                    val groupedByConvicted = ninesVotes
                        .mapValues { votes -> votes.value.groupBy { it.convicted.first() } }
                        .toList()
                        .map { it.second }
                        .forEach { convictedToVotes ->
                            val votedByFour = convictedToVotes
                                .filterValues { votes ->
                                    votes.size == 4 && votes.all { it.expelled }
                                }
                            val votedByFourRed =
                                votedByFour.filterValues { votes ->
                                    votes.map { data.getRole(it.position) }
                                        .filter { it == Role.PEACE }.size == 4
                                }
                                    .toList()
                            if (votedByFourRed.isNotEmpty()) {
                                val vote = votedByFourRed.first()
                                if (data.getRole(vote.first) == Role.SHERIFF) {
                                    toRed++
                                    games.add(
                                        ResearchVotedByFourRedVotesGame(
                                            gameId = game.gameId,
                                            gamePlace = game.gamePlace,
                                            gameStarted = game.started
                                        )
                                    )
                                } else if (data.getRole(vote.first) == Role.DON) {
                                    toBlack++
                                }
                                // 33
                            }
                        }
                }
            }

        return ResearchVotedByFourRedVotesAnswer(toRed, toBlack, games.sortedByDescending { it.gameStarted })
    }

    override fun getMajorPairs(ids: List<Long>): String {
        val value: MutableMap<Pair<String, String>, Int> = hashMapOf()
        gameRepository.findAll().forEach { game ->
            val data = game.data
            val withTags =
                game.data.tags?.intersect(listOf("PremierLeague", "ChampionshipLeague"))?.isNotEmpty() ?: false
            val inCompetition = game.gamePlace.competitionId != null
            if (!(withTags || inCompetition)) return@forEach
            for (player1 in data.players.filter { ids.contains(it.player) }) {
                for (player2 in data.players.filter { ids.contains(it.player) }) {
                    val pair = normalizePair(Pair(player1.username, player2.username))
                    if (data.getRole(player1.position).isBlack() &&
                        data.getRole(player2.position).isBlack()
                    ) {
                        value.merge(pair, 1, Int::plus)
                    }
                }
            }
        }
        return value.map { "${it.key.first},${it.key.second},${it.value}" }
            .joinToString("\n") { it }
    }

    override fun getBlackMoveTeamWinStat(): String {
        var blackWin = 0
        var redWin = 0
        gameRepository.findAllWhereByGamePlace_ClubId(289).forEach { game ->
            val data = game.data
            if (data.isBlackWin()) {
                blackWin += 1
            } else {
                redWin += 1
            }
        }
        return "${blackWin},${redWin}"
    }

    override fun getBlackMoveRefereeStat(): String {
        val counter = hashMapOf<String, Int>()
        gameRepository.findAllWhereByGamePlace_ClubId(289).forEach { game ->
            val data = game.data
            counter.merge(data.referee.username, 1, Int::plus)
        }
        return counter.toList().sortedByDescending { it.second }.joinToString("\n") { "${it.first},${it.second}" }
    }

    fun normalizePair(pair: Pair<String, String>): Pair<String, String> {
        val sorted = pair.toList().sorted()
        return Pair(sorted.first(), sorted.last())
    }

    override fun getTwoTwoTwoTwoDivInNinth(): Map<Int, Int> {
        val counter: MutableMap<Int, Int> = hashMapOf()
        gameRepository.findAll().forEach { game: Game ->
            val data = game.data
            val filteredResults = data.votes.groupBy { it.day to it.num }
                .filter { (_, groupVotes) ->
                    groupVotes.size == 9 &&
                        groupVotes.groupingBy { it.candidate }.eachCount().values.sorted() == listOf(1, 2, 2, 2, 2)
                }
            filteredResults.values.forEach { groupVotes ->
                val blacksInDivision = groupVotes.groupingBy { it.candidate }
                    .eachCount()
                    .filter { it.value == 2 }
                    .keys
                    .map { data.getRole(it) }
                    .count { it.isBlack() }
                counter.merge(blacksInDivision, 1, Int::plus)
            }
        }
        return counter
    }
}
