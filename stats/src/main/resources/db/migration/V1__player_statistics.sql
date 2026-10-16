-- Yearly player statistics.
--
-- Plain SQL that PostgreSQL and H2 in PostgreSQL mode both accept: no PostgreSQL-only types, UUIDs as
-- UUID. The year column is called stats_year because YEAR is a reserved word in H2.

-- One row per participant and round. The source of truth: player_year_stats can be rebuilt from it.
CREATE TABLE player_round_stats (
    round_id              UUID                     NOT NULL,
    player_uuid           UUID                     NOT NULL,
    stats_year            INTEGER                  NOT NULL,
    finished_at           TIMESTAMP WITH TIME ZONE NOT NULL,
    role                  VARCHAR(16)              NOT NULL,
    outcome               VARCHAR(16)              NOT NULL,
    finish_reason         VARCHAR(32)              NOT NULL,
    pages_found           INTEGER                  NOT NULL,
    kills                 INTEGER                  NOT NULL,
    died                  BOOLEAN                  NOT NULL,
    survived              BOOLEAN                  NOT NULL,
    finisher              BOOLEAN                  NOT NULL,
    play_time_seconds     BIGINT                   NOT NULL,
    time_survived_seconds BIGINT                   NOT NULL,
    damage_taken          DOUBLE PRECISION         NOT NULL,
    painkillers_used      INTEGER                  NOT NULL,
    pages_against         INTEGER                  NOT NULL,
    participants          INTEGER                  NOT NULL,
    taken_over            BOOLEAN                  NOT NULL,
    PRIMARY KEY (round_id, player_uuid)
);

-- participants is the number of tracked participants (Slender and Survivors at the round start), the same
-- on every row of the round. role stays the role at the start; taken_over marks the survivor who became
-- the Slender when the original one left. Both are kept for a later role balancing.

-- Yearly reads and rebuilds of one player's rounds.
CREATE INDEX idx_player_round_stats_player_year ON player_round_stats (player_uuid, stats_year);

-- Recent history: the last N rounds of a player ordered by finish time, across all servers.
CREATE INDEX idx_player_round_stats_player_finished ON player_round_stats (player_uuid, finished_at);

-- One row per player and calendar year, for fast profile and leaderboard reads. Counters are summed,
-- best_* and longest_* keep the maximum, fastest_win_seconds the minimum. version is the optimistic lock
-- several servers writing to the same row rely on.
CREATE TABLE player_year_stats (
    player_uuid              UUID             NOT NULL,
    stats_year               INTEGER          NOT NULL,
    rounds_played            INTEGER          NOT NULL,
    rounds_slender           INTEGER          NOT NULL,
    rounds_survivor          INTEGER          NOT NULL,
    wins_slender             INTEGER          NOT NULL,
    wins_survivor            INTEGER          NOT NULL,
    losses_slender           INTEGER          NOT NULL,
    losses_survivor          INTEGER          NOT NULL,
    abandoned                INTEGER          NOT NULL,
    play_time_seconds        BIGINT           NOT NULL,
    wins_time_over           INTEGER          NOT NULL,
    wins_all_pages_found     INTEGER          NOT NULL,
    wins_all_survivor_dead   INTEGER          NOT NULL,
    wins_slender_left        INTEGER          NOT NULL,
    wins_survivor_left       INTEGER          NOT NULL,
    pages_found              INTEGER          NOT NULL,
    best_pages_round         INTEGER          NOT NULL,
    rounds_survived          INTEGER          NOT NULL,
    deaths                   INTEGER          NOT NULL,
    time_survived_seconds    BIGINT           NOT NULL,
    longest_survived_seconds BIGINT           NOT NULL,
    all_pages_contributions  INTEGER          NOT NULL,
    last_pages_found         INTEGER          NOT NULL,
    damage_taken             DOUBLE PRECISION NOT NULL,
    painkillers_used         INTEGER          NOT NULL,
    kills                    INTEGER          NOT NULL,
    best_kills_round         INTEGER          NOT NULL,
    fastest_win_seconds      BIGINT,
    pages_against            INTEGER          NOT NULL,
    takeovers                INTEGER          NOT NULL,
    version                  BIGINT           NOT NULL,
    PRIMARY KEY (player_uuid, stats_year)
);
