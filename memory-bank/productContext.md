# Product Context

This file provides a high-level overview of the project and the expected product that will be created. Initially it is
based upon projectBrief.md (if provided) and all other available project-related information in the working directory.
This file is intended to be updated as the project evolves, and should be used to inform all other modes of the
project's goals and context.
2025-01-27 - Log of updates made will be appended as footnotes to the end of this file.

*

## Project Goal

* The main goal of this project is to create a service for the Mafia community to analyze game statistics, with data
  collected from the official website. The primary focus is on player ratings and game history.

## Key Features

* **Player Ratings:** The service calculates and displays player ratings, which is the most important feature for the
  community.
* **Game History:** It provides a detailed history of games for each player.
* **Achievement Tracking:** The service calculates and displays various in-game achievements for players.
* **Detailed Statistics:** Provides comprehensive statistics for individual players and pairs of players.
* **Data Crawling:** Periodically fetches new game data from specified clubs.
* **REST API:** Exposes a REST API for accessing statistics and achievements.
* **Web Interface:** Offers a web interface for viewing ratings and game history.

## Achievement Subsystem

The achievement subsystem is a core feature that tracks and rewards player accomplishments in Mafia games. It provides
gamification elements that enhance player engagement and recognition.

### Purpose

Achievements recognize exceptional gameplay moments and patterns, such as winning under difficult conditions, making
accurate guesses, or demonstrating consistent skill. They are calculated automatically from game data and stored
persistently for player profiles.

### Achievement Architecture

**Core Components:**

* **Achievement Interface:** All achievements implement the `Achievement` interface, which defines:
    * `id`: Unique identifier (e.g., "voteForBlack", "winAsLastBlack")
    * `name`: Display name in Russian
    * `description`: Detailed explanation of the achievement
    * `levels`: List of thresholds for multi-level achievements (e.g., [1, 5, 25, 100, 500])
    * `category`: One of COMMON, RED, or BLACK
    * `order`: Display ordering (default 100)
    * `check(game, position)`: Core calculation method that returns an integer (0 = not achieved, >0 = achievement
      count/value)

* **Achievement Service Layer:**
    * `AchievementService`: Main service interface for querying achievements
    * `AchievementServiceImpl`: Implements achievement checking, retrieval, and top user rankings
    * `AchievementTransactionalService`: Handles transactional operations for processing achievements per game

* **Data Model:**
    * `AchievementGame`: Links achievements to games (one record per achievement per game)
    * `AchievementGameUser`: Links users to achievement instances with counters (composite key: achievement_game_id +
      user_id)
    * Stores achievement counters that accumulate across games to determine levels

* **REST API:**
    * `GET /achievements`: Retrieve achievements for specified users with optional date filtering
    * `GET /achievements/_top`: Get top achievements for specified users with ranking limits

### Achievement Types

The system currently implements 22 achievements across three categories:

**RED Category (Red Team Achievements):**

* `VoteForBlackAchievement`: Vote for black players as red
* `VotingOnlyForBlackAchievement`: Vote only for black players throughout the game
* `WinAsRedInLastAchievement`: Win as red team in the last round
* `WinWithoutCriticAchievement`: Win without having a critic role
* `FoulsForWinOnCriticAchievement`: Win despite having critic role
* `FindSheriffAchievement`: Successfully identify the sheriff
* `FindAllMafsAchievement`: Identify all mafia members
* `PartialMafsGuessAchievement`: Partially identify mafia members
* `FirstKickedFullGuessAchievement`: Get kicked first but correctly guess all roles
* `StrongCityAchievement`: Strong performance as city player
* `StrongSheriffAchievement`: Strong performance as sheriff
* `SheriffLiveAchievement`: Survive as sheriff until the end
* `SheriffViceAchievement`: Sheriff-related achievement
* `ManyVicesAchievement`: Multiple vice-related achievements

**BLACK Category (Black Team Achievements):**

* `WinAsBlackAchievement`: Win as black team
* `WinAsLastBlackAchievement`: Win as the last remaining black player
* `WinAsDonAchievement`: Win as don
* `WinThreeToThreeLastAchievement`: Win in a 3v3 final situation
* `FullMafsAchievement`: Complete mafia-related achievement
* `SniperAchievement`: Sniper-related achievement

**COMMON Category:**

* `SamuraiPathAchievement`: General gameplay achievement
* `WinWithSelfKillAchievement`: Win despite self-kill

### Achievement Calculation Process

1. **Initial Processing:**
    * `checkAchievements()` processes all games that haven't been checked for each achievement
    * Uses `GameRepository.findAllWhereNotAchievement(achievementId)` to find unchecked games
    * For each game, `processAchievementForGame()` is called transactionally

