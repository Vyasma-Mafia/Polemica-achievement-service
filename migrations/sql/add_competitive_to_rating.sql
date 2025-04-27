-- liquibase formatted sql
-- changeset your-name:add-is-competitive-to-player-rating-history

-- Добавляем поле для отслеживания типа игры
ALTER TABLE "player_rating_history"
    ADD COLUMN "is_competitive" boolean NOT NULL DEFAULT false;

-- rollback ALTER TABLE "player_rating_history" DROP COLUMN "is_competitive";
