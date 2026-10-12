package net.onelitefeather.cygnus.clientguard;

import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * What a client has said about itself so far: its brand and the mod channels it registered.
 * <p>
 * The client sends these in pieces, so a profile is built up over several messages. Every change returns a new
 * profile, which keeps a profile handed to another feature from changing under it.
 * </p>
 *
 * @param brand    the brand the client named itself with, or {@code null} while it has not said so
 * @param channels the plugin channels the client registered
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.16.0
 */
public record ClientProfile(@Nullable String brand, Set<String> channels) {

    /** A client that has said nothing yet. */
    public static final ClientProfile EMPTY = new ClientProfile(null, Set.of());

    /**
     * Copies the channels, so the profile cannot be changed from outside.
     */
    public ClientProfile {
        channels = Set.copyOf(channels);
    }

    /**
     * Returns the profile with another brand. A later brand replaces an earlier one.
     *
     * @param brand the brand the client named itself with
     * @return the updated profile
     */
    public ClientProfile withBrand(String brand) {
        return new ClientProfile(Objects.requireNonNull(brand, "brand"), this.channels);
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
        return new ClientProfile(this.brand, merged);
    }
}
