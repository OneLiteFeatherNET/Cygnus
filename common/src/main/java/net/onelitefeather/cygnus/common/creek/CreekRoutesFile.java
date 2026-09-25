package net.onelitefeather.cygnus.common.creek;

import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import net.onelitefeather.cygnus.common.map.GameMap;
import net.onelitefeather.cygnus.common.util.GsonHelper;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Reads and writes the {@code creek.json} next to a map's {@code map.json}.
 *
 * @author theEvilReaper
 * @version 1.0.0
 * @since 2.15.0
 */
public final class CreekRoutesFile {

    private static final Logger LOGGER = LoggerFactory.getLogger(CreekRoutesFile.class);
    private static final String FILE_NAME = "creek.json";
    private static final Type ROUTES_TYPE = TypeToken.getParameterized(List.class, CreekRoute.class).getType();

    private CreekRoutesFile() {
        // Nothing to do here
    }

    /**
     * Returns the {@code creek.json} path that belongs to a map file.
     *
     * @param mapFile the map file
     * @return the sibling path, whether it exists or not
     */
    public static Path resolve(Path mapFile) {
        return mapFile.resolveSibling(FILE_NAME);
    }

    /**
     * Loads the routes the game can use. Invalid routes are skipped with a warning that names them.
     *
     * @param mapFile the map file the routes belong to
     * @return the valid routes, empty if the file does not exist
     */
    public static List<CreekRoute> load(Path mapFile) {
        Path file = resolve(mapFile);
        return valid(read(file), file);
    }

    /**
     * Loads every route as written, including unfinished ones. Meant for the setup, which has to
     * show routes that are still being built.
     *
     * @param mapFile the map file the routes belong to
     * @return all routes, empty if the file does not exist
     */
    public static List<CreekRoute> loadUnchecked(Path mapFile) {
        return read(resolve(mapFile));
    }

    /**
     * Writes the routes to the sibling {@code creek.json}.
     *
     * @param mapFile the map file the routes belong to
     * @param routes  the routes to write
     */
    public static void save(Path mapFile, List<CreekRoute> routes) {
        Path file = resolve(mapFile);
        try {
            if (file.getParent() != null) {
                Files.createDirectories(file.getParent());
            }
            try (var writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                GsonHelper.GSON.toJson(routes, ROUTES_TYPE, writer);
            }
        } catch (IOException exception) {
            throw new UncheckedIOException("Failed to save creek routes to " + file, exception);
        }
    }

    /**
     * Returns a copy of the map carrying the valid routes from the sibling {@code creek.json}.
     *
     * @param mapFile the map file {@code map} was loaded from
     * @param map     the map
     * @return the map with its routes
     */
    public static GameMap loadInto(Path mapFile, GameMap map) {
        return map.withCreekRoutes(load(mapFile));
    }

    private static List<CreekRoute> read(Path file) {
        if (!Files.exists(file)) {
            return List.of();
        }
        try (var reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            List<CreekRoute> routes;
            try {
                routes = GsonHelper.GSON.fromJson(reader, ROUTES_TYPE);
            } catch (JsonSyntaxException exception) {
                throw new JsonSyntaxException("Failed to parse creek routes from " + file + ": " + exception.getMessage(), exception);
            }
            if (routes == null) return List.of();
            return routes.stream().filter(route -> route != null).toList();
        } catch (IOException exception) {
            throw new UncheckedIOException("Failed to load creek routes from " + file, exception);
        }
    }

    private static List<CreekRoute> valid(List<CreekRoute> routes, Path file) {
        Set<String> names = new HashSet<>();
        List<CreekRoute> valid = new ArrayList<>(routes.size());
        for (CreekRoute route : routes) {
            String problem = problemOf(route, names);
            if (problem != null) {
                LOGGER.warn("Skipping creek route '{}' in {}: {}", route.name(), file, problem);
                continue;
            }
            names.add(route.name());
            valid.add(route);
        }
        return List.copyOf(valid);
    }

    private static @Nullable String problemOf(CreekRoute route, Set<String> names) {
        if (route.name().isEmpty()) return "the name is empty";
        if (route.points().size() < CreekRoute.MIN_POINTS) {
            return "it needs at least " + CreekRoute.MIN_POINTS + " points";
        }
        if (names.contains(route.name())) return "another route already uses this name";
        return null;
    }
}
