CREATE TABLE Users
(
    user_id    bigint      NOT NULL PRIMARY KEY,
    username   varchar(50) NOT NULL,
    created_at TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,

    UNIQUE (username)
);

CREATE TABLE Games
(
    game_id           bigint    NOT NULL PRIMARY KEY,
    data              jsonb  NOT NULL,
    processed_version bigint NOT NULL DEFAULT 0,
    club_id           bigint,
    started           TIMESTAMP,
    created_at        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

create index game_started
    on Games using btree (started);

create index games_processed_version
    on Games (processed_version);

CREATE TABLE achievement_gains
(
    id          bigint generated always as identity,
    achievement varchar(20) not null,
    user_id     bigint      not null REFERENCES Users (user_id),
    achievement_counter bigint,

    created_at  TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (achievement, user_id)
);

create index achievement_gains_user_id
    on achievement_gains using hash (user_id);
