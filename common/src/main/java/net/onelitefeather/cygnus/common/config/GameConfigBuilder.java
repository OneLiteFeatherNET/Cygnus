package net.onelitefeather.cygnus.common.config;

import net.kyori.adventure.key.Key;
import org.jetbrains.annotations.Nullable;

import java.net.URI;

/**
 * The {@link GameConfigBuilder} is the implementation of the {@link GameConfig.Builder} interface.
 * It collects the values for a game configuration and creates a {@link GameConfigImpl} from them.
 *
 * @author theEvilReaper
 * @version 1.2.0
 * @since 1.0.0
 */
public final class GameConfigBuilder implements GameConfig.Builder {

    private int minPlayers;
    private int maxPlayers;
    private int lobbyTime;
    private int maxGameTime;
    private int slenderTeamSize;
    private int survivorTeamSize;
    private @Nullable String sentryDsn;
    private @Nullable URI resourcePackUrl;
    private @Nullable String resourcePackSha1;
    private boolean pageProximityEnabled;
    private int pageProximityRange;
    private int pageProximityInterval;
    private Key pageProximitySound = GameConfig.DEFAULT_PAGE_PROXIMITY_SOUND;
    private float pageProximityVolumeFactor = GameConfig.DEFAULT_PAGE_PROXIMITY_VOLUME_FACTOR;
    private boolean damageSoundEnabled;
    private int damageSoundCooldown;
    private Key damageSound = GameConfig.DEFAULT_DAMAGE_SOUND;
    // Pre-set rather than left at zero, the way pageProximitySound above is. build() checks the
    // two distances against each other, and a builder used directly - as the tests do - would trip
    // that check on 0 >= 0 without ever having said anything about the gaze.
    private int glitchRange = GameConfig.DEFAULT_GLITCH_RANGE;
    private int glitchCloseRange = GameConfig.DEFAULT_GLITCH_CLOSE_RANGE;
    private int glitchViewAngle = GameConfig.DEFAULT_GLITCH_VIEW_ANGLE;
    private float lobbyAtmosphereShare = GameConfig.DEFAULT_LOBBY_ATMOSPHERE_SHARE;
    private boolean slenderStaticEnabled;
    private Key slenderStaticSound = GameConfig.DEFAULT_SLENDER_STATIC_SOUND;
    // Pre-set for the same reason as the glitch distances above: build() checks the two intervals
    // and the two volumes against each other, and a builder that was never told about the static
    // would trip those checks on a pair of zeroes.
    private int slenderStaticQuietInterval = GameConfig.DEFAULT_SLENDER_STATIC_QUIET_INTERVAL;
    private int slenderStaticFranticInterval = GameConfig.DEFAULT_SLENDER_STATIC_FRANTIC_INTERVAL;
    private float slenderStaticMinVolume = GameConfig.DEFAULT_SLENDER_STATIC_MIN_VOLUME;
    private float slenderStaticMaxVolume = GameConfig.DEFAULT_SLENDER_STATIC_MAX_VOLUME;

    @Override
    public GameConfig.Builder minPlayers(int minPlayers) {
        this.minPlayers = minPlayers;
        return this;
    }

    @Override
    public GameConfig.Builder maxPlayers(int maxPlayers) {
        this.maxPlayers = maxPlayers;
        return this;
    }

    @Override
    public GameConfig.Builder lobbyTime(int lobbyTime) {
        if (lobbyTime <= GameConfig.FORCE_START_TIME) {
            throw new IllegalArgumentException("Lobby time must be greater than " + GameConfig.FORCE_START_TIME);
        }
        this.lobbyTime = lobbyTime;
        return this;
    }

    @Override
    public GameConfig.Builder gameTime(int gameTime) {
        this.maxGameTime = gameTime;
        return this;
    }

    @Override
    public GameConfig.Builder slenderTeamSize(int slenderTeamSize) {
        int minSlenderSize = InternalGameConfig.defaultConfig().slenderTeamSize();
        if (slenderTeamSize < minSlenderSize) {
            throw new IllegalArgumentException("Slender team size must be at least " + minSlenderSize);
        }
        this.slenderTeamSize = slenderTeamSize;
        return this;
    }

    @Override
    public GameConfig.Builder survivorTeamSize(int survivorTeamSize) {
        int minSurvivorSize = InternalGameConfig.defaultConfig().slenderTeamSize() + 1;
        if (survivorTeamSize < minSurvivorSize) {
            throw new IllegalArgumentException("Survivor team size must be at least " + minSurvivorSize);
        }
        this.survivorTeamSize = survivorTeamSize;
        return this;
    }

