package net.onelitefeather.cygnus.common.map.settings.adapter;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import net.onelitefeather.cygnus.common.map.settings.PageSettings;

import java.lang.reflect.Type;

/**
 * Reads and writes the {@code settings} block of a map's {@code pages.json}. A key the block leaves
 * out takes its value from {@link PageSettings#DEFAULT}, and a value equal to it is not written.
 *
 * @author theEvilReaper
 * @version 1.0.0
 * @since 2.17.0
 */
public final class PageSettingsAdapter extends SettingsAdapter<PageSettings> {

    private static final String PROXIMITY_RANGE_KEY = "proximityRange";
    private static final String MIN_ACTIVE_PAGES_KEY = "minActivePages";
    private static final String TTL_SECONDS_KEY = "ttlSeconds";
    private static final String RESPAWN_DELAY_SECONDS_KEY = "respawnDelaySeconds";

    @Override
    public JsonElement serialize(PageSettings src, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject block = new JsonObject();
        PageSettings defaults = PageSettings.DEFAULT;
        putInt(block, PROXIMITY_RANGE_KEY, src.proximityRange(), defaults.proximityRange());
        putInt(block, MIN_ACTIVE_PAGES_KEY, src.minActivePages(), defaults.minActivePages());
        putInt(block, TTL_SECONDS_KEY, src.ttlSeconds(), defaults.ttlSeconds());
        putInt(block, RESPAWN_DELAY_SECONDS_KEY, src.respawnDelaySeconds(), defaults.respawnDelaySeconds());
        return block;
    }

    @Override
    public PageSettings deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonObject block = block(json);
        PageSettings defaults = PageSettings.DEFAULT;
        return new PageSettings(
                getInt(block, PROXIMITY_RANGE_KEY, defaults.proximityRange()),
                getInt(block, MIN_ACTIVE_PAGES_KEY, defaults.minActivePages()),
                getInt(block, TTL_SECONDS_KEY, defaults.ttlSeconds()),
                getInt(block, RESPAWN_DELAY_SECONDS_KEY, defaults.respawnDelaySeconds()));
    }
}
