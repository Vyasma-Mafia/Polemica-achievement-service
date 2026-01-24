-- Создание таблицы processed_tournament_ids для отслеживания обработанных турниров
CREATE TABLE processed_tournament_ids
(
    id           BIGINT PRIMARY KEY,
    processed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Создаем индекс для быстрого поиска обработанных турниров
CREATE INDEX idx_processed_tournament_ids_processed_at ON processed_tournament_ids (processed_at);
