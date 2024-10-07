package net.onelitefeather.cygnus.common.util;

import com.google.gson.Gson;
import de.icevizion.aves.file.gson.PositionGsonAdapter;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.onelitefeather.cygnus.common.page.PageResource;
import net.onelitefeather.cygnus.common.page.adapter.PageResourceAdapter;

public class GsonUtil {

    public static final Gson GSON;

    static {
        PositionGsonAdapter typeAdapter = new PositionGsonAdapter();
        GSON = new Gson().newBuilder()
                .registerTypeAdapter(Pos.class, typeAdapter)
                .registerTypeAdapter(Vec.class, typeAdapter)
                .registerTypeAdapter(PageResource.class, new PageResourceAdapter())
                .create();
    }

    private GsonUtil() {
        throw new UnsupportedOperationException("This class cannot be instantiated");
    }
}
