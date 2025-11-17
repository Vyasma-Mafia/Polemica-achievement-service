# System Patterns *Optional*

This file documents recurring patterns and standards used in the project.
It is optional, but recommended to be updated as the project evolves.
2025-01-27 - Log of updates made.

*

## Coding Patterns

* The project follows standard Kotlin coding conventions.
* It utilizes the Spring Boot framework for dependency injection and other core functionalities.

## Architectural Patterns

* The application is designed as a monolithic web service with a layered architecture (Controller, Service, Repository).
* It exposes a `REST API` for external clients and a `Thymeleaf`-based web interface for users.

## Testing Patterns

* The project uses `JUnit 5` for unit and integration testing.
* `Testcontainers` are used to spin up a `PostgreSQL` database for integration tests.

## Achievement-Specific Patterns

### Achievement Implementation Pattern

* **Object Singleton Pattern:** All achievements are implemented as Kotlin `object` singletons implementing the
  `Achievement` interface
* **Registration:** Achievements are registered in a list in `AchievementServiceImpl.achievements`
* **ID Convention:** Achievement IDs use camelCase (e.g., `voteForBlack`, `winAsLastBlack`)
* **Metadata Properties:** Each achievement defines `id`, `name` (Russian), `description`, `levels` (threshold list),
  `category`, and optional `order`
* **Check Method:** Core logic in `check(game: PolemicaGame, position: Position): Int` method
    * Returns 0 if achievement not earned
    * Returns positive integer representing achievement value/count for that game
    * Uses helper method `boolToInt(value: Boolean)` for boolean-to-integer conversion

### Achievement Calculation Pattern

* **Incremental Processing:** Only processes games that haven't been checked for each achievement
* **Per-Game Transaction:** Each game is processed in a separate transaction via `AchievementTransactionalService`
* **Player Iteration:** For each game, iterates through all players and calls `achievement.check()` for each position
* **Counter Storage:** Achievement counters are stored per user per game, then aggregated via SQL queries
* **Level Calculation:** Levels computed on-the-fly by comparing cumulative counters to threshold lists

### Achievement Service Layer Pattern

* **Service Separation:** Business logic in `AchievementService`, transactional operations in
  `AchievementTransactionalService`
* **Achievement Registry:** All achievements listed explicitly in `AchievementServiceImpl.achievements` list
* **Map Lookup:** Achievements accessible via `achievementsMap` (ID -> Achievement) for O(1) lookup
* **Query Pattern:** Repository methods use native SQL with JOINs to aggregate counters across games
* **Date Filtering:** All query methods support optional `startDate` parameter with configurable default

### Achievement Data Model Pattern

* **Two-Table Design:** `AchievementGame` (game-achievement link) and `AchievementGameUser` (user-achievement instance
  with counter)
* **Composite Key:** `AchievementGameUser` uses composite key `(achievementGameId, userId)`
* **Unique Constraints:** `AchievementGame` has unique constraint on `(game_id, achievement)`
* **Counter Aggregation:** Counters stored per game, aggregated via `SUM(achievement_counter)` in queries
* **Timestamp Tracking:** Both entities have `created_at` and `updated_at` timestamps

### Achievement Testing Pattern

* **Test Utility:** `AchievementTestUtil.kt` provides `testAchievement()` helper function
* **Test Data:** Game data stored as JSON files in `src/test/resources/games/` directory
* **Test Structure:** Tests load game JSON, call `achievement.check()`, and assert expected integer result
* **Position Testing:** Tests often iterate through all positions to verify achievement behavior across roles
* **Example Pattern:**
  ```kotlin
  testAchievement(
      VoteForBlackAchievement, 
      gameId = 273009, 
      position = Position.ONE, 
      expected = 2
  )
  ```

### Achievement API Pattern

* **REST Endpoints:** Achievements exposed via `/achievements` and `/achievements/_top` endpoints
* **Query Parameters:** Support filtering by `usernames`, `ids`, `startDate`, and `limit`
* **Response Structure:** Returns `AchievementsWithGains` containing list of achievements and list of user gains
* **Date Format:** Uses ISO-8601 format (`yyyy-MM-ddTHH:mm:ss`) for date parameters
* **Swagger Documentation:** All endpoints documented with `@Operation` and `@Parameter` annotations

### Achievement Processing Flow Pattern

1. **Initialization:** `checkAchievements()` called (manually or via scheduler)
2. **User Extraction:** `saveUsers()` processes games with `processedVersion = 0` to extract and save users
3. **Achievement Loop:** For each achievement in registry:
    * Find unchecked games via `GameRepository.findAllWhereNotAchievement(achievementId)`
    * For each game, call `processAchievementForGame()` transactionally
4. **Per-Game Processing:**
    * Create `AchievementGame` record
    * Iterate players, call `achievement.check()`, create `AchievementGameUser` if result > 0
5. **Query Pattern:** When retrieving achievements, aggregate counters via SQL JOINs with date filtering
