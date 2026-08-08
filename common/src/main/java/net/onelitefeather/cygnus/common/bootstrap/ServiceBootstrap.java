package net.onelitefeather.cygnus.common.bootstrap;

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
 * assigns per-service, and reacting to CloudNet's stdin-based stop signal instead of being killed
 * after a timeout.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.6.7
 **/
public final class ServiceBootstrap {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServiceBootstrap.class);
    private static final String DEFAULT_BIND_HOST = "localhost";
    private static final int DEFAULT_BIND_PORT = 25565;
    private static final String DEFAULT_WORKING_DIR = "";

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
