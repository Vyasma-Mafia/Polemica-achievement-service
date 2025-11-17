package com.github.mafia.vyasma.polemicaachivementservice.statistics

import com.github.mafia.vyasma.polemica.library.model.game.PolemicaGame
import com.github.mafia.vyasma.polemica.library.model.game.PolemicaGameResult
import com.github.mafia.vyasma.polemica.library.model.game.PolemicaPlayer
import com.github.mafia.vyasma.polemica.library.model.game.PolemicaUser
import com.github.mafia.vyasma.polemica.library.model.game.Position
import com.github.mafia.vyasma.polemica.library.model.game.Role
import com.github.mafia.vyasma.polemicaachivementservice.configurations.PairStatisticsConfig
import com.github.mafia.vyasma.polemicaachivementservice.model.PlayerPoints
import com.github.mafia.vyasma.polemicaachivementservice.model.PolemicaGamePlayersPoints
import com.github.mafia.vyasma.polemicaachivementservice.model.jpa.Game
import com.github.mafia.vyasma.polemicaachivementservice.model.jpa.PolemicaGamePlace
import com.github.mafia.vyasma.polemicaachivementservice.model.jpa.User
import com.github.mafia.vyasma.polemicaachivementservice.repositories.GameRepository
import com.github.mafia.vyasma.polemicaachivementservice.repositories.UserRepository
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.data.repository.findByIdOrNull
import java.time.LocalDateTime

class PairStatisticsServiceTest {

    private lateinit var gameRepository: GameRepository
    private lateinit var userRepository: UserRepository
    private lateinit var service: PairStatisticsService

    @BeforeEach
    fun setUp() {
        gameRepository = mockk()
        userRepository = mockk()
        val config = mockk<PairStatisticsConfig>()
        service = PairStatisticsService(gameRepository, userRepository, config)
    }

    @Test
    fun `should calculate pair statistics for players in same team`() {
        // Given
        val player1Id = 1L
        val player2Id = 2L

        val user1 = User(userId = player1Id, username = "Player1", rating = 1200.0, gamesPlayed = 10, gamesWon = 5)
        val user2 = User(userId = player2Id, username = "Player2", rating = 1100.0, gamesPlayed = 8, gamesWon = 3)

        val games = listOf(
            // Both red, win
            createGameWithTwoPlayers(1L, player1Id, Role.PEACE, player2Id, Role.SHERIFF, PolemicaGameResult.RED_WIN),
            // Both black, win
            createGameWithTwoPlayers(2L, player1Id, Role.MAFIA, player2Id, Role.DON, PolemicaGameResult.BLACK_WIN),
            // Both red, loss
            createGameWithTwoPlayers(3L, player1Id, Role.PEACE, player2Id, Role.PEACE, PolemicaGameResult.BLACK_WIN)
        )

        every { userRepository.findByIdOrNull(player1Id) } returns user1
        every { userRepository.findByIdOrNull(player2Id) } returns user2
        every { gameRepository.findAllByUserJoinedFromPlayerRatingHistory(user1) } returns games
        every { gameRepository.findAllByUserJoinedFromPlayerRatingHistory(user2) } returns games

        // When
        val statistics = service.getPairStatistics(player1Id, player2Id)

        // Then
        assertNotNull(statistics)
        assertEquals(3, statistics?.totalGamesPlayed)

        // Same team statistics
        assertEquals(3, statistics?.sameTeamStatistics?.totalGames)
        assertEquals(2, statistics?.sameTeamStatistics?.totalWins)
        assertEquals(2.0 / 3.0, statistics?.sameTeamStatistics?.winRate)

        // Red team statistics
        assertEquals(2, statistics?.sameTeamStatistics?.asRed?.games)
        assertEquals(1, statistics?.sameTeamStatistics?.asRed?.wins)

        // Black team statistics
        assertEquals(1, statistics?.sameTeamStatistics?.asBlack?.games)
        assertEquals(1, statistics?.sameTeamStatistics?.asBlack?.wins)

        // No opposite team games
        assertEquals(0, statistics?.oppositeTeamStatistics?.totalGames)
    }

    @Test
    fun `should calculate pair statistics for players in opposite teams`() {
        // Given
        val player1Id = 1L
        val player2Id = 2L

        val user1 = User(userId = player1Id, username = "Player1", rating = 1200.0, gamesPlayed = 10, gamesWon = 5)
        val user2 = User(userId = player2Id, username = "Player2", rating = 1100.0, gamesPlayed = 8, gamesWon = 3)

        val games = listOf(
            // Player1 red, Player2 black, red wins
            createGameWithTwoPlayers(1L, player1Id, Role.PEACE, player2Id, Role.MAFIA, PolemicaGameResult.RED_WIN),
            // Player1 black, Player2 red, black wins
            createGameWithTwoPlayers(2L, player1Id, Role.DON, player2Id, Role.SHERIFF, PolemicaGameResult.BLACK_WIN)
        )

        every { userRepository.findByIdOrNull(player1Id) } returns user1
        every { userRepository.findByIdOrNull(player2Id) } returns user2
        every { gameRepository.findAllByUserJoinedFromPlayerRatingHistory(user1) } returns games
        every { gameRepository.findAllByUserJoinedFromPlayerRatingHistory(user2) } returns games

        // When
        val statistics = service.getPairStatistics(player1Id, player2Id)

        // Then
        assertNotNull(statistics)
        assertEquals(2, statistics?.totalGamesPlayed)

        // Opposite team statistics
        assertEquals(2, statistics?.oppositeTeamStatistics?.totalGames)
        assertEquals(2, statistics?.oppositeTeamStatistics?.firstPlayerWins)
        assertEquals(0, statistics?.oppositeTeamStatistics?.secondPlayerWins)
        assertEquals(1.0, statistics?.oppositeTeamStatistics?.firstPlayerWinRate)
        assertEquals(0.0, statistics?.oppositeTeamStatistics?.secondPlayerWinRate)
    }

