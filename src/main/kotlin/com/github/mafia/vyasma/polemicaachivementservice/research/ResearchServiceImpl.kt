package com.github.mafia.vyasma.polemicaachivementservice.research

import com.github.mafia.vyasma.polemica.library.client.PolemicaClient
import com.github.mafia.vyasma.polemica.library.model.game.PolemicaGame
import com.github.mafia.vyasma.polemica.library.model.game.PolemicaPlayer
import com.github.mafia.vyasma.polemica.library.model.game.PolemicaUser
import com.github.mafia.vyasma.polemica.library.model.game.Position
import com.github.mafia.vyasma.polemica.library.model.game.Role
import com.github.mafia.vyasma.polemica.library.utils.MetricsUtils
import com.github.mafia.vyasma.polemica.library.utils.getFinalVotes
import com.github.mafia.vyasma.polemica.library.utils.getFirstKilled
import com.github.mafia.vyasma.polemica.library.utils.getRole
import com.github.mafia.vyasma.polemica.library.utils.isBlack
import com.github.mafia.vyasma.polemica.library.utils.isBlackWin
import com.github.mafia.vyasma.polemica.library.utils.isRed
import com.github.mafia.vyasma.polemica.library.utils.isRedWin
import com.github.mafia.vyasma.polemicaachivementservice.model.jpa.Game
import com.github.mafia.vyasma.polemicaachivementservice.rating.GamePointsService
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
    val polemicaClient: PolemicaClient,
    val pointsService: GamePointsService
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
                val ninesVotes = data.getFinalVotes(null)
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
            for (player1 in data.players!!.filter { ids.contains(it.player?.id) }) {
                for (player2 in data.players!!.filter { ids.contains(it.player?.id) }) {
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
            val filteredResults = data.votes!!.groupBy { it.day to it.num }
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
            if (data.players!!.any { it.player?.id == firstId } && data.players!!.any { it.player?.id == secondId }) {
                val firstRole = data.players!!.first { it.player?.id == firstId }.role
                val secondRole = data.players!!.first { it.player?.id == secondId }.role
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
            .filter { it.data.players!!.any { it.player?.id == personId } }
            .filter { p(it.data.players!!.first { it.player?.id == personId }) }
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
            game.data.players!!.filter(p).forEach { player ->
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

    fun guessStat(filter: (Game) -> Boolean = { true }): MutableMap<Int, Pair<Double, Int>> {
        val stat = mutableMapOf(
            Pair(0, Pair(0.0, 0)),
            Pair(1, Pair(0.0, 0)),
            Pair(2, Pair(0.0, 0)),
            Pair(3, Pair(0.0, 0))
        )
        gameRepository.findAll()
            .filter { it.data.scoringVersion == "3.0" }
            // .filter { it.data.tags?.contains("PremierLeague") ?: false || it.gamePlace.competitionId == 3232L }
            // .filter { it.data.tags?.contains("ChampionshipLeague") ?: false || it.gamePlace.competitionId == 3249L }
            // .filter { it.data.tags?.contains("LeagueOne") ?: false || it.gamePlace.competitionId == 3289L }
            .filter { filter.invoke(it) }
            .forEach {
                val game = it.data
                val fk = game.players?.find { it.position == game.getFirstKilled() }
                if (fk == null) {
                    return@forEach
                }

                val civs = fk.guess?.civs?.map {
                    if (game.getRole(it).isRed()) {
                        0.2
                    } else {
                        -0.1
                    }
                } ?: arrayListOf()
                val mafs = fk.guess?.mafs?.map {
                    if (game.getRole(it).isBlack()) {
                        0.3
                    } else {
                        -0.1
                    }
                }?.toMutableList() ?: arrayListOf()
                if (civs.size + mafs.size != 3) {
                    return@forEach
                }
                if (mafs.filter { it == 0.3 }.size == 3) {
                    mafs.add(0.1)
                }
                val old = stat[civs.size]!!
                stat[civs.size] = Pair(old.first + civs.sum() + mafs.sum(), old.second + 1)
            }
        return stat
    }

    fun leagueOneLeaders(): List<MutableMap.MutableEntry<Long, MutableList<Double>>> {
        val playerScores = mutableMapOf<Long, MutableList<Double>>()
        gameRepository.findAll()
            .filter { it.gamePlace.competitionId == 3289L }
            .filter { it.data.num != null }
            .groupBy { (it.data.num!! - 1) / 4 }
            .forEach { games ->
                println(games.value.map { it.data.num })
                val playerScoresSeria = mutableMapOf<Long, Double>()
                games.value.forEach { game ->
                    pointsService.fetchPlayerStats(game.gameId).forEach { player ->
                        game.data.players?.find { it.position.value == player.position }?.player?.id?.let { playerId ->
                            playerScoresSeria.merge(playerId, player.points, Double::plus)
                        }
                    }
                }
                playerScoresSeria.entries.forEach { playerScore ->
                    playerScores.getOrPut(playerScore.key) { arrayListOf() }.add(playerScore.value)
                }
            }

        return playerScores.entries.sortedByDescending { it.value.sortedDescending().take(5).sum() }
        // leagueOneLeaders().mapIndexed { i, it -> "${i + 1}. ${userRepository.findById(it.key).get().username}: ${"%.2f".format(it.value.sortedDescending().take(5).sum())} (${it.value.map { "%.2f".format(it) }})" }.joinToString("\n") { it }
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

    fun getPlayerStatsCsv(profileUrls: List<String>): String {
        val csvBuilder = StringBuilder()
        // CSV Header
        csvBuilder.appendLine(
            listOf(
                "Ссылка", "Username", "Рейтинг", "Количество игр (всего)",
                "Количество игр (актуальный скоринг)",
                "WinRate Мирный", "WinRate Мафия", "WinRate Дон", "WinRate Шериф",
                "Средний доп Мирный", "Средний доп Мафия", "Средний доп Дон", "Средний доп Шериф"
            ).joinToString(",")
        )

        val allGames = gameRepository.findAll().filter { it.data.scoringType == 1 } // Загружаем все игры один раз


        for (url in profileUrls) {
            val playerId = try {
                url.substringAfterLast('/').substringBefore('#').toLongOrNull()
            } catch (e: Exception) {
                logger.warn("Could not parse player ID from URL: $url", e)
                null
            }

            if (playerId == null) {
                logger.warn("Skipping invalid URL (no player ID): $url")
                // Можно добавить строку с N/A если нужно для полноты
                // csvBuilder.appendLine(List(13) { if (it == 0) url else "N/A" }.joinToString(","))
                continue
            }

            val userEntity = userRepository.findByIdOrNull(playerId) ?: continue
            val username = userEntity.username
            val rating = userEntity.rating ?: 0.0
            val totalGamesPlayedByUser = userEntity.gamesPlayed

            val roleStats = Role.entries.associateWith { RoleStatSummary() }.toMutableMap()
            var gamesWithActualScoring = 0

            // Фильтруем игры для текущего игрока
            val playerGames = allGames.filter { game ->
                game.data.players?.any { it.player?.id == playerId } ?: false
            }

            for (gameEntity in playerGames) {
                val gameData = gameEntity.data // This is PolemicaGame
                val playerInGame = gameData.players?.find { it.player?.id == playerId } ?: continue

                val playerRole = playerInGame.role
                val statSummary = roleStats[playerRole] ?: continue // Should always exist

                statSummary.gamesPlayed++

                val playerWon: Boolean = when {
                    playerRole.isRed() && gameData.isRedWin() -> true
                    playerRole.isBlack() && gameData.isBlackWin() -> true
                    else -> false
                }
                if (playerWon) {
                    statSummary.gamesWon++
                }

                if (gameData.scoringVersion == "3.0") {
                    gamesWithActualScoring++
                }

                try {
                    val points = gameEntity.points?.players?.find { it.position == playerInGame.position.value }?.points
                    if (points != null) {
                        val additionalPoints = points
                        statSummary.totalAdditionalPoints += additionalPoints
                        statSummary.gamesWithPointsCount++
                    }
                } catch (e: Exception) {
                    logger.error(
                        "Failed to fetch or process points for game ${gameEntity.gameId} for player $playerId",
                        e
                    )
                    // Не увеличиваем gamesWithPointsCount, если не смогли получить очки
                }
            }

            // Форматирование с двумя знаками после запятой для Double
            fun Double.format() = "%.2f".format(this)

            csvBuilder.appendLine(
                listOf(
                    url,
                    username,
                    rating.format(),
                    totalGamesPlayedByUser.toString(), // Используем данные из User entity для общего числа игр
                    gamesWithActualScoring.toString(),
                    roleStats[Role.PEACE]?.winRate?.format() ?: "0.00",
                    roleStats[Role.MAFIA]?.winRate?.format() ?: "0.00",
                    roleStats[Role.DON]?.winRate?.format() ?: "0.00",
                    roleStats[Role.SHERIFF]?.winRate?.format() ?: "0.00",
                    roleStats[Role.PEACE]?.averageAdditionalPoints?.format() ?: "0.00",
                    roleStats[Role.MAFIA]?.averageAdditionalPoints?.format() ?: "0.00",
                    roleStats[Role.DON]?.averageAdditionalPoints?.format() ?: "0.00",
                    roleStats[Role.SHERIFF]?.averageAdditionalPoints?.format() ?: "0.00"
                ).joinToString(",")
            )
        }
        return csvBuilder.toString()
    }

    data class RoleStatSummary(
        var gamesPlayed: Int = 0,
        var gamesWon: Int = 0,
        var totalAdditionalPoints: Double = 0.0,
        var gamesWithPointsCount: Int = 0 // To average additional points correctly
    ) {
        val winRate: Double
            get() = if (gamesPlayed > 0) gamesWon.toDouble() / gamesPlayed else 0.0

        val averageAdditionalPoints: Double
            get() = if (gamesWithPointsCount > 0) totalAdditionalPoints / gamesWithPointsCount else 0.0
    }
}
