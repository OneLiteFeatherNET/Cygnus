package net.onelitefeather.cygnus.common.map.adapter;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import net.minestom.server.coordinate.Pos;
import net.onelitefeather.cygnus.common.map.GameMap;
import net.onelitefeather.cygnus.common.page.PageResource;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Set;

/**
 * Custom JsonDeserializer for {@link GameMap} to safely handle missing or null collections during deserialization.
 *
 * @author Jotras
 * @version 1.0.0
 * @since 2.7.0
 */
public final class GameMapAdapter implements JsonDeserializer<GameMap> {

    private static final String NAME_KEY = "name";
    private static final String SPAWN_KEY = "spawn";
    private static final String SLENDER_SPAWN_KEY = "slenderSpawn";
    private static final String PAGE_FACES_KEY = "pageFaces";
    private static final String SURVIVOR_SPAWNS_KEY = "survivorSpawns";
    private static final String BUILDERS_KEY = "builders";

    private static final Type PAGE_FACES_TYPE = TypeToken.getParameterized(Set.class, PageResource.class).getType();
    private static final Type SURVIVOR_SPAWNS_TYPE = TypeToken.getParameterized(Set.class, Pos.class).getType();
    private static final Type BUILDERS_TYPE = TypeToken.getParameterized(List.class, String.class).getType();

    /**
     * Deserializes a {@link GameMap} from the given JSON element.
     * Missing or {@code null} fields are replaced with safe defaults to prevent {@link NullPointerException}s
     * during downstream processing (e.g. in {@link net.onelitefeather.cygnus.common.map.GameMapBuilder}).
     *
     * @param json    the JSON element to deserialize
     * @param typeOfT the type of the object to deserialize to
     * @param context the deserialization context
     * @return a fully initialized {@link GameMap} instance
     * @throws JsonParseException if the JSON structure is fundamentally invalid
     */
    @Override
    public GameMap deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonObject object = json.getAsJsonObject();

        String name = deserializeOrDefault(object, NAME_KEY, String.class, context, "");
        Pos spawn = deserializeOrDefault(object, SPAWN_KEY, Pos.class, context, null);
        Pos slenderSpawn = deserializeOrDefault(object, SLENDER_SPAWN_KEY, Pos.class, context, null);
        Set<PageResource> pageFaces = deserializeOrDefault(object, PAGE_FACES_KEY, PAGE_FACES_TYPE, context, Set.of());
        Set<Pos> survivorSpawns = deserializeOrDefault(object, SURVIVOR_SPAWNS_KEY, SURVIVOR_SPAWNS_TYPE, context, Set.of());
        List<String> builders = deserializeOrDefault(object, BUILDERS_KEY, BUILDERS_TYPE, context, List.of());

        return new GameMap(name, spawn, slenderSpawn, pageFaces, survivorSpawns, builders);
    }

    /**
     * Attempts to deserialize a value from the given {@link JsonObject} by key.
     * Returns the provided default value if the key is absent, the value is {@code null},
     * or deserialization itself yields {@code null}.
     *
     * @param object       the JSON object to read from
     * @param key          the field name to look up
     * @param type         the target type for deserialization
     * @param context      the deserialization context
     * @param defaultValue the fallback value when the field is missing or {@code null}
     * @param <T>          the expected return type
     * @return the deserialized value, or {@code defaultValue} if unavailable
     */
    private <T> @Nullable T deserializeOrDefault(
            JsonObject object,
            String key,
            Type type,
            JsonDeserializationContext context,
            @Nullable T defaultValue
    ) {
        if (!object.has(key) || object.get(key).isJsonNull()) {
            return defaultValue;
        }
        T result = context.deserialize(object.get(key), type);
        return result != null ? result : defaultValue;
    }
}
