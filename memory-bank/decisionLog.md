# Decision Log

This file records architectural and implementation decisions using a list format.
2025-01-27 - Log of updates made.

*

## Decision: Achievement Interface Design with Integer Return Values

**Date:** (Inherited from existing codebase)

**Decision:** Achievements implement an interface with a `check(game, position)` method that returns an integer rather
than a boolean. The return value represents the achievement count or value for that game (0 = not achieved, >0 =
achievement value).

**Rationale:**

* Allows achievements to have cumulative counters across multiple games
* Supports multi-level achievements where players progress through thresholds (e.g., 1, 5, 25, 100, 500)
* Enables achievements that can be earned multiple times in a single game (e.g., voting for multiple black players)
* Provides flexibility for future achievement types that may have variable values

**Implementation Details:**

* Defined in `Achievement.kt` interface
* All 22 achievement implementations use this pattern
* Counter values are stored in `AchievementGameUser.achievementCounter` and aggregated via SQL queries
* Level calculation happens in `AchievementServiceImpl.getAchievementLevel()` by comparing counter to threshold list

---

## Decision: Two-Table Storage Model for Achievement Tracking

**Date:** (Inherited from existing codebase)

**Decision:** Use two separate JPA entities (`AchievementGame` and `AchievementGameUser`) to track achievements, rather
than a single denormalized table.

**Rationale:**

* `AchievementGame` provides a many-to-many relationship between games and achievements (one record per achievement per
  game)
* `AchievementGameUser` links users to specific achievement instances with counters (composite key:
  achievement_game_id + user_id)
* Separates game-level tracking from user-level tracking, allowing efficient queries
* Enables querying "which games had this achievement" separately from "which users earned it"

**Implementation Details:**

* `AchievementGame` has unique constraint on (game_id, achievement)
* `AchievementGameUser` uses composite key `AchievementGameUserKey(achievementGameId, userId)`
* Repository queries use JOINs to aggregate counters across games for user summaries
* Transactional service creates both records atomically in `processAchievementForGame()`

---

## Decision: Separation of Transactional and Business Logic Services

**Date:** (Inherited from existing codebase)

**Decision:** Split achievement processing into `AchievementService` (business logic) and
`AchievementTransactionalService` (database operations).

**Rationale:**

* Clear separation of concerns: business logic vs. data persistence
* Transactional service handles all database writes with `@Transactional` annotations
* Business service orchestrates achievement checking across all games and achievements
* Makes testing easier by allowing mocking of transactional operations
* Follows Spring Boot best practices for service layer organization

**Implementation Details:**

* `AchievementServiceImpl` contains the list of all achievements and orchestrates processing
* `AchievementTransactionalServiceImpl` handles per-game processing and user saving
* Transaction boundaries ensure atomicity when processing multiple players in a game
* `Game.processedVersion` field tracks which games have been processed for user extraction

---

## Decision: Object-Based Achievement Implementations

**Date:** (Inherited from existing codebase)

**Decision:** All achievements are implemented as Kotlin `object` singletons rather than classes or data classes.

**Rationale:**

* Achievements are stateless and immutable - they only contain metadata and calculation logic
* No need for multiple instances of the same achievement
* Simplifies registration: achievements are listed directly in `AchievementServiceImpl`
* Reduces memory footprint and improves performance
* Achievements can be referenced directly by their object name in code

**Implementation Details:**

* All 22 achievements are defined as `object AchievementName : Achievement`
* Registered in `AchievementServiceImpl.achievements` list
* Accessed via `achievementsMap` for O(1) lookup by ID
* Test utilities reference achievement objects directly

---

## Decision: Incremental Achievement Processing with Game Tracking

**Date:** (Inherited from existing codebase)

**Decision:** Process achievements incrementally by tracking which games have been checked for each achievement, rather
than recalculating all achievements for all games every time.

**Rationale:**

* Performance: only processes new games that haven't been checked yet
* Scalability: as game database grows, processing time only increases for new games
* Idempotency: safe to run multiple times without duplicating records
* Uses `GameRepository.findAllWhereNotAchievement(achievementId)` to find unchecked games

**Implementation Details:**

* `AchievementGame` table tracks which games have been processed for which achievements
* Repository query excludes games that already have an `AchievementGame` record for that achievement
* `recheckAchievements()` method provides full recalculation when needed (deletes all records first)
* Supports fixing bugs in achievement logic by recalculating from scratch

---

## Decision: Date-Based Filtering for Achievement Queries

**Date:** (Inherited from existing codebase, enhanced with date filtering)

**Decision:** Achievement queries support optional date filtering via `startDate` parameter, with a configurable default
cutoff date.

**Rationale:**

* Allows historical analysis: "achievements earned since date X"
* Supports recalibration: can reset achievement tracking from a specific date
* Default cutoff date (`achievementCheckGameStartedAfter`) prevents processing very old games that may have data quality
  issues
* Enables time-based leaderboards and statistics

**Implementation Details:**

* `AchievementService.getAchievements()` and `getTopAchievementUsers()` accept `LocalDateTime?` parameter
* Repository queries use `WHERE g.started >= :startDate` in native SQL
* Default date comes from `ApplicationConfig.achievementCheckGameStartedAfter`
* API endpoints expose date filtering via `@RequestParam(required = false) startDate: LocalDateTime?`

---

## Decision: Multi-Level Achievement System with Threshold Lists

**Date:** (Inherited from existing codebase)

**Decision:** Achievements support multiple levels defined by threshold lists (e.g.,
`levels = listOf(1, 5, 25, 100, 500)`), where level is calculated by comparing cumulative counter to thresholds.

**Rationale:**

* Provides progression and long-term goals for players
* Encourages repeated gameplay to reach higher levels
* Flexible: each achievement can define its own progression curve
* Level calculation is simple: find highest threshold that counter exceeds

**Implementation Details:**

* `Achievement.levels` property defines thresholds
* `AchievementServiceImpl.getAchievementLevel()` calculates level by iterating through thresholds
* Level 0 = counter is 0 or below first threshold
* Level N = counter exceeds N thresholds (returns `levels.size` if counter exceeds all thresholds)
* Displayed in API responses as `achievementLevel` field
