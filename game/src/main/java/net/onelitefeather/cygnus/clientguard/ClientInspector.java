package net.onelitefeather.cygnus.clientguard;

import net.onelitefeather.cygnus.common.config.ClientGuardConfig;

import java.util.List;
import java.util.Locale;

/**
 * Decides whether a client may stay, from what it has said about itself.
 * <p>
 * The checks run in this order, and the first one that fails gives the reason: a blocked brand fragment, a
 * blocked channel prefix, then the brand whitelist if one is configured. The whitelist only judges a client once
 * it has named a brand: the client may send its channel list first, so until then it passes that step, while the
 * blocklists still apply. Nothing here touches Minestom, so the rules can be tested without a server.
 * </p>
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.16.0
 */
public final class ClientInspector {

    private ClientInspector() {
    }

    /**
     * Checks a client against the configured rules.
     * <p>
     * A missing brand is never a failure. The brand and channel messages can arrive in either order, so a client
     * that has not named its brand yet passes the whitelist step. The whitelist is judged on the brand the client
     * has named; a brand that arrives later is checked again by the caller.
     * </p>
     *
     * @param profile the client's profile so far
     * @param config  the configured rules
     * @return {@link Verdict.Allowed} if the client may stay, otherwise {@link Verdict.Blocked} with the reason
     */
    public static Verdict inspect(ClientProfile profile, ClientGuardConfig config) {
        if (config.mode() == ClientGuardConfig.Mode.OFF) {
            return new Verdict.Allowed();
        }
        String brand = profile.brand();
        if (brand != null) {
            String folded = brand.toLowerCase(Locale.ROOT);
            for (String fragment : config.blockedBrands()) {
                if (folded.contains(fragment)) {
                    return new Verdict.Blocked("brand '" + brand + "' matches the blocked brand '" + fragment + "'");
                }
            }
        }
        for (String channel : profile.channels().stream().sorted().toList()) {
            String folded = channel.toLowerCase(Locale.ROOT);
            for (String prefix : config.blockedChannels()) {
                if (folded.startsWith(prefix)) {
                    return new Verdict.Blocked("channel '" + channel + "' matches the blocked channel '" + prefix + "'");
                }
            }
        }
        if (brand != null && !config.allowedBrands().isEmpty()) {
            return checkWhitelist(brand, config.allowedBrands());
        }
        return new Verdict.Allowed();
    }

    private static Verdict checkWhitelist(String brand, List<String> allowed) {
        if (allowed.contains(brand.toLowerCase(Locale.ROOT))) {
            return new Verdict.Allowed();
        }
        return new Verdict.Blocked("brand '" + brand + "' is not on the allowed brand list");
    }
}