2. **Per-Game Processing:**
    * Creates an `AchievementGame` record linking the achievement to the game
    * Iterates through all players in the game
    * Calls `achievement.check(game.data, player.position)` for each player
    * If result > 0, creates `AchievementGameUser` record with the counter value
    * Counter values accumulate across games to determine achievement levels

3. **Level Calculation:**
    * Achievement levels are calculated based on cumulative counters
    * Each achievement defines threshold levels (e.g., [1, 5, 25, 100, 500])
    * Level is determined by finding the highest threshold the counter exceeds
    * Level 0 = not achieved, Level N = achieved N thresholds

4. **Date Filtering:**
    * Achievements can be filtered by game start date
    * Default cutoff date is configured via `achievementCheckGameStartedAfter`
    * Allows historical analysis and recalibration of achievement tracking

5. **Rechecking:**
    * `recheckAchievements()` deletes all existing achievement records and recalculates from scratch
    * Useful for fixing bugs in achievement logic or recalculating after rule changes

### Integration Points

* **Game Data:** Achievements analyze `PolemicaGame` objects stored in the `Game` entity
* **User Management:** Links to `User` entity for player identification
* **Scheduling:** Can be triggered via scheduled jobs (configured via `achievementCheckScheduler`)
* **API Exposure:** REST endpoints provide achievement data for frontend and external consumers

## Rating System

The rating system is the most important feature of the service, providing a skill-based ranking system for players using
the OpenSkill algorithm (Thurstone-Mosteller Full model).

### Purpose

The rating system calculates and tracks player skill levels based on game outcomes, providing a fair and accurate
representation of player ability. It supports both regular and competitive games, with different weightings for
tournament play.

### Rating Algorithm

**OpenSkill Implementation:**

* Uses `com.pocketcombats.openskill` library with Thurstone-Mosteller Full model
* Each player has two parameters: `mu` (mean skill) and `sigma` (uncertainty)
* Display rating calculated as `mu - 3 * sigma` (conservative estimate)
* Default values: `mu = 250.0`, `sigma = 83.33` (mu/3)

**Team-Based Rating:**

* Games are treated as team competitions (Red team vs Black team)
* Team ratings aggregated using `AverageTeamRatingAggregator`
* Rating adjustments calculated per player based on team performance
* Supports weighted adjustments based on player points and game type

### Rating Components

**Core Services:**

* `RatingService`: Orchestrates rating calculation and batch processing
* `PlayerRatingService`: Implements OpenSkill algorithm and rating updates
* `GamePointsService`: Fetches player points from external source
* `AverageTeamRatingAggregator`: Aggregates individual ratings into team ratings

**Data Model:**

* `User`: Stores current rating (`mu`, `sigma`, `rating`), games played/won counters
* `PlayerRatingHistory`: Tracks rating changes per game with full history
* `RecalibrationHistory`: Records sigma recalibration events

**Recalibration System:**

* Automatically adjusts `sigma` (uncertainty) based on:
    * Game count thresholds (25, 50, 100, 200, 500 games)
    * Rating stagnation detection (if average change < 2.0 over last 15 games)
* Prevents new players with high ratings from growing too quickly
* Ensures rating system remains dynamic for experienced players

### Rating Calculation Process

1. **Game Processing:**
    * `RatingService.crawlGames()` processes games without points
    * Fetches player points from external API via `GamePointsService`
    * Adjusts points based on game result predictions
    * Updates player ratings via `PlayerRatingService.updatePlayerRatings()`

2. **Rating Update:**
    * Extracts teams (Red/Black) with player points
    * Performs sigma recalibration if needed
    * Calculates team aggregate ratings
    * Uses OpenSkill adjudicator to compute rating adjustments
    * Applies weighted adjustments based on player performance and game type
    * Updates player records and saves rating history

3. **Competitive Games:**
    * Games marked as competitive (tournament/league) have 2.5x weight multiplier
    * Regular games have 0.5x weight multiplier
    * Competitive status determined by `competitionId` or game tags

4. **Batch Recalculation:**
    * `recalculateRatingBatched()` processes all games in batches of 200
    * Clears all rating data and recalculates from scratch
    * Useful for fixing bugs or adjusting algorithm parameters

### Rating API and UI

**REST Endpoints:**

* `GET /rating`: Paginated rating leaderboard with search functionality
* `GET /rating/player/{userId}`: Detailed player rating history with charts
* `GET /rating/games/{gameId}`: Game results with rating changes

**Web Interface:**

* Rating leaderboard with pagination and search
* Player history page with rating charts, recalibration markers, and statistics
* Game results page showing rating changes for all players
* Integration with achievements and role statistics

## Statistics System

The statistics system provides comprehensive analytics for individual players and player pairs, enabling detailed
performance analysis.