    @Override
    public GameConfig.Builder sentryDsn(@Nullable String sentryDsn) {
        this.sentryDsn = sentryDsn;
        return this;
    }

    @Override
    public GameConfig.Builder resourcePackUrl(@Nullable URI resourcePackUrl) {
        this.resourcePackUrl = resourcePackUrl;
        return this;
    }

    @Override
    public GameConfig.Builder resourcePackSha1(@Nullable String resourcePackSha1) {
        this.resourcePackSha1 = resourcePackSha1;
        return this;
    }

    @Override
    public GameConfig.Builder pageProximityEnabled(boolean pageProximityEnabled) {
        this.pageProximityEnabled = pageProximityEnabled;
        return this;
    }

    @Override
    public GameConfig.Builder pageProximityRange(int pageProximityRange) {
        if (pageProximityRange < 1 || pageProximityRange > GameConfig.MAX_PAGE_PROXIMITY_RANGE) {
            throw new IllegalArgumentException(
                    "Page proximity range must be between 1 and " + GameConfig.MAX_PAGE_PROXIMITY_RANGE);
        }
        this.pageProximityRange = pageProximityRange;
        return this;
    }

    @Override
    public GameConfig.Builder pageProximityInterval(int pageProximityInterval) {
        if (pageProximityInterval < 1) {
            throw new IllegalArgumentException("Page proximity interval must be at least 1 tick");
        }
        this.pageProximityInterval = pageProximityInterval;
        return this;
    }

    @Override
    public GameConfig.Builder pageProximitySound(Key pageProximitySound) {
        this.pageProximitySound = pageProximitySound;
        return this;
    }

    @Override
    public GameConfig.Builder pageProximityVolumeFactor(float pageProximityVolumeFactor) {
        if (pageProximityVolumeFactor < 1.0F
                || pageProximityVolumeFactor > GameConfig.MAX_PAGE_PROXIMITY_VOLUME_FACTOR) {
            throw new IllegalArgumentException(
                    "Page proximity volume factor must be between 1 and "
                            + GameConfig.MAX_PAGE_PROXIMITY_VOLUME_FACTOR);
        }
        this.pageProximityVolumeFactor = pageProximityVolumeFactor;
        return this;
    }

    @Override
    public GameConfig.Builder damageSoundEnabled(boolean damageSoundEnabled) {
        this.damageSoundEnabled = damageSoundEnabled;
        return this;
    }

    @Override
    public GameConfig.Builder damageSoundCooldown(int damageSoundCooldown) {
        if (damageSoundCooldown < 1) {
            throw new IllegalArgumentException("Damage sound cooldown must be at least 1 tick");
        }
        this.damageSoundCooldown = damageSoundCooldown;
        return this;
    }

    @Override
    public GameConfig.Builder damageSound(Key damageSound) {
        this.damageSound = damageSound;
        return this;
    }

    @Override
    public GameConfig.Builder glitchRange(int glitchRange) {
        if (glitchRange < 1 || glitchRange > GameConfig.MAX_GLITCH_RANGE) {
            throw new IllegalArgumentException(
                    "Glitch range must be between 1 and " + GameConfig.MAX_GLITCH_RANGE);
        }
        this.glitchRange = glitchRange;
        return this;
    }

    @Override
    public GameConfig.Builder glitchCloseRange(int glitchCloseRange) {
        if (glitchCloseRange < 1) {
            throw new IllegalArgumentException("Glitch close range must be at least 1 block");
        }
        this.glitchCloseRange = glitchCloseRange;
        return this;
    }

    @Override
    public GameConfig.Builder glitchViewAngle(int glitchViewAngle) {
        if (glitchViewAngle < 1 || glitchViewAngle > GameConfig.MAX_GLITCH_VIEW_ANGLE) {
            throw new IllegalArgumentException(
                    "Glitch view angle must be between 1 and " + GameConfig.MAX_GLITCH_VIEW_ANGLE + " degrees");
        }
        this.glitchViewAngle = glitchViewAngle;
        return this;
    }

    @Override
    public GameConfig.Builder lobbyAtmosphereShare(float lobbyAtmosphereShare) {
        if (lobbyAtmosphereShare < 0.0F || lobbyAtmosphereShare > 1.0F) {
            throw new IllegalArgumentException("Lobby atmosphere share must be between 0 and 1");
        }
        this.lobbyAtmosphereShare = lobbyAtmosphereShare;
        return this;
    }

