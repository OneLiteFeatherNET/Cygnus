package net.onelitefeather.cygnus.common.util;

import com.google.gson.Gson;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.onelitefeather.cygnus.common.page.PageResource;
import net.onelitefeather.cygnus.common.page.adapter.PageResourceAdapter;
import net.theevilreaper.aves.file.GsonFileHandler;
import net.theevilreaper.aves.file.gson.PositionGsonAdapter;

/**
 * Utility class to handle gson related code
 *
 * @author theEvilReaper
 * @version 1.0.0
 * @since 2.1.0
 */
public final class GsonHelper {

    public static final Gson GSON;
    public static final GsonFileHandler FILE_HANDLER;

    static {

        var typeAdapter = new PositionGsonAdapter();
        GSON = new Gson().newBuilder()
                .registerTypeAdapter(Pos.class, typeAdapter)
                .registerTypeAdapter(Vec.class, typeAdapter)
                .registerTypeAdapter(PageResource.class, new PageResourceAdapter())
                .create();
        FILE_HANDLER = new GsonFileHandler(GSON);
    }

    private GsonHelper() {
        // Nothing to do here
    }
}
