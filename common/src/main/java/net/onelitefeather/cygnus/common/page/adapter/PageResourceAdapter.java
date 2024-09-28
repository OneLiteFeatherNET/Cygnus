package net.onelitefeather.cygnus.common.page.adapter;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Vec;
import net.onelitefeather.cygnus.common.page.PageResource;

import java.lang.reflect.Type;

/**
 * @author theEvilReaper
 * @version 1.0.0
 * @since 1.0.0
 **/
public final class PageResourceAdapter implements JsonSerializer<PageResource>, JsonDeserializer<PageResource> {

    private static final String POSITION_KEY = "position";
    private static final String FACE_KEY = "face";

    @Override
    public JsonElement serialize(PageResource src, Type typeOfSrc, JsonSerializationContext context) {
        if (src == null) return null;
        JsonObject object = new JsonObject();
        object.addProperty(FACE_KEY, src.face());
        object.add(POSITION_KEY, context.serialize(src.position(), Vec.class));
        return object;
    }

    @Override
    public PageResource deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        String face = "north";
        Point position = null;

        JsonObject object = json.getAsJsonObject();

        if (object.has(FACE_KEY)) {
            face = object.get(FACE_KEY).getAsString();
        }

        if (!object.has(POSITION_KEY)) {
            throw new IllegalStateException("The position can't be null");
        }

        position = context.deserialize(object.get(POSITION_KEY), Vec.class);

        return new PageResource(position, face);
    }
}
