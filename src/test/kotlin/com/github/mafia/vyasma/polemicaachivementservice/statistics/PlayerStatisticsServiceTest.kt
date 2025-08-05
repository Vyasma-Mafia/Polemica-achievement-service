package com.github.mafia.vyasma.polemicaachivementservice.statistics

import com.github.mafia.vyasma.polemica.library.model.game.*
import com.github.mafia.vyasma.polemicaachivementservice.model.PlayerPoints
import com.github.mafia.vyasma.polemicaachivementservice.model.PolemicaGamePlayersPoints
import com.github.mafia.vyasma.polemicaachivementservice.model.jpa.Game
import com.github.mafia.vyasma.polemicaachivementservice.model.jpa.PolemicaGamePlace
import com.github.mafia.vyasma.polemicaachivementservice.model.jpa.User
import com.github.mafia.vyasma.polemicaachivementservice.repositories.GameRepository
import com.github.mafia.vyasma.polemicaachivementservice.repositories.UserRepository
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.data.repository.findByIdOrNull
import java.time.LocalDateTime

class PlayerStatisticsServiceTest {

    private lateinit var gameRepository: GameRepository
    private lateinit var userRepository: UserRepository
    private lateinit var service: PlayerStatisticsService

    @BeforeEach
    fun setUp() {
        gameRepository = mockk()
        userRepository = mockk()
        service = PlayerStatisticsService(gameRepository, userRepository)
    }

    @Test
    fun `should return statistics for player with games`() {
        // Given
        val playerId = 1L
        val user = User(
            userId = playerId,
            username = "TestPlayer",
            rating = 1200.0,
            gamesPlayed = 10,
            gamesWon = 5
        )

        val games = listOf(
            createGame(1L, playerId, Role.PEACE, PolemicaGameResult.RED_WIN, 0.5),
            createGame(2L, playerId, Role.MAFIA, PolemicaGameResult.BLACK_WIN, 0.8),
            createGame(3L, playerId, Role.DON, PolemicaGameResult.RED_WIN, -0.3),
            createGame(4L, playerId, Role.SHERIFF, PolemicaGameResult.RED_WIN, 1.2)
        )

        every { userRepository.findByIdOrNull(playerId) } returns user
        every { gameRepository.findAll() } returns games

        // When
        val statistics = service.getPlayerStatistics(playerId)

        // Then
        assertNotNull(statistics)
        assertEquals(playerId, statistics?.playerId)
        assertEquals("TestPlayer", statistics?.username)
        assertEquals(4, statistics?.totalGames)
        assertEquals(3, statistics?.totalWins) // 3 wins (PEACE, MAFIA, SHERIFF)
        assertEquals(0.75, statistics?.winRate)

        // Check role statistics
        val peaceStats = statistics?.roleStatistics?.get(Role.PEACE)
        assertNotNull(peaceStats)
        assertEquals(1, peaceStats?.gamesPlayed)
        assertEquals(1, peaceStats?.gamesWon)
        assertEquals(1.0, peaceStats?.winRate)

        val mafiaStats = statistics?.roleStatistics?.get(Role.MAFIA)
        assertNotNull(mafiaStats)
        assertEquals(1, mafiaStats?.gamesPlayed)
        assertEquals(1, mafiaStats?.gamesWon)
        assertEquals(1.0, mafiaStats?.winRate)
    }

    @Test
    fun `should return empty statistics for player without games`() {
        // Given
        val playerId = 1L
        val user = User(
            userId = playerId,
            username = "TestPlayer",
            rating = 1200.0,
            gamesPlayed = 0,
            gamesWon = 0
        )

        every { userRepository.findByIdOrNull(playerId) } returns user
        every { gameRepository.findAll() } returns emptyList()

        // When
        val statistics = service.getPlayerStatistics(playerId)

        // Then
        assertNotNull(statistics)
        assertEquals(0, statistics?.totalGames)
        assertEquals(0, statistics?.totalWins)
        assertEquals(0.0, statistics?.winRate)
        assertTrue(statistics?.roleStatistics?.isEmpty() ?: false)
    }

    @Test
    fun `should return null for non-existent player`() {
        // Given
        val playerId = 999L
        every { userRepository.findByIdOrNull(playerId) } returns null

        // When
        val statistics = service.getPlayerStatistics(playerId)

        // Then
        assertNull(statistics)
    }

    @Test
    fun `should search players by username`() {
        // Given
        val query = "test"
        val users = listOf(
            User(userId = 1L, username = "TestPlayer1", rating = 1200.0, gamesPlayed = 10, gamesWon = 5),
            User(userId = 2L, username = "TestPlayer2", rating = 1100.0, gamesPlayed = 8, gamesWon = 3)
        )

        every { userRepository.findByUsernameContainingIgnoreCase(query) } returns users

        // When
        val results = service.searchPlayers(query)

        // Then
        assertEquals(2, results.size)
        assertEquals("TestPlayer1", results[0].username)
        assertEquals("TestPlayer2", results[1].username)
    }

    @Test
    fun `should return empty list for short search query`() {
        // Given
        val query = "t"

        // When
        val results = service.searchPlayers(query)

        // Then
        assertTrue(results.isEmpty())
    }

    private fun createGame(
        gameId: Long,
        playerId: Long,
        playerRole: Role,
        result: PolemicaGameResult,
        points: Double
    ): Game {
        val players = listOf(
            PolemicaPlayer(
                position = Position.ONE,
                username = "TestPlayer",
                player = PolemicaUser(playerId, "TestPlayer"),
                role = playerRole
            )
        )

        val polemicaGame = PolemicaGame(
            id = gameId,
            result = result,
            players = players,
            votes = emptyList(),
            scoringVersion = "3.0"
        )

        val gamePoints = PolemicaGamePlayersPoints(
            players = listOf(
                PlayerPoints(
                    position = 1,
                    points = points
                )
            )
        )

        return Game(
            gameId = gameId,
            data = polemicaGame,
            points = gamePoints,
            gamePlace = PolemicaGamePlace(),
            started = LocalDateTime.now()
        )
    }
}
