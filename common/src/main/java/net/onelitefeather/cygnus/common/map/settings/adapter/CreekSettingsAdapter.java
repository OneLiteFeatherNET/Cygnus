package net.onelitefeather.cygnus.common.map.settings.adapter;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import net.onelitefeather.cygnus.common.map.settings.CreekSettings;

import java.lang.reflect.Type;

/**
 * Reads and writes the {@code settings} block of a map's {@code creek.json}. A key the block leaves
 * out takes its value from {@link CreekSettings#DEFAULT}.
 *
 * @author theEvilReaper
 * @version 1.0.0
 * @since 2.17.0
 */
public final class CreekSettingsAdapter extends SettingsAdapter<CreekSettings> {

    private static final String SIGHT_RANGE_KEY = "sightRange";
    private static final String STALK_MIN_DISTANCE_KEY = "stalkMinDistance";
    private static final String STALK_MAX_DISTANCE_KEY = "stalkMaxDistance";
    private static final String RESPAWN_MIN_DISTANCE_KEY = "respawnMinDistance";
    private static final String ROUTE_LINK_DISTANCE_KEY = "routeLinkDistance";
    private static final String PERSONAL_SPACE_KEY = "personalSpace";
    private static final String LAUNCH_HEIGHT_KEY = "launchHeight";

    @Override
    public JsonElement serialize(CreekSettings src, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject object = new JsonObject();
        object.addProperty(SIGHT_RANGE_KEY, src.sightRange());
        object.addProperty(STALK_MIN_DISTANCE_KEY, src.stalkMinDistance());
        object.addProperty(STALK_MAX_DISTANCE_KEY, src.stalkMaxDistance());
        object.addProperty(RESPAWN_MIN_DISTANCE_KEY, src.respawnMinDistance());
        object.addProperty(ROUTE_LINK_DISTANCE_KEY, src.routeLinkDistance());
        object.addProperty(PERSONAL_SPACE_KEY, src.personalSpace());
        object.addProperty(LAUNCH_HEIGHT_KEY, src.launchHeight());
        return object;
    }

    @Override
    public CreekSettings deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonObject block = block(json);
        CreekSettings defaults = CreekSettings.DEFAULT;
        return new CreekSettings(
                getInt(block, SIGHT_RANGE_KEY, defaults.sightRange()),
                getInt(block, STALK_MIN_DISTANCE_KEY, defaults.stalkMinDistance()),
                getInt(block, STALK_MAX_DISTANCE_KEY, defaults.stalkMaxDistance()),
                getInt(block, RESPAWN_MIN_DISTANCE_KEY, defaults.respawnMinDistance()),
                getDouble(block, ROUTE_LINK_DISTANCE_KEY, defaults.routeLinkDistance()),
                getInt(block, PERSONAL_SPACE_KEY, defaults.personalSpace()),
                getDouble(block, LAUNCH_HEIGHT_KEY, defaults.launchHeight()));
    }
}
