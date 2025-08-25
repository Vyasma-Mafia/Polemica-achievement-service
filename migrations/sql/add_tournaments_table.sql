-- Создание таблицы tournaments для хранения настраиваемых турниров
CREATE TABLE tournaments
(
    id               BIGINT PRIMARY KEY,
    name             VARCHAR(255) NOT NULL,
    games_per_series INTEGER      NOT NULL DEFAULT 4,
    created_at       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    active           BOOLEAN      NOT NULL DEFAULT TRUE
);

-- Создаем индекс для быстрого поиска активных турниров
CREATE INDEX idx_tournaments_active ON tournaments (active);
