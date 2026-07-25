package net.onelitefeather.cygnus.common.bootstrap;

import net.minestom.server.MinecraftServer;
import net.minestom.server.command.CommandManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * Wires the parts a CloudNet-managed service process needs: reading the bind address CloudNet
 * assigns per-service, and reacting to CloudNet's stdin-based stop signal instead of being killed
 * after a timeout.
 */
public final class ServiceBootstrap {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServiceBootstrap.class);
    private static final String DEFAULT_BIND_HOST = "localhost";
    private static final int DEFAULT_BIND_PORT = 25565;

    private ServiceBootstrap() {
    }

    public static String resolveBindHost() {
        return System.getProperty("service.bind.host", DEFAULT_BIND_HOST);
    }

    public static int resolveBindPort() {
        return Integer.getInteger("service.bind.port", DEFAULT_BIND_PORT);
    }

    public static void installShutdownHandling() {
        MinecraftServer.getCommandManager().register(new StopCommand());
        Thread.ofPlatform().name("cygnus-console-input").daemon().start(ServiceBootstrap::listenForConsoleInput);
    }

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
