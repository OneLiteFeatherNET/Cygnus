package net.onelitefeather.cygnus.common.page.adapter;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.icevizion.aves.file.gson.PositionGsonAdapter;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.utils.Direction;
import net.onelitefeather.cygnus.common.page.PageResource;
import net.onelitefeather.cygnus.common.util.DirectionFaceHelper;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class PageResourceAdapterTest {

    private static final String RESOURCE_AS_JSON = """
            {"face":"EAST","position":{"x":0.0,"y":0.0,"z":0.0,"yaw":0.0,"pitch":0.0}}
            """.trim();

    private static Gson gson;

    @BeforeAll
    static void setup() {
        PositionGsonAdapter positionGsonAdapter = new PositionGsonAdapter();
        gson = new GsonBuilder()
                .registerTypeAdapter(Pos.class, positionGsonAdapter)
                .registerTypeAdapter(Vec.class, positionGsonAdapter)
                .registerTypeAdapter(PageResource.class, new PageResourceAdapter())
                .create();
    }

    @AfterAll
    static void tearDown() {
        gson = null;
    }

    @Test
    void testPageToJson() {
        PageResource pageResource = new PageResource(Pos.ZERO, Direction.EAST);
        assertEquals(Pos.ZERO, pageResource.position());
        assertNotEquals(Direction.WEST, pageResource.face());

        String pageAsJson = gson.toJson(pageResource, PageResource.class);

        assertNotNull(pageAsJson);

        assertEquals(RESOURCE_AS_JSON, pageAsJson);
    }

    @Test
    void testJsonToPageResource() {
        PageResource pageResource = gson.fromJson(RESOURCE_AS_JSON, PageResource.class);

        assertNotNull(pageResource);
        assertEquals(Pos.ZERO, pageResource.position());
        assertEquals(Direction.EAST, pageResource.face());
    }
}