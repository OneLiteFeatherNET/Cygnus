package net.onelitefeather.cygnus.telemetry;

import java.util.List;

/**
 * The names of the pieces of work measured by {@link TickSections}.
 * <p>
 * A name ends up as the {@code cygnus.tick.section.name} attribute of a slow tick's child span, so
 * the set is fixed and small: one name per scheduled service, never per player or per round. A
 * service that runs once per player (the stamina bars) reports the sum of all of them under one name.
 * </p>
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.15.0
 */
public final class TickSectionNames {

    /** The creek's step. */
    public static final String CREEK = "creek";
    /** The reveal that makes a betrayed survivor glow for the slender. */
    public static final String GLOW_REVEAL = "glow-reveal";
    /** The slender's gaze check. */
    public static final String SLENDER_GAZE = "slender-gaze";
    /** The tunnel vision overlay. */
    public static final String TUNNEL_VISION = "tunnel-vision";
    /** The page glitch overlay. */
    public static final String PAGE_GLITCH = "page-glitch";
    /** The fading of blood splatters. */
    public static final String BLOOD_SPLATTER = "blood-splatter";
    /** The hint that tells survivors a page is close. */
    public static final String PAGE_PROXIMITY = "page-proximity";
    /** The ambient sounds and blackouts. */
    public static final String AMBIENT = "ambient";
    /** The adrenaline rush of hunted survivors. */
    public static final String ADRENALINE = "adrenaline";
    /** Taking over the slender role when the slender leaves. */
    public static final String SLENDER_TAKEOVER = "slender-takeover";
    /** The stamina bars of the survivors, all added up. */
    public static final String STAMINA = "stamina";
    /** The stamina bar of the slender. */
    public static final String SLENDER_BAR = "slender-bar";
    /** The jump scare's despawn. */
    public static final String JUMP_SCARE = "jump-scare";
    /** The lobby's waiting action bar. */
    public static final String LOBBY_WAITING = "lobby-waiting";
    /** The lobby's slide of the world time. */
    public static final String LOBBY_TIME = "lobby-time";

    /** Every name, for tests and documentation to check against. */
    public static final List<String> ALL = List.of(CREEK, GLOW_REVEAL, SLENDER_GAZE, TUNNEL_VISION, PAGE_GLITCH,
            BLOOD_SPLATTER, PAGE_PROXIMITY, AMBIENT, ADRENALINE, SLENDER_TAKEOVER, STAMINA, SLENDER_BAR,
            JUMP_SCARE, LOBBY_WAITING, LOBBY_TIME);

    private TickSectionNames() {
    }
}
