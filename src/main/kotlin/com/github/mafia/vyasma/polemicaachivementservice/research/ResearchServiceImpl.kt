package com.github.mafia.vyasma.polemicaachivementservice.research

import com.github.mafia.vyasma.polemica.library.client.PolemicaClient
import com.github.mafia.vyasma.polemica.library.model.game.PolemicaGame
import com.github.mafia.vyasma.polemica.library.model.game.PolemicaPlayer
import com.github.mafia.vyasma.polemica.library.model.game.PolemicaUser
import com.github.mafia.vyasma.polemica.library.model.game.Position
import com.github.mafia.vyasma.polemica.library.model.game.Role
import com.github.mafia.vyasma.polemica.library.utils.MetricsUtils
import com.github.mafia.vyasma.polemica.library.utils.getFinalVotes
import com.github.mafia.vyasma.polemica.library.utils.getRole
import com.github.mafia.vyasma.polemica.library.utils.isBlack
import com.github.mafia.vyasma.polemica.library.utils.isBlackWin
import com.github.mafia.vyasma.polemica.library.utils.isRed
import com.github.mafia.vyasma.polemica.library.utils.isRedWin
import com.github.mafia.vyasma.polemicaachivementservice.model.jpa.Game
import com.github.mafia.vyasma.polemicaachivementservice.repositories.GameRepository
import com.github.mafia.vyasma.polemicaachivementservice.repositories.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import java.util.function.Predicate

