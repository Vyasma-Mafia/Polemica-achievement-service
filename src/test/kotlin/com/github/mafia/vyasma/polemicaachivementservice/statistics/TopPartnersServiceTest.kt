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
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.data.repository.findByIdOrNull
import java.time.LocalDateTime

class TopPartnersServiceTest {

    private lateinit var gameRepository: GameRepository
    private lateinit var userRepository: UserRepository
    private lateinit var config: PairStatisticsConfig
    private lateinit var service: PairStatisticsService

    @BeforeEach
    fun setUp() {
        gameRepository = mockk()
        userRepository = mockk()
        config = PairStatisticsConfig(
            minGamesThreshold = 2,
            topPartnersCount = 2,
            cacheEnabled = false,
            cacheTtlMinutes = 60
        )
        service = PairStatisticsService(gameRepository, userRepository, config)
    }

    @Test
    fun `getTopPartners should return empty lists when player not found`() {
        // Given
        every { userRepository.findByIdOrNull(1L) } returns null

        // When
        val result = service.getTopPartners(1L)

        // Then
        assertNull(result)
    }

    @Test
    fun `getTopPartners should return empty lists when player has no games`() {
        // Given
        val player = User(userId = 1L, username = "Player1", rating = 1000.0)
        every { userRepository.findByIdOrNull(1L) } returns player
        every { gameRepository.findAllByUserJoinedFromPlayerRatingHistory(player) } returns emptyList()

        // When
        val result = service.getTopPartners(1L)

        // Then
        assertNotNull(result)
        assertEquals(1L, result?.playerId)
        assertEquals("Player1", result?.playerName)
        assertTrue(result?.bestPartners?.isEmpty() ?: false)
        assertTrue(result?.worstPartners?.isEmpty() ?: false)
        assertEquals(0, result?.totalPartnersAnalyzed)
    }

    @Test
    fun `getTopPartners should correctly calculate partner statistics`() {
        // Given
        val player1 = User(userId = 1L, username = "Player1", rating = 1000.0)
        val player2 = User(userId = 2L, username = "Player2", rating = 1100.0)
        val player3 = User(userId = 3L, username = "Player3", rating = 1200.0)
        val player4 = User(userId = 4L, username = "Player4", rating = 900.0)

        every { userRepository.findByIdOrNull(1L) } returns player1
        every { userRepository.findByIdOrNull(2L) } returns player2
        every { userRepository.findByIdOrNull(3L) } returns player3
        every { userRepository.findByIdOrNull(4L) } returns player4

        val games = listOf(
            // Game 1: Player1 & Player2 (red) win against Player3 & Player4 (black)
            createGame(
                1L,
                listOf(
                    createPlayer(player1, Position.ONE, Role.PEACE),
                    createPlayer(player2, Position.TWO, Role.SHERIFF),
                    createPlayer(player3, Position.THREE, Role.MAFIA),
                    createPlayer(player4, Position.FOUR, Role.DON)
                ),
                PolemicaGameResult.RED_WIN
            ),
            // Game 2: Player1 & Player2 (red) win again
            createGame(
                2L,
                listOf(
                    createPlayer(player1, Position.ONE, Role.PEACE),
                    createPlayer(player2, Position.TWO, Role.PEACE),
                    createPlayer(player3, Position.THREE, Role.MAFIA),
                    createPlayer(player4, Position.FOUR, Role.DON)
                ),
                PolemicaGameResult.RED_WIN
            ),
            // Game 3: Player1 & Player3 (black) lose
            createGame(
                3L,
                listOf(
                    createPlayer(player1, Position.ONE, Role.MAFIA),
                    createPlayer(player2, Position.TWO, Role.PEACE),
                    createPlayer(player3, Position.THREE, Role.DON),
                    createPlayer(player4, Position.FOUR, Role.SHERIFF)
                ),
                PolemicaGameResult.RED_WIN
            ),
            // Game 4: Player1 & Player3 (black) lose again
            createGame(
                4L,
                listOf(
                    createPlayer(player1, Position.ONE, Role.DON),
                    createPlayer(player2, Position.TWO, Role.SHERIFF),
                    createPlayer(player3, Position.THREE, Role.MAFIA),
                    createPlayer(player4, Position.FOUR, Role.PEACE)
                ),
                PolemicaGameResult.RED_WIN
            )
        )

        every { gameRepository.findAllByUserJoinedFromPlayerRatingHistory(player1) } returns games
        every { userRepository.findByIdOrNull(2L) } returns player2
        every { userRepository.findByIdOrNull(3L) } returns player3
        every { userRepository.findByIdOrNull(4L) } returns player4

        // When
        val result = service.getTopPartners(1L)

        // Then
        assertNotNull(result)
        assertEquals(1L, result?.playerId)
        assertEquals("Player1", result?.playerName)

        // Best partners
        assertEquals(1, result?.bestPartners?.size)
        val bestPartner = result?.bestPartners?.first()
        assertEquals(2L, bestPartner?.partnerId)
        assertEquals("Player2", bestPartner?.partnerName)
        assertEquals(2, bestPartner?.totalGames)
        assertEquals(2, bestPartner?.wins)
        assertEquals(100.0, bestPartner?.winRate)

        // Worst partners
        assertEquals(1, result?.worstPartners?.size)
        val worstPartner = result?.worstPartners?.first()
        assertEquals(3L, worstPartner?.partnerId)
        assertEquals("Player3", worstPartner?.partnerName)
        assertEquals(2, worstPartner?.totalGames)
        assertEquals(0, worstPartner?.wins)
        assertEquals(0.0, worstPartner?.winRate)

        assertEquals(2, result?.totalPartnersAnalyzed)
    }

    @Test
    fun `getTopPartners should respect custom parameters`() {
        // Given
        val player1 = User(userId = 1L, username = "Player1", rating = 1000.0)
        every { userRepository.findByIdOrNull(1L) } returns player1
        every { gameRepository.findAllByUserJoinedFromPlayerRatingHistory(player1) } returns emptyList()

        // When
        val result = service.getTopPartners(1L, minGames = 5, topCount = 10)

        // Then
        assertNotNull(result)
        assertEquals(5, result?.minGamesThreshold)
    }

    private fun createGame(
        gameId: Long,
        players: List<PolemicaPlayer>,
        result: PolemicaGameResult
    ): Game {
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
            started = LocalDateTime.now(),
            stop = null,
            isLive = false,
            result = result,
            num = null,
            table = null,
            phase = null,
            factor = null
        )

        return Game(
            gameId = gameId,
            processedVersion = 1L,
            data = polemicaGame,
            points = PolemicaGamePlayersPoints(
                success = true,
                players = players.map { player ->
                    PlayerPoints(
                        position = player.position.value,
                        points = 1.0
                    )
                }
            ),
            gamePlace = PolemicaGamePlace(clubId = 1L, competitionId = null),
            started = LocalDateTime.now()
        )
    }

    private fun createPlayer(user: User, position: Position, role: Role): PolemicaPlayer {
        return PolemicaPlayer(
            position = position,
            username = user.username,
            role = role,
            techs = emptyList(),
            fouls = emptyList(),
            guess = null,
            player = PolemicaUser(user.userId, user.username),
            disqual = null,
            award = null
        )
    }
}
