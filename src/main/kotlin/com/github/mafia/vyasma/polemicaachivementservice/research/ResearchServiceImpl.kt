package com.github.mafia.vyasma.polemicaachivementservice.research

import com.github.mafia.vyasma.polemicaachivementservice.model.game.Role
import com.github.mafia.vyasma.polemicaachivementservice.repositories.GameRepository
import com.github.mafia.vyasma.polemicaachivementservice.utils.getFinalVotes
import com.github.mafia.vyasma.polemicaachivementservice.utils.getPositionRole
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
                                    votes.map { data.getPositionRole(it.position) }
                                        .filter { it == Role.PEACE }.size == 4
                                }
                                    .toList()
                            if (votedByFourRed.isNotEmpty()) {
                                val vote = votedByFourRed.first()
                                if (data.getPositionRole(vote.first) == Role.SHERIFF) {
                                    toRed++
                                    games.add(
                                        ResearchVotedByFourRedVotesGame(
                                            gameId = game.gameId,
                                            gamePlace = game.gamePlace,
                                            gameStarted = game.started
                                        )
                                    )
                                } else if (data.getPositionRole(vote.first) == Role.DON) {
                                    toBlack++
                                }
                                // 33
                            }
                        }
                }
            }

        return ResearchVotedByFourRedVotesAnswer(toRed, toBlack, games.sortedByDescending { it.gameStarted })
    }
}
