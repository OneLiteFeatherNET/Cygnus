package net.onelitefeather.cygnus.common.permission;

import me.lucko.luckperms.minestom.loader.MinestomLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Decides whether Cygnus runs with LuckPerms.
 * <p>
 * Minestom has no plugin folder, so LuckPerms is bootstrapped from {@code main} and shipped inside
 * the fat jar. A build that leaves the loader out - the test class path and the
 * {@code runWithoutLuckPerms} task do exactly that - runs without any permission backend, and
 * {@code PermissionAwarePlayer} then grants every permission instead of failing on
 * {@code LuckPermsProvider.get()}.
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
    private static final String LOADER_CLASS = "me.lucko.luckperms.minestom.loader.MinestomLoader";
    private static final boolean PRESENT = detect();

    /**
     * Returns whether LuckPerms can be used.
     *
     * @return {@code true} if the LuckPerms loader is on the class path
     */
    public static boolean isPresent() {
        return PRESENT;
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
        MinestomLoader.get().load().registerShutdownHook().start();
    }

    /**
     * Resolves the loader class without initialising it.
     *
     * @return {@code true} if the class is available, {@code false} after logging a warning
     */
    private static boolean detect() {
        try {
            Class.forName(LOADER_CLASS, false, LuckPermsSupport.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException exception) {
            LOGGER.warn("LuckPerms is not on the class path. Every permission check resolves to TRUE. "
                    + "This mode is meant for local runs and tests, never for production.");
            return false;
        }
    }

    private LuckPermsSupport() {
    }
}
