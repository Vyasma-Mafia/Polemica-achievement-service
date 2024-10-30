CREATE TABLE achievement_game
(
    achievement_game_id bigint      NOT NULL PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
    game_id             bigint      NOT NULL,
    achievement         varchar(20) NOT NULL,

    created_at          TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,

    UNIQUE (game_id, achievement)
);

CREATE INDEX on achievement_game using hash (achievement);

CREATE TABLE achievement_game_user
(
    achievement_game_id bigint NOT NULL REFERENCES achievement_game (achievement_game_id),
    user_id             bigint NOT NULL REFERENCES Users (user_id),
    achievement_counter bigint,

    primary key (achievement_game_id, user_id)
);

DELETE
FROM achievement_gains
where 1 = 1;

ALTER TABLE achievement_gains
    rename to achievement_users;



