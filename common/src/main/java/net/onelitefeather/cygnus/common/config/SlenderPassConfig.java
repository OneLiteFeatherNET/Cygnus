package net.onelitefeather.cygnus.common.config;

import static net.onelitefeather.cygnus.common.config.ConfigChecks.atLeast;
import static net.onelitefeather.cygnus.common.config.ConfigChecks.notNegative;

/**
 * The settings of the slender pass earning: how many Survivor rounds earn one pass, and the cap that earned passes
 * never lift a balance above. Read from the keys {@code pass.pity-rounds} and {@code pass.max-balance}.
 *
 * @param pityRounds the Survivor rounds until one pass is earned, at least 1
 * @param maxBalance the cap for earned passes, zero or more
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.21.0
 */
public record SlenderPassConfig(int pityRounds, int maxBalance) {

    /** The defaults: a pass after ten Survivor rounds, at most five passes from earning. */
    public static final SlenderPassConfig DEFAULT = new SlenderPassConfig(10, 5);

    /**
     * Rejects values that would make the earning meaningless. Invalid values stop the start; they are not replaced by
     * the defaults.
     */
    public SlenderPassConfig {
        atLeast("pass.pity-rounds", pityRounds, 1);
        notNegative("pass.max-balance", maxBalance);
    }

    /**
     * Reads the section {@code pass.*}. A missing key takes its default, an invalid value fails the read.
     *
     * @param section the section that holds the {@code pass.} keys
     * @return the settings
     */
    public static SlenderPassConfig read(ConfigSection section) {
        return new SlenderPassConfig(
                section.getInt("pity-rounds", DEFAULT.pityRounds()),
                section.getInt("max-balance", DEFAULT.maxBalance())
        );
    }
}
