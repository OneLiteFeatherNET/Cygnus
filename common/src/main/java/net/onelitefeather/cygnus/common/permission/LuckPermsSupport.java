package net.onelitefeather.cygnus.common.permission;

import me.lucko.luckperms.minestom.app.LuckPermsMinestomHandle;
import me.lucko.luckperms.minestom.app.LuckPermsMinestomOptions;
import me.lucko.luckperms.minestom.loader.MinestomLoader;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Decides whether Cygnus runs with LuckPerms.
 * <p>
 * Minestom has no plugin folder, so LuckPerms is bootstrapped from {@code main} and shipped inside
 * the fat jar. A class path that leaves the loader out - the test class path does exactly that -
 * runs without any permission backend, and {@code PermissionAwarePlayer} then grants every
 * permission instead of failing on {@code LuckPermsProvider.get()}.
 * <p>
 * Detection reads the class path once and caches the answer, so a permission check never pays for
 * it twice.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.6.7
 **/
public final class LuckPermsSupport {

    private static final Logger LOGGER = LoggerFactory.getLogger(LuckPermsSupport.class);
    /** The LuckPerms meta that gives the monthly quota of slender passes of a player. @since 2.21.0 */
    public static final String MONTHLY_QUOTA_META = "cygnus.slender-pass.monthly-quota";
    private static final List<String> COMMAND_ALIASES =
            List.of("luckperms", "lp", "perm", "perms", "permission", "permissions");
    private static final String LOADER_CLASS = "me.lucko.luckperms.minestom.loader.MinestomLoader";
    private static final boolean PRESENT = detect();
    private static final AtomicBoolean FALLBACK_GRANT_LOGGED = new AtomicBoolean(false);

    /**
     * Returns whether LuckPerms can be used.
     *
     * @return {@code true} if the LuckPerms loader is on the class path
     */
    public static boolean isPresent() {
        return PRESENT;
    }

    /**
     * Logs, once, that the fallback is actually granting permissions.
     * <p>
     * The startup WARN from {@link #detect()} is easy to miss underneath Minestom's own boot
     * output, so this leaves a second trace at the moment the fallback first does something
     * observable: a real permission check resolving to {@link net.kyori.adventure.util.TriState#TRUE}
     * purely because LuckPerms is absent. Guarded by a single atomic compare-and-set so repeated
     * calls cost one volatile read and nothing else.
     *
     * @param permission the permission node that triggered the fallback
     */
    public static void noteFallbackGrant(String permission) {
        if (FALLBACK_GRANT_LOGGED.compareAndSet(false, true)) {
            LOGGER.warn("Granting '{}' unconditionally because LuckPerms is absent from the class path. "
                    + "Every subsequent permission check does the same; this line only prints once.", permission);
        }
    }

    /**
     * Starts LuckPerms and registers its shutdown hook. Does nothing when LuckPerms is absent.
     */
    public static void bootstrap() {
        if (!PRESENT) {
            return;
        }
        startLuckPerms();
    }

    /**
     * Starts LuckPerms. Kept separate so resolving {@link MinestomLoader} cannot happen while
     * {@link #bootstrap()} itself is being verified.
     */
    private static void startLuckPerms() {
        LuckPermsMinestomHandle ignored = MinestomLoader.create(
                        LuckPermsMinestomOptions.builder()
                                .dataDirectory(Path.of("data"))
                                .commandAliases(COMMAND_ALIASES)
                                .registerShutdownHook(true)
                                .build())
                .load()
                .enable();
    }

    /**
     * Resolves the loader class without initialising it.
     *
     * @return {@code true} if the class is available, {@code false} after logging a warning
     */
    /**
     * Reads the monthly quota of slender passes from the LuckPerms meta {@link #MONTHLY_QUOTA_META} of a player. The
     * cached user data is read, so no I/O happens.
     *
     * @param player the player
     * @return the quota, {@code 0} without LuckPerms, without user data, without the meta, or for a value that is not
     * an integer
     * @since 2.21.0
     */
    public static int monthlyQuota(UUID player) {
        if (!PRESENT) {
            return 0;
        }
        User user = LuckPermsProvider.get().getUserManager().getUser(player);
        if (user == null) {
            return 0;
        }
        String value = user.getCachedData().getMetaData().getMetaValue(MONTHLY_QUOTA_META);
        if (value == null) {
            return 0;
        }
        try {
            return Math.max(0, Integer.parseInt(value.trim()));
        } catch (NumberFormatException exception) {
            LOGGER.warn("The meta {} of {} is not a whole number: {}", MONTHLY_QUOTA_META, player, value);
            return 0;
        }
    }

    private static boolean detect() {
        try {
            Class.forName(LOADER_CLASS, false, LuckPermsSupport.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException _) {
            LOGGER.warn("LuckPerms is not on the class path. Every permission check resolves to TRUE. "
                    + "This mode is meant for local runs and tests, never for production.");
            return false;
        }
    }

    private LuckPermsSupport() {
    }
}
