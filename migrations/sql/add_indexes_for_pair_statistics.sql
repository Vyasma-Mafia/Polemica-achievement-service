-- Добавление индексов для оптимизации запросов парной статистики

-- Индекс на поле data для быстрого поиска по игрокам
CREATE INDEX IF NOT EXISTS idx_games_data_players ON games USING gin ((data -> 'players'));

-- Индекс на поле data для быстрого поиска по результату игры
CREATE INDEX IF NOT EXISTS idx_games_data_result ON games ((data ->> 'result'));

-- Составной индекс для быстрого поиска игр по игроку
CREATE INDEX IF NOT EXISTS idx_games_player_id ON games USING gin ((data -> 'players') jsonb_path_ops);

-- Индекс на поле started для сортировки по дате
CREATE INDEX IF NOT EXISTS idx_games_started ON games (started);

-- Индекс на поле points для фильтрации игр с очками
CREATE INDEX IF NOT EXISTS idx_games_points_not_null ON games (game_id) WHERE points IS NOT NULL;
