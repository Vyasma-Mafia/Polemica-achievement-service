# Active Context

This file tracks the project's current status, including recent changes, current goals, and open questions.
2025-01-27 - Log of updates made.

*

## Current Focus

* Memory Bank now comprehensively documents all major subsystems of the service:
    * **Achievement System:** 22 achievements across RED, BLACK, and COMMON categories with incremental processing
    * **Rating System:** OpenSkill-based rating system with recalibration, competitive game support, and batch
      processing
    * **Statistics System:** Player and pair statistics with top partners analysis and caching
    * **Research System:** Specialized analytical endpoints for advanced game analysis
    * **Crawler System:** Automated data fetching from external sources with scheduled updates
    * **Tournament System:** Tournament and series management with competitive game integration

## Recent Changes

* [2026-02-25] - Added client-side player profile personalization for rating pages:
    * Added "home profile" controls on `player-history-view.html` and `player-games-view.html`
    * Added localStorage-based quick navigation panel on `ratings-view.html` ("Мой профиль")
    * Added localStorage-based "recently viewed profiles" history and rendering on `ratings-view.html`
* [2025-01-27] - Completed comprehensive Memory Bank documentation for all service subsystems:
    * Added detailed sections to `productContext.md` for Achievement, Rating, Statistics, Research, Crawler, and
      Tournament subsystems
    * Documented 7 key architectural decisions in `decisionLog.md` (focused on achievements, with patterns applicable to
      other systems)
    * Extended `systemPatterns.md` with achievement-specific patterns and general system patterns
    * Updated `activeContext.md` and `progress.md` to reflect complete system documentation

## Open Questions/Issues

* None at the moment.

## Local Run Notes

* Agent can run and verify the local service independently when needed.
* Environment variables for local run:
    * `DATABASE_PASSWORD=mafia`
    * `DATABASE_URL=jdbc:postgresql://localhost:5434/achievement`
* Env file path: `/Users/chulkov-alex/personal/mafia/polemica-achivement-service/.env`
