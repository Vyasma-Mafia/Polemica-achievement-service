package com.github.mafia.vyasma.polemicaachivementservice.research

import com.github.mafia.vyasma.polemica.library.client.PolemicaClient
import com.github.mafia.vyasma.polemica.library.model.game.PolemicaGame
import com.github.mafia.vyasma.polemica.library.model.game.PolemicaPlayer
import com.github.mafia.vyasma.polemica.library.model.game.PolemicaUser
import com.github.mafia.vyasma.polemica.library.model.game.Position
import com.github.mafia.vyasma.polemica.library.model.game.Role
import com.github.mafia.vyasma.polemica.library.utils.KickReason
import com.github.mafia.vyasma.polemica.library.utils.MetricsUtils
import com.github.mafia.vyasma.polemica.library.utils.getFinalVotes
import com.github.mafia.vyasma.polemica.library.utils.getFirstKilled
import com.github.mafia.vyasma.polemica.library.utils.getKickedFromTable
import com.github.mafia.vyasma.polemica.library.utils.getRole
import com.github.mafia.vyasma.polemica.library.utils.isBlack
import com.github.mafia.vyasma.polemica.library.utils.isBlackWin
import com.github.mafia.vyasma.polemica.library.utils.isRed
import com.github.mafia.vyasma.polemica.library.utils.isRedWin
import com.github.mafia.vyasma.polemicaachivementservice.achievements.services.AchievementService
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
    val pointsService: GamePointsService,
    val achievementService: AchievementService
) : ResearchService {
    val logger = LoggerFactory.getLogger(ResearchServiceImpl::class.java)

    override fun blank() {
        return
    }

    override fun getFirstKilledStats(playerId: Long): FirstKilledStats {
        var firstKilledAsRed = 0L
        var firstKilledAsRedAndRedWin = 0L
        var firstKilledAsSheriff = 0L
        var firstKilledAsSheriffAndRedWin = 0L

        gameRepository.findAll().forEach { game ->
            val data = game.data
            val firstKilledPosition = data.getFirstKilled()

            // Find the player in this game
            val playerInGame = data.players?.find { it.player?.id == playerId }
            if (playerInGame == null) {
                return@forEach
            }

            // Check if this player was first killed
            if (playerInGame.position == firstKilledPosition) {
                val playerRole = playerInGame.role
                val isRedWin = data.isRedWin()

                // Check if player is red (PEACE or SHERIFF)
                if (playerRole.isRed()) {
                    firstKilledAsRed++
                    if (isRedWin) {
                        firstKilledAsRedAndRedWin++
                    }
                }

                // Check if player is sheriff
                if (playerRole == Role.SHERIFF) {
                    firstKilledAsSheriff++
                    if (isRedWin) {
                        firstKilledAsSheriffAndRedWin++
                    }
                }
            }
        }

        return FirstKilledStats(
            firstKilledAsRed = firstKilledAsRed,
            firstKilledAsRedAndRedWin = firstKilledAsRedAndRedWin,
            firstKilledAsSheriff = firstKilledAsSheriff,
            firstKilledAsSheriffAndRedWin = firstKilledAsSheriffAndRedWin
        )
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

    fun getPlayerStatsCsv(): String {
        val csvBuilder = StringBuilder()
        // CSV Header
        csvBuilder.appendLine(
            listOf(
                "Id", "Username", "Рейтинг", "Количество игр",
                "Игр Мирный", "Игр Мафия", "Игр Дон", "Игр Шериф",
                "WinRate Мирный", "WinRate Мафия", "WinRate Дон", "WinRate Шериф",
                "Средний доп Мирный", "Средний доп Мафия", "Средний доп Дон", "Средний доп Шериф",
                "ПУ"
            ).joinToString(",")
        )

        val allGames = gameRepository.findAll()


        for (playerId in userRepository.findAll().map { it.userId }) {
            val userEntity = userRepository.findByIdOrNull(playerId) ?: continue
            val username = userEntity.username
            val rating = userEntity.rating ?: 0.0

            val roleStats = Role.entries.associateWith { RoleStatSummary() }.toMutableMap()
            var gamesWithActualScoring = 0
            var firstKilledCounter = 0

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

                if (gameData.getFirstKilled() == playerInGame.position) {
                    firstKilledCounter++
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
                    playerId,
                    username,
                    rating.format(),
                    gamesWithActualScoring.toString(),
                    roleStats[Role.PEACE]?.gamesPlayed ?: "0",
                    roleStats[Role.MAFIA]?.gamesPlayed ?: "0",
                    roleStats[Role.DON]?.gamesPlayed ?: "0",
                    roleStats[Role.SHERIFF]?.gamesPlayed ?: "0",
                    roleStats[Role.PEACE]?.winRate?.format() ?: "0.00",
                    roleStats[Role.MAFIA]?.winRate?.format() ?: "0.00",
                    roleStats[Role.DON]?.winRate?.format() ?: "0.00",
                    roleStats[Role.SHERIFF]?.winRate?.format() ?: "0.00",
                    roleStats[Role.PEACE]?.averageAdditionalPoints?.format() ?: "0.00",
                    roleStats[Role.MAFIA]?.averageAdditionalPoints?.format() ?: "0.00",
                    roleStats[Role.DON]?.averageAdditionalPoints?.format() ?: "0.00",
                    roleStats[Role.SHERIFF]?.averageAdditionalPoints?.format() ?: "0.00",
                    firstKilledCounter
                ).joinToString(",")
            )
        }
        return csvBuilder.toString()
    }

    fun getAchievement(): String {
        return achievementService.getAchievements(emptyList(), userRepository.findAll().map { it.userId }, null)
            .achievementsGains
            .filter { it.achievementId == "sniper" }
            .joinToString(separator = "\n") { "${it.user.id},${it.achievementCounter ?: 0}" }
    }

    fun getDayVotingStatistics(): String {
        // Statistics tracking
        var day1RedSheriffRedWin = 0L
        var day1RedSheriffBlackWin = 0L
        var day1BlackDonRedWin = 0L
        var day1BlackDonBlackWin = 0L

        // Day 2 statistics: key is (day2Role, day1Team)
        val day2Stats = mutableMapOf<Pair<String, String>, Pair<Long, Long>>()

        // Convert to list to ensure all results are loaded (avoid lazy loading issues)
        val allGames = gameRepository.findAll().toList()
        logger.info("Total games in repository: ${allGames.size}")

        var processedCount = 0L
        var day1VotedOutNot1 = 0L
        var day2VotedOutNot1 = 0L
        allGames.forEach { game ->
            processedCount++
            val data = game.data
            val kickedPlayers = data.getKickedFromTable()

            // Filter by VOTING reason and day 1 or day 2
            val day1VotedOut = kickedPlayers.filter {
                it.gamePhase.num == 2 && it.reason == KickReason.VOTING
            }
            val day2VotedOut = kickedPlayers.filter {
                it.gamePhase.num == 3 && it.reason == KickReason.VOTING
            }

            day1VotedOutNot1 += if (day1VotedOut.size != 1) {
                1
            } else {
                0
            }
            day2VotedOutNot1 += if (day2VotedOut.size != 1) {
                1
            } else {
                0
            }

            // Process Day 1
            // For day 1, use only votings where exactly one candidate was voted out
            if (day1VotedOut.size == 1) {
                val day1KickedPlayer = day1VotedOut.first()
                val day1Role = data.getRole(day1KickedPlayer.position)

                // Check if red/sheriff or black/don
                val isRedSheriff = day1Role == Role.PEACE || day1Role == Role.SHERIFF
                val isBlackDon = day1Role == Role.MAFIA || day1Role == Role.DON

                if (isRedSheriff) {
                    if (data.isRedWin()) {
                        day1RedSheriffRedWin++
                    } else {
                        day1RedSheriffBlackWin++
                    }
                } else if (isBlackDon) {
                    if (data.isRedWin()) {
                        day1BlackDonRedWin++
                    } else {
                        day1BlackDonBlackWin++
                    }
                }
            }

            // Process Day 2
            // For day 2, need exactly one player voted out on day 2, and day 1 must have exactly one
            if (day2VotedOut.size == 1 && day1VotedOut.size == 1) {
                // First, determine day 1 vote result
                val day1KickedPlayer = day1VotedOut.first()
                val day1Role = data.getRole(day1KickedPlayer.position)
                val day1Team = if (day1Role == Role.PEACE || day1Role == Role.SHERIFF) {
                    "Red"
                } else if (day1Role == Role.MAFIA || day1Role == Role.DON) {
                    "Black"
                } else {
                    null
                }

                // Find the expelled player on day 2
                val day2KickedPlayer = day2VotedOut.first()
                val day2Role = data.getRole(day2KickedPlayer.position)

                // Check if red/sheriff or black/don
                val isRedSheriff = day2Role == Role.PEACE || day2Role == Role.SHERIFF
                val isBlackDon = day2Role == Role.MAFIA || day2Role == Role.DON

                if ((isRedSheriff || isBlackDon) && day1Team != null) {
                    val category = if (isRedSheriff) "RedSheriff" else "BlackDon"
                    val key = Pair(category, day1Team)

                    val currentStats = day2Stats.getOrDefault(key, Pair(0L, 0L))
                    if (data.isRedWin()) {
                        day2Stats[key] = Pair(currentStats.first + 1, currentStats.second)
                    } else {
                        day2Stats[key] = Pair(currentStats.first, currentStats.second + 1)
                    }
                }
            }
        }

        logger.info("Processed $processedCount games. Day 1 Red/Sheriff: ${day1RedSheriffRedWin + day1RedSheriffBlackWin}, Day 1 Black/Don: ${day1BlackDonRedWin + day1BlackDonBlackWin}")
        logger.info("day1VotedOutNot1: $day1VotedOutNot1, day2VotedOutNot1: $day2VotedOutNot1")
        // Format as CSV
        val csvBuilder = StringBuilder()
        csvBuilder.appendLine("Category,Day1VoteResult,RedWin,BlackWin,Total")

        // Day 1 Red/Sheriff
        val day1RedSheriffTotal = day1RedSheriffRedWin + day1RedSheriffBlackWin
        csvBuilder.appendLine("Day1_RedSheriff,,$day1RedSheriffRedWin,$day1RedSheriffBlackWin,$day1RedSheriffTotal")

        // Day 1 Black/Don
        val day1BlackDonTotal = day1BlackDonRedWin + day1BlackDonBlackWin
        csvBuilder.appendLine("Day1_BlackDon,,$day1BlackDonRedWin,$day1BlackDonBlackWin,$day1BlackDonTotal")

        // Day 2 statistics
        val day2RedSheriffRed = day2Stats.getOrDefault(Pair("RedSheriff", "Red"), Pair(0L, 0L))
        val day2RedSheriffRedTotal = day2RedSheriffRed.first + day2RedSheriffRed.second
        csvBuilder.appendLine("Day2_RedSheriff,Red,${day2RedSheriffRed.first},${day2RedSheriffRed.second},$day2RedSheriffRedTotal")

        val day2RedSheriffBlack = day2Stats.getOrDefault(Pair("RedSheriff", "Black"), Pair(0L, 0L))
        val day2RedSheriffBlackTotal = day2RedSheriffBlack.first + day2RedSheriffBlack.second
        csvBuilder.appendLine("Day2_RedSheriff,Black,${day2RedSheriffBlack.first},${day2RedSheriffBlack.second},$day2RedSheriffBlackTotal")

        val day2BlackDonRed = day2Stats.getOrDefault(Pair("BlackDon", "Red"), Pair(0L, 0L))
        val day2BlackDonRedTotal = day2BlackDonRed.first + day2BlackDonRed.second
        csvBuilder.appendLine("Day2_BlackDon,Red,${day2BlackDonRed.first},${day2BlackDonRed.second},$day2BlackDonRedTotal")

        val day2BlackDonBlack = day2Stats.getOrDefault(Pair("BlackDon", "Black"), Pair(0L, 0L))
        val day2BlackDonBlackTotal = day2BlackDonBlack.first + day2BlackDonBlack.second
        csvBuilder.appendLine("Day2_BlackDon,Black,${day2BlackDonBlack.first},${day2BlackDonBlack.second},$day2BlackDonBlackTotal")

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
