package net.onelitefeather.cygnus.clientguard;

import net.onelitefeather.cygnus.common.config.ShaderMod;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * What a client has said about itself so far: its brand, the mod channels it registered and the shader mods the
 * shader probe found on it.
 * <p>
 * The client sends the brand and the channels in pieces, so a profile is built up over several messages. The
 * shader mods come from the probe, see {@link ShaderModProbe}. Every change returns a new profile, which keeps a
 * profile handed to another feature from changing under it.
 * </p>
 *
 * @param brand        the brand the client named itself with, or {@code null} while it has not said so
 * @param channels     the plugin channels the client registered
 * @param detectedMods the shader mods the probe found on the client. A mod that was not probed, or whose keys
 *                     did not resolve, is not in here, which does not prove that the client lacks it.
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.16.0
 */
public record ClientProfile(@Nullable String brand, Set<String> channels, Set<ShaderMod> detectedMods) {

    /** A client that has said nothing yet. */
    public static final ClientProfile EMPTY = new ClientProfile(null, Set.of(), Set.of());

    /**
     * Creates a profile without any detected shader mods.
     *
     * @param brand    the brand the client named itself with, or {@code null}
     * @param channels the plugin channels the client registered
     */
    public ClientProfile(@Nullable String brand, Set<String> channels) {
        this(brand, channels, Set.of());
    }

    /**
     * Copies the channels and the detected mods, so the profile cannot be changed from outside.
     */
    public ClientProfile {
        channels = Set.copyOf(channels);
        detectedMods = Set.copyOf(detectedMods);
    }

    /**
     * Returns the profile with another brand. A later brand replaces an earlier one.
     *
     * @param brand the brand the client named itself with
     * @return the updated profile
     */
    public ClientProfile withBrand(String brand) {
        return new ClientProfile(Objects.requireNonNull(brand, "brand"), this.channels, this.detectedMods);
    }

    /**
     * Returns the profile with more channels registered. The channels already known are kept.
     *
     * @param more the channels the client registered
     * @return the updated profile
     */
    public ClientProfile withChannels(Collection<String> more) {
        Set<String> merged = new HashSet<>(this.channels);
        merged.addAll(more);
        return new ClientProfile(this.brand, merged, this.detectedMods);
    }

    /**
     * Returns the profile with more shader mods detected. The mods already known are kept.
     *
     * @param more the shader mods the probe found
     * @return the updated profile
     */
    public ClientProfile withDetectedMods(Collection<ShaderMod> more) {
        Set<ShaderMod> merged = EnumSet.noneOf(ShaderMod.class);
        merged.addAll(this.detectedMods);
        merged.addAll(more);
        return new ClientProfile(this.brand, this.channels, merged);
    }
}