@Service
class ResearchServiceImpl(
    val gameRepository: GameRepository,
    val userRepository: UserRepository,
    val polemicaClient: PolemicaClient
) : ResearchService {
    val logger = LoggerFactory.getLogger(ResearchServiceImpl::class.java)

    override fun blank() {
        return
    }

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
                    ninesVotes
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

    override fun getPairStat(firstId: Long, secondId: Long): ResearchPairStat {
        val counter = ResearchPairStatCounter()
        gameRepository.findAll().forEach { game ->
            val data = game.data
            if (data.players.any { it.player == firstId } && data.players.any { it.player == secondId }) {
                val firstRole = data.players.first { it.player == firstId }.role
                val secondRole = data.players.first { it.player == secondId }.role
                val isRedWin = data.isRedWin()
                counter.firstRedSecondRedWin += if (firstRole.isRed() && secondRole.isRed() && isRedWin) 1 else 0
                counter.firstRedSecondRedTotal += if (firstRole.isRed() && secondRole.isRed()) 1 else 0
                counter.firstRedSecondBlackWin += if (firstRole.isRed() && secondRole.isBlack() && isRedWin) 1 else 0
                counter.firstRedSecondBlackTotal += if (firstRole.isRed() && secondRole.isBlack()) 1 else 0
                counter.firstBlackSecondRedWin += if (firstRole.isBlack() && secondRole.isRed() && isRedWin) 1 else 0
                counter.firstBlackSecondRedTotal += if (firstRole.isBlack() && secondRole.isRed()) 1 else 0
                counter.firstBlackSecondBlackWin += if (firstRole.isBlack() && secondRole.isBlack() && isRedWin) 1 else 0
                counter.firstBlackSecondBlackTotal += if (firstRole.isBlack() && secondRole.isBlack()) 1 else 0
            }
        }

        return ResearchPairStat(
            firstUser = getPolemicaUser(firstId),
            secondUser = getPolemicaUser(secondId),
            firstRedSecondRedWin = counter.firstRedSecondRedWin,
            firstRedSecondRedTotal = counter.firstRedSecondRedTotal,
            firstRedSecondBlackWin = counter.firstRedSecondBlackWin,
            firstRedSecondBlackTotal = counter.firstRedSecondBlackTotal,
            firstBlackSecondRedWin = counter.firstBlackSecondRedWin,
            firstBlackSecondRedTotal = counter.firstBlackSecondRedTotal,
            firstBlackSecondBlackWin = counter.firstBlackSecondBlackWin,
            firstBlackSecondBlackTotal = counter.firstBlackSecondBlackTotal
        )
    }

    fun getCompetitionsForUser(userId: Long): List<Pair<PolemicaClient.PolemicaCompetition, List<PolemicaUser>>> {
        return polemicaClient.getCompetitions().filter { it.city == "Санкт-Петербург" }
            // .filter { polemicaClient.getCompetitionMembers(it.id).any { it.player.id == userId } }
            .map { Pair(it, MetricsUtils.getRating(polemicaClient.getCompetitionResultMetrics(it.id, it.scoringType))) }
            .filter { it.second.any { it.id == userId } }
    }

    override fun getCompetitionsForUserCsv(userId: Long): String {
        return getCompetitionsForUser(userId).map {
            "${it.first.name},${it.second.size - it.second.indexOfFirst { it.id == userId }}/${it.second.size}"
        }.joinToString("\n") { it }
    }

    fun getPolemicaUser(userId: Long): PolemicaUser? {
        return userRepository.findByIdOrNull(userId)?.let { PolemicaUser(it.userId, it.username) }
    }

    fun getRedWinRateForFilter(p: Predicate<Game>): TeamWinRate {
        var redWin = 0L
        var blackWin = 0L
        gameRepository.findAll().forEach { game ->
            if (p.test(game)) {
                logger.info("Game ${game.gameId}, ${game.gamePlace} is in filter")
                if (game.data.isRedWin()) {
                    redWin += 1
                } else {
                    blackWin += 1
                }
            }
        }


        return TeamWinRate(redWin, blackWin)
    }

    fun getGamesForPerson(personId: Long, p: (PolemicaPlayer) -> Boolean = { true }): List<Game> {
        return gameRepository.findAll()
            .filter { it.data.players.any { it.player == personId } }
            .filter { p(it.data.players.first { it.player == personId }) }
            .map { it }
    }

    fun countGamesByFilter(p: (PolemicaGame) -> Boolean = { true }): Int {
        return gameRepository.findAll().map { it.data }.count(p)
    }

    fun getStatByPosition(p: (PolemicaPlayer) -> Boolean = { true }): List<Pair<Position, SimpleStat>> {
        val positionSimpleStats = mutableMapOf<Position, SimpleStat>()
        Position.entries.forEach { position ->
            positionSimpleStats[position] = SimpleStat()
        }
        gameRepository.findAll().forEach { game ->
            game.data.players.filter(p).forEach { player ->
                positionSimpleStats[player.position]?.let { stat ->
                    stat.red += if (player.role.isRed()) 1 else 0
                    stat.black += if (player.role.isBlack()) 1 else 0
                    stat.redWin += if (game.data.isRedWin() && player.role.isRed()) 1 else 0
                    stat.blackWin += if (game.data.isBlackWin() && player.role.isBlack()) 1 else 0
                }
            }
        }
        return positionSimpleStats.entries.map { it.key to it.value }
    }

    data class SimpleStat(
        var red: Long = 0,
        var black: Long = 0,
        var redWin: Long = 0,
        var blackWin: Long = 0
    ) {
        fun winRate(): Double {
            return (redWin.toDouble() + blackWin) / (red + black)
        }

        fun redWinRate(): Double {
            return redWin.toDouble() / red
        }

        fun blackWinRate(): Double {
            return blackWin.toDouble() / black
        }
    }

    data class ResearchPairStatCounter(
        var firstRedSecondRedWin: Long = 0,
        var firstRedSecondRedTotal: Long = 0,
        var firstRedSecondBlackWin: Long = 0,
        var firstRedSecondBlackTotal: Long = 0,
        var firstBlackSecondRedWin: Long = 0,
        var firstBlackSecondRedTotal: Long = 0,
        var firstBlackSecondBlackWin: Long = 0,
        var firstBlackSecondBlackTotal: Long = 0
    )

    data class TeamWinRate(val redWin: Long, val blackWin: Long)
}
