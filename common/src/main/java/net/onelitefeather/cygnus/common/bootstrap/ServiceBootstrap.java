package net.onelitefeather.cygnus.common.bootstrap;

import net.minestom.server.Auth;
import net.minestom.server.MinecraftServer;
import net.minestom.server.command.CommandManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Wires the parts a CloudNet-managed service process needs: reading the bind address CloudNet
 * assigns per-service, resolving how incoming connections are authenticated, and reacting to
 * CloudNet's stdin-based stop signal instead of being killed after a timeout.
 *
 * @author TheMeinerLP
 * @version 1.1.0
 * @since 2.6.7
 **/
public final class ServiceBootstrap {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServiceBootstrap.class);
    private static final String DEFAULT_BIND_HOST = "localhost";
    private static final int DEFAULT_BIND_PORT = 25565;
    private static final String DEFAULT_WORKING_DIR = "";
    static final String VELOCITY_SECRET_PROPERTY = "minestom-velocity-secret";

    private ServiceBootstrap() {
    }

    /**
     * Resolves the host to bind the server to.
     *
     * @return the value of the {@code service.bind.host} system property CloudNet assigns per
     * service, or {@value #DEFAULT_BIND_HOST} for standalone (non-CloudNet) runs
     */
    public static String resolveBindHost() {
        return System.getProperty("service.bind.host", DEFAULT_BIND_HOST);
    }

    /**
     * Resolves the port to bind the server to.
     *
     * @return the value of the {@code service.bind.port} system property CloudNet assigns per
     * service, or {@value #DEFAULT_BIND_PORT} for standalone (non-CloudNet) runs
     */
    public static int resolveBindPort() {
        return Integer.getInteger("service.bind.port", DEFAULT_BIND_PORT);
    }

    /**
     * Resolves the working directory root used to locate config, map, and other data files.
     *
     * @return the value of the {@code service.working.dir} system property CloudNet assigns per
     * service, or the JVM's current working directory for standalone (non-CloudNet) runs
     */
    public static Path resolveWorkingDirectory() {
        return Paths.get(System.getProperty("service.working.dir", DEFAULT_WORKING_DIR));
    }

    /**
     * Resolves how incoming connections are authenticated.
     * <p>
     * Passing {@code -Dminestom-velocity-secret=<secret>} puts the server behind a Velocity proxy:
     * the secret is the {@code forwarding.secret} of that proxy, and players are then expected to
     * arrive through it rather than connect directly. Without the property the server keeps
     * authenticating in offline mode, which is what a standalone run needs.
     * </p>
     * <p>
     * The result has to reach {@code MinecraftServer.init(Auth)} - Minestom binds the {@link Auth}
     * to the server process at that point and offers no way to switch it on afterwards.
     * </p>
     *
     * @return {@link Auth.Velocity} carrying the configured secret, or {@link Auth.Offline} if the
     * property is absent or blank
     */
    public static Auth resolveAuth() {
        String secret = System.getProperty(VELOCITY_SECRET_PROPERTY);
        if (secret == null) {
            return new Auth.Offline();
        }
        secret = secret.trim();
        if (secret.isEmpty()) {
            // An empty secret is never a deliberate choice - it is a start script whose variable did
            // not expand. Refusing it beats handing Minestom a key it rejects with an exception that
            // says nothing about where the value came from.
            LOGGER.warn("{} is set but empty - falling back to offline mode authentication",
                    VELOCITY_SECRET_PROPERTY);
            return new Auth.Offline();
        }
        LOGGER.info("Velocity modern forwarding enabled - authenticating players through the proxy");
        return new Auth.Velocity(secret);
    }

    /**
     * Registers the {@link StopCommand} and starts a daemon thread reading commands from stdin,
     * so CloudNet can stop the service cleanly instead of killing it after a timeout. Should be
     * called once during startup, before the server starts accepting connections.
     */
    public static void installShutdownHandling() {
        MinecraftServer.getCommandManager().register(new StopCommand());
        Thread.ofPlatform().name("cygnus-console-input").daemon().start(ServiceBootstrap::listenForConsoleInput);
    }

    /**
     * Reads lines from {@link System#in} until the stream closes and executes each one as a
     * console command. This is what lets CloudNet's stdin-based {@code stop} signal reach the
     * {@link StopCommand}.
     */
    private static void listenForConsoleInput() {
        CommandManager commandManager = MinecraftServer.getCommandManager();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) continue;
                commandManager.execute(commandManager.getConsoleSender(), line);
            }
        } catch (IOException e) {
            LOGGER.error("Failed to read command from stdin", e);
        }
    }
}
