package net.onelitefeather.cygnus.common.creek;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import net.onelitefeather.cygnus.common.map.GameMap;
import net.onelitefeather.cygnus.common.map.settings.CreekSettings;
import net.onelitefeather.cygnus.common.util.GsonHelper;
import net.onelitefeather.cygnus.common.util.JsonSettings;
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
 * <p>
 * The file holds the routes under {@code routes} and the creek values the map sets for itself under
 * {@code settings}. A file in the old layout is the bare list and leaves every setting at its default.
 * </p>
 *
 * @author theEvilReaper
 * @version 1.0.0
 * @since 2.15.0
 */
public final class CreekRoutesFile {

    private static final Logger LOGGER = LoggerFactory.getLogger(CreekRoutesFile.class);
    private static final String FILE_NAME = "creek.json";
    private static final String ROUTES_KEY = "routes";
    private static final String SETTINGS_KEY = "settings";
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
     * Loads the creek values the map sets for itself from the sibling {@code creek.json}.
     *
     * @param mapFile the map file the settings belong to
     * @return the settings, {@link CreekSettings#DEFAULT} for a missing file and filled up with
     *         defaults for every key the file leaves out
     * @throws IllegalArgumentException if a value is out of range or two values contradict each other, naming the file
     */
    public static CreekSettings loadSettings(Path mapFile) {
        Path file = resolve(mapFile);
        JsonElement root = parse(file);
        if (root == null || root.isJsonNull()) return CreekSettings.DEFAULT;
        JsonSettings json = root.isJsonObject() ? JsonSettings.of(root.getAsJsonObject(), file) : JsonSettings.none(file);
        CreekSettings defaults = CreekSettings.DEFAULT;
        CreekSettings settings;
        try {
            settings = new CreekSettings(
                    json.getInt("sightRange", defaults.sightRange()),
                    json.getInt("stalkMinDistance", defaults.stalkMinDistance()),
                    json.getInt("stalkMaxDistance", defaults.stalkMaxDistance()),
                    json.getInt("respawnMinDistance", defaults.respawnMinDistance()),
                    json.getDouble("routeLinkDistance", defaults.routeLinkDistance()),
                    json.getInt("personalSpace", defaults.personalSpace()),
                    json.getDouble("launchHeight", defaults.launchHeight()));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Invalid settings in " + file + ": " + exception.getMessage(), exception);
        }
        json.logMissing(LOGGER);
        return settings;
    }

    /**
     * Writes the routes to the sibling {@code creek.json} with the default creek settings.
     *
     * @param mapFile the map file the routes belong to
     * @param routes  the routes to write
     */
    public static void save(Path mapFile, List<CreekRoute> routes) {
        save(mapFile, routes, CreekSettings.DEFAULT);
    }

    /**
     * Writes the routes and the creek settings to the sibling {@code creek.json}.
     *
     * @param mapFile  the map file the routes belong to
     * @param routes   the routes to write
     * @param settings the creek values the map sets for itself
     */
    public static void save(Path mapFile, List<CreekRoute> routes, CreekSettings settings) {
        Path file = resolve(mapFile);
        JsonObject root = new JsonObject();
        root.add(ROUTES_KEY, GsonHelper.GSON.toJsonTree(routes, ROUTES_TYPE));
        root.add(SETTINGS_KEY, GsonHelper.GSON.toJsonTree(settings));
        try {
            if (file.getParent() != null) {
                Files.createDirectories(file.getParent());
            }
            try (var writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                GsonHelper.GSON.toJson(root, writer);
            }
        } catch (IOException exception) {
            throw new UncheckedIOException("Failed to save creek routes to " + file, exception);
        }
    }

    /**
     * Returns a copy of the map carrying the valid routes and the creek settings from the sibling {@code creek.json}.
     *
     * @param mapFile the map file {@code map} was loaded from
     * @param map     the map
     * @return the map with its routes and settings
     */
    public static GameMap loadInto(Path mapFile, GameMap map) {
        return map.withCreekRoutes(load(mapFile)).withCreekSettings(loadSettings(mapFile));
    }

    private static List<CreekRoute> read(Path file) {
        JsonElement root = parse(file);
        if (root == null || root.isJsonNull()) return List.of();
        // The old layout is the bare list, the new one keeps it under "routes" next to the settings.
        JsonElement element = root.isJsonObject() ? root.getAsJsonObject().get(ROUTES_KEY) : root;
        if (element == null || element.isJsonNull()) return List.of();
        List<CreekRoute> routes;
        try {
            routes = GsonHelper.GSON.fromJson(element, ROUTES_TYPE);
        } catch (JsonSyntaxException exception) {
            throw new JsonSyntaxException("Failed to parse creek routes from " + file + ": " + exception.getMessage(), exception);
        }
        if (routes == null) return List.of();
        return routes.stream().filter(route -> route != null).toList();
    }

    private static @Nullable JsonElement parse(Path file) {
        if (!Files.exists(file)) return null;
        try (var reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader);
        } catch (JsonParseException exception) {
            throw new JsonSyntaxException("Failed to parse creek routes from " + file + ": " + exception.getMessage(), exception);
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

    @SuppressWarnings("ConstantValue")
    private static @Nullable String problemOf(CreekRoute route, Set<String> names) {
        if (route.name().isEmpty()) return "the name is empty";
        if (route.points().size() < CreekRoute.MIN_POINTS) {
            return "it needs at least " + CreekRoute.MIN_POINTS + " points";
        }
        // Gson leaves the position out of a hand-written point, or of one in the older flat layout.
        if (route.points().stream().anyMatch(point -> point.position() == null)) {
            return "a point has no position";
        }
        if (route.points().stream().anyMatch(point -> point.pauseMillis() < 0)) {
            return "a point has a negative pause";
        }
        if (names.contains(route.name())) return "another route already uses this name";
        return null;
    }
}
