package net.onelitefeather.cygnus.stats;

import java.util.UUID;

/**
 * Builds a {@link YearStatsView} for tests with every counter at zero unless set.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.16.0
 */
final class YearStatsViewBuilder {

    private static final UUID PLAYER = UUID.fromString("00000000-0000-0000-0000-0000000000a1");

    private int rounds;
    private int survivorRounds;
    private int slenderRounds;
    private int winsSurvivor;
    private int winsSlender;
    private int abandoned;
    private int pages;
    private int kills;
    private int deaths;
    private long survived;
    private long playTime;

    private YearStatsViewBuilder() {
    }

    static YearStatsViewBuilder of() {
        return new YearStatsViewBuilder();
    }

    YearStatsViewBuilder rounds(int rounds) {
        this.rounds = rounds;
        return this;
    }

    YearStatsViewBuilder survivorRounds(int rounds) {
        this.survivorRounds = rounds;
        return this;
    }

    YearStatsViewBuilder slenderRounds(int rounds) {
        this.slenderRounds = rounds;
        return this;
    }

    YearStatsViewBuilder wins(int survivor, int slender) {
        this.winsSurvivor = survivor;
        this.winsSlender = slender;
        return this;
    }

    YearStatsViewBuilder abandoned(int abandoned) {
        this.abandoned = abandoned;
        return this;
    }

    YearStatsViewBuilder pages(int pages) {
        this.pages = pages;
        return this;
    }

    YearStatsViewBuilder kills(int kills) {
        this.kills = kills;
        return this;
    }

    YearStatsViewBuilder deaths(int deaths) {
        this.deaths = deaths;
        return this;
    }

    YearStatsViewBuilder survived(long seconds) {
        this.survived = seconds;
        return this;
    }

    YearStatsViewBuilder playTime(long seconds) {
        this.playTime = seconds;
        return this;
    }

    YearStatsView build() {
        return new YearStatsView(PLAYER, 2026, this.rounds, this.slenderRounds, this.survivorRounds,
                this.winsSlender, this.winsSurvivor, 0, 0, this.abandoned, this.playTime, this.pages, 0, 0,
                this.deaths, this.survived, 0L, 0, 0, 0.0D, 0, this.kills, 0, null, 0, 0);
    }
}