### Player Statistics

**Components:**

* `PlayerStatisticsService`: Calculates player-level statistics
* `PlayerStatistics`: Aggregated statistics including win rates, role performance, average points

**Features:**

* Total games played and win rate
* Statistics per role (Peace, Mafia, Don, Sheriff)
* Average points earned
* Best and worst games per role
* Player search functionality

**API:**

* `GET /api/players/{playerId}/statistics`: Full player statistics
* `GET /api/players/search`: Search players by username

### Pair Statistics

**Components:**

* `PairStatisticsService`: Analyzes games between two specific players
* `PairStatistics`: Comprehensive statistics for player pairs

**Features:**

* Same team statistics (games played together, win rate, performance as Red/Black)
* Opposite team statistics (head-to-head record, win rates)
* Detailed game list with pagination
* Top partners analysis (best/worst teammates by win rate)

**API:**

* `GET /api/players/{player1Id}/pair-statistics/{player2Id}`: Pair statistics
* `GET /api/players/{player1Id}/games-with/{player2Id}`: Common games list
* `GET /api/players/{playerId}/top-partners`: Best and worst teammates

**Caching:**

* Top partners results are cached for performance
* Cache configuration via `PairStatisticsConfig`

## Research System

The research system provides specialized analytical endpoints for advanced game analysis and data exploration.

### Research Features

**Components:**

* `ResearchService`: Interface for research operations
* `ResearchServiceImpl`: Implementation of research queries

**Available Research Endpoints:**

* Games where four red players voted for the same person
* Major pairs analysis (most frequent player combinations)
* Black team move statistics (team wins, referee decisions)
* Pair statistics for specific player combinations
* Tournament/competition data export (CSV format)
* Special game pattern analysis (e.g., "2-2-2-2 division in 9th position")

**API:**

* `GET /research/*`: Various research endpoints (see `ResearchController`)

## Data Crawler System

The crawler system fetches game data from external sources and populates the database.

### Crawler Components

**Services:**

* `CrawlerService`: Interface for crawling operations
* `CrawlerServiceImpl`: Implementation of data fetching
* `PolemicaCrawlSchedulerComponent`: Scheduled crawling component

**Features:**

* Fetches games from specified clubs (configured via `crawlClubs`)
* Parses and stores game data in database
* Supports scheduled automatic crawling
* Can reparse existing games if needed

**Configuration:**

* Crawling enabled/disabled via `crawlScheduler.enable`
* Crawling interval configurable via `crawlScheduler.interval`
* Club IDs specified in `crawlClubs` list

## Tournament/Series System

The tournament system manages competitive game series and league information.

### Tournament Features

**Components:**

* `TournamentService`: Tournament management
* `PolemicaTournamentService`: Integration with Polemica tournament data
* `SeriesResultsController`: Web interface for series results

**Features:**

* Tournament and series display
* Competition results tracking
* CSV export for tournament data
* Integration with rating system (competitive games have higher weight)

**API:**

* `GET /clubs/polemicaspb/leagues`: League listings
* `GET /clubs/polemicaspb/leagues/competition/{competitionId}/series/{seriesNumber}`: Series results
* `POST /clubs/polemicaspb/leagues/add-tournament`: Add tournament
* `GET /clubs/polemicaspb/leagues/tournaments`: List tournaments

## Future Development Plans

* **Advanced Game Filters:** Add filters for games (e.g., games with a specific player, on a specific role, with complex
  conditions like "voted out at 9").
* **Player-to-Player Comparison:** Implement a feature to compare the statistics of two players.
* **Optimized Game Fetching:** Refactor the game fetching mechanism to use `updatedAt` from the source for more
  efficient data synchronization.
* **Tournament Display:** Add support for displaying tournament information from the `polemica` website.
* **Gamification of Achievements:** Introduce a system of levels or points for achievements to increase player
  engagement.
* **Personalized Recommendations:** Provide players with personalized recommendations for improving their gameplay based
  on their statistics.
* **Game Outcome Predictions:** Develop a feature to predict game outcomes based on statistical data.

## Overall Architecture

* **Backend:** The application is built using a `Kotlin/Spring Boot` stack.
* **Database:** It uses `PostgreSQL` to store all data related to games, players, ratings, and achievements.
* **Database Migrations:** `Liquibase` is used to manage database schema changes.
* **Frontend:** The web interface is rendered using `Thymeleaf`.
* **API Documentation:** `Swagger` is integrated to provide documentation for the `REST API`.
* **Monitoring:** `Prometheus` is used for monitoring application metrics.
* **Testing:** The project includes integration tests using `Testcontainers`.
* **Deployment:** The application is containerized using `Docker`.