    @Override
    public GameConfig.Builder slenderStaticEnabled(boolean slenderStaticEnabled) {
        this.slenderStaticEnabled = slenderStaticEnabled;
        return this;
    }

    @Override
    public GameConfig.Builder slenderStaticSound(Key slenderStaticSound) {
        this.slenderStaticSound = slenderStaticSound;
        return this;
    }

    @Override
    public GameConfig.Builder slenderStaticQuietInterval(int slenderStaticQuietInterval) {
        if (slenderStaticQuietInterval < 1
                || slenderStaticQuietInterval > GameConfig.MAX_SLENDER_STATIC_INTERVAL) {
            throw new IllegalArgumentException(
                    "Slender static quiet interval must be between 1 and "
                            + GameConfig.MAX_SLENDER_STATIC_INTERVAL + " seconds");
        }
        this.slenderStaticQuietInterval = slenderStaticQuietInterval;
        return this;
    }

    @Override
    public GameConfig.Builder slenderStaticFranticInterval(int slenderStaticFranticInterval) {
        if (slenderStaticFranticInterval < 1) {
            throw new IllegalArgumentException("Slender static frantic interval must be at least 1 second");
        }
        this.slenderStaticFranticInterval = slenderStaticFranticInterval;
        return this;
    }

    @Override
    public GameConfig.Builder slenderStaticMinVolume(float slenderStaticMinVolume) {
        this.slenderStaticMinVolume = checkVolume(slenderStaticMinVolume, "minimum");
        return this;
    }

    @Override
    public GameConfig.Builder slenderStaticMaxVolume(float slenderStaticMaxVolume) {
        this.slenderStaticMaxVolume = checkVolume(slenderStaticMaxVolume, "maximum");
        return this;
    }

    /**
     * Checks a static volume against the range a sound can carry.
     *
     * @param volume the volume to check
     * @param name   how the volume is named in the message of a failure
     * @return the volume
     * @throws IllegalArgumentException if the volume is outside 0 to 1
     */
    private static float checkVolume(float volume, String name) {
        if (volume < 0.0F || volume > 1.0F) {
            throw new IllegalArgumentException(
                    "Slender static " + name + " volume must be between 0 and 1");
        }
        return volume;
    }

    /**
     * {@inheritDoc}
     * <p>
     * The two glitch distances are checked against each other here rather than in their setters.
     * Each one alone is a valid number; only together do they say whether the slope they describe
     * runs the right way, and a setter cannot know that - it would depend on which of the two was
     * called first.
     * </p>
     *
     * @throws IllegalArgumentException if the close range is not below the range
     */
    @Override
    public GameConfig build() {
        if (glitchCloseRange >= glitchRange) {
            throw new IllegalArgumentException(
                    "Glitch close range (" + glitchCloseRange + ") must be below the glitch range ("
                            + glitchRange + ")");
        }
        if (slenderStaticFranticInterval >= slenderStaticQuietInterval) {
            throw new IllegalArgumentException(
                    "Slender static frantic interval (" + slenderStaticFranticInterval
                            + ") must be below the quiet interval (" + slenderStaticQuietInterval + ")");
        }
        if (slenderStaticMinVolume > slenderStaticMaxVolume) {
            throw new IllegalArgumentException(
                    "Slender static minimum volume (" + slenderStaticMinVolume
                            + ") must not be above the maximum volume (" + slenderStaticMaxVolume + ")");
        }
        return new GameConfigImpl(
                minPlayers,
                maxPlayers,
                lobbyTime,
                maxGameTime,
                slenderTeamSize,
                survivorTeamSize,
                sentryDsn,
                resourcePackUrl,
                resourcePackSha1,
                pageProximityEnabled,
                pageProximityRange,
                pageProximityInterval,
                pageProximitySound,
                pageProximityVolumeFactor,
                damageSoundEnabled,
                damageSoundCooldown,
                damageSound,
                glitchRange,
                glitchCloseRange,
                glitchViewAngle,
                slenderStaticEnabled,
                slenderStaticSound,
                slenderStaticQuietInterval,
                slenderStaticFranticInterval,
                slenderStaticMinVolume,
                slenderStaticMaxVolume,
                lobbyAtmosphereShare
        );
    }
}