    @Test
    fun `should return empty statistics for players without common games`() {
        // Given
        val player1Id = 1L
        val player2Id = 2L

        val user1 = User(userId = player1Id, username = "Player1", rating = 1200.0, gamesPlayed = 10, gamesWon = 5)
        val user2 = User(userId = player2Id, username = "Player2", rating = 1100.0, gamesPlayed = 8, gamesWon = 3)

        every { userRepository.findByIdOrNull(player1Id) } returns user1
        every { userRepository.findByIdOrNull(player2Id) } returns user2
        every { gameRepository.findAllByUserJoinedFromPlayerRatingHistory(user1) } returns emptyList()
        every { gameRepository.findAllByUserJoinedFromPlayerRatingHistory(user2) } returns emptyList()

        // When
        val statistics = service.getPairStatistics(player1Id, player2Id)

        // Then
        assertNotNull(statistics)
        assertEquals(0, statistics?.totalGamesPlayed)
        assertEquals(0, statistics?.sameTeamStatistics?.totalGames)
        assertEquals(0, statistics?.oppositeTeamStatistics?.totalGames)
    }

    @Test
    fun `should return common games with pagination`() {
        // Given
        val player1Id = 1L
        val player2Id = 2L

        val user1 = User(userId = player1Id, username = "Player1", rating = 1200.0, gamesPlayed = 10, gamesWon = 5)
        val user2 = User(userId = player2Id, username = "Player2", rating = 1100.0, gamesPlayed = 8, gamesWon = 3)

        val games = (1L..5L).map { gameId ->
            createGameWithTwoPlayers(
                gameId,
                player1Id,
                Role.PEACE,
                player2Id,
                Role.MAFIA,
                PolemicaGameResult.RED_WIN
            )
        }

        every { userRepository.findByIdOrNull(player1Id) } returns user1
        every { userRepository.findByIdOrNull(player2Id) } returns user2
        every { gameRepository.findAllByUserJoinedFromPlayerRatingHistory(user1) } returns games
        every { gameRepository.findAllByUserJoinedFromPlayerRatingHistory(user2) } returns games

        // When
        val response = service.getCommonGames(player1Id, player2Id, page = 0, size = 2)

        // Then
        assertNotNull(response)
        assertEquals(5, response?.totalCount)
        assertEquals(2, response?.games?.size)
        assertEquals(0, response?.page)
        assertEquals(2, response?.pageSize)
    }

    private fun createGameWithTwoPlayers(
        gameId: Long,
        player1Id: Long,
        player1Role: Role,
        player2Id: Long,
        player2Role: Role,
        result: PolemicaGameResult
    ): Game {
        val players = listOf(
            PolemicaPlayer(
                position = Position.ONE,
                username = "Player1",
                role = player1Role,
                techs = emptyList(),
                fouls = emptyList(),
                guess = null,
                player = PolemicaUser(player1Id, "Player1"),
                disqual = null,
                award = null
            ),
            PolemicaPlayer(
                position = Position.TWO,
                username = "Player2",
                role = player2Role,
                techs = emptyList(),
                fouls = emptyList(),
                guess = null,
                player = PolemicaUser(player2Id, "Player2"),
                disqual = null,
                award = null
            )
        )

        val polemicaGame = PolemicaGame(
            id = gameId,
            master = 1L,
            referee = PolemicaUser(1L, "Referee"),
            scoringVersion = "3.0",
            scoringType = 1,
            version = 1,
            zeroVoting = null,
            tags = null,
            players = players,
            checks = null,
            shots = null,
            stage = null,
            votes = emptyList(),
            comKiller = null,
            bonuses = null,
            started = LocalDateTime.now().minusDays(gameId),
            stop = null,
            isLive = false,
            result = result,
            num = null,
            table = null,
            phase = null,
            factor = null
        )

        val gamePoints = PolemicaGamePlayersPoints(
            success = true,
            players = listOf(
                PlayerPoints(position = 1, points = 0.5),
                PlayerPoints(position = 2, points = 0.3)
            )
        )

        return Game(
            gameId = gameId,
            data = polemicaGame,
            points = gamePoints,
            gamePlace = PolemicaGamePlace(),
            started = LocalDateTime.now().minusDays(gameId)
        )
    }
}
