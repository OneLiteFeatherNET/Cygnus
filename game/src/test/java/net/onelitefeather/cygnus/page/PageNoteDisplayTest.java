package net.onelitefeather.cygnus.page;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.metadata.display.AbstractDisplayMeta;
import net.minestom.server.entity.metadata.display.TextDisplayMeta;
import net.minestom.server.instance.Instance;
import net.minestom.server.utils.Direction;
import net.minestom.testing.Env;
import net.minestom.testing.extension.MicrotusExtension;
import net.onelitefeather.cygnus.common.page.PageEntity;
import net.onelitefeather.cygnus.common.page.PageFactory;
import net.onelitefeather.cygnus.common.page.PageNote;
import net.onelitefeather.cygnus.common.page.PageResource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MicrotusExtension.class)
class PageNoteDisplayTest {

    private static PageEntity placePage(Instance instance) {
        PageEntity page = PageFactory.createPage(new PageResource(new Pos(0, 40, 2), Direction.NORTH), 1);
        page.place(instance).join();
        return page;
    }

    @Test
    void spawnsTextDisplayVisibleOnlyToTheReader(Env env) {
        Instance instance = env.createFlatInstance();
        Player reader = env.createPlayer(instance, new Pos(0, 40, 0));
        Player other = env.createPlayer(instance, new Pos(1, 40, 0));
        PageEntity page = placePage(instance);

        PageNoteDisplay display = PageNoteDisplay.spawn(reader, page, PageNote.HELP_ME.getWorldComponent());

        assertEquals(EntityType.TEXT_DISPLAY, display.entity().getEntityType());
        assertTrue(display.entity().getViewers().contains(reader), "the reader must see the note");
        assertFalse(display.entity().getViewers().contains(other), "nobody else may see the note");
        assertEquals(page.getUuid(), display.pageId());
        assertEquals(page.getPosition(), display.pagePosition());
    }

    @Test
    void appliesTheAgreedMetadata(Env env) {
        Instance instance = env.createFlatInstance();
        Player reader = env.createPlayer(instance, new Pos(0, 40, 0));
        PageEntity page = placePage(instance);

        PageNoteDisplay display = PageNoteDisplay.spawn(reader, page, PageNote.HELP_ME.getWorldComponent());
        TextDisplayMeta meta = (TextDisplayMeta) display.entity().getEntityMeta();

        assertEquals(PageNote.HELP_ME.getWorldComponent(), meta.getText());
        assertEquals(0, meta.getBackgroundColor());
        assertFalse(meta.isShadow());
        assertFalse(meta.isSeeThrough());
        assertEquals(PageNoteDisplay.LINE_WIDTH, meta.getLineWidth());
        assertEquals(AbstractDisplayMeta.BillboardConstraints.CENTER, meta.getBillboardRenderConstraints());
        assertEquals(Vec.ZERO, meta.getScale(), "a note spawns collapsed and grows in on the next tick");
    }

    /**
     * A page's direction is the way the player looked when it was set, so the page sits on the
     * opposite face of its block: a NORTH page hangs on the block's +Z side and is read from +Z.
     */
    @ParameterizedTest
    @CsvSource({"NORTH, 0, 1", "SOUTH, 0, -1", "EAST, -1, 0", "WEST, 1, 0"})
    void floatsInFrontOfAndAboveThePage(Direction direction, int frontX, int frontZ, Env env) {
        Instance instance = env.createFlatInstance();
        Player reader = env.createPlayer(instance, new Pos(0, 40, 0));
        PageEntity page = PageFactory.createPage(new PageResource(new Pos(0, 40, 2), direction), 1);
        page.place(instance).join();

        PageNoteDisplay display = PageNoteDisplay.spawn(reader, page, PageNote.HELP_ME.getWorldComponent());

        Pos expected = page.getPosition().add(
                frontX * PageNoteDisplay.FACE_OFFSET,
                PageNoteDisplay.LIFT,
                frontZ * PageNoteDisplay.FACE_OFFSET
        );
        assertTrue(expected.samePoint(display.entity().getPosition()),
                "expected " + expected + " but was " + display.entity().getPosition());
    }

    @Test
    void growsToNoteScaleOnTheNextTick(Env env) {
        Instance instance = env.createFlatInstance();
        Player reader = env.createPlayer(instance, new Pos(0, 40, 0));
        PageEntity page = placePage(instance);

        PageNoteDisplay display = PageNoteDisplay.spawn(reader, page, PageNote.HELP_ME.getWorldComponent());
        TextDisplayMeta meta = (TextDisplayMeta) display.entity().getEntityMeta();
        env.tick();

        assertEquals(new Vec(PageNoteDisplay.NOTE_SCALE), meta.getScale());
        assertEquals(PageNoteDisplay.SHOW_TICKS, meta.getTransformationInterpolationDuration());
    }

    @Test
    void hideShrinksThenRemoves(Env env) {
        Instance instance = env.createFlatInstance();
        Player reader = env.createPlayer(instance, new Pos(0, 40, 0));
        PageEntity page = placePage(instance);
        PageNoteDisplay display = PageNoteDisplay.spawn(reader, page, PageNote.HELP_ME.getWorldComponent());
        env.tick();

        display.hide();
        TextDisplayMeta meta = (TextDisplayMeta) display.entity().getEntityMeta();
        assertEquals(Vec.ZERO, meta.getScale());
        assertEquals(PageNoteDisplay.HIDE_TICKS, meta.getTransformationInterpolationDuration());
        assertFalse(display.entity().isRemoved(), "the fade-out must play before removal");

        env.tickWhile(() -> !display.entity().isRemoved(), Duration.ofSeconds(2));
        assertTrue(display.entity().isRemoved());
    }

    @Test
    void removeNowIsImmediateAndIdempotent(Env env) {
        Instance instance = env.createFlatInstance();
        Player reader = env.createPlayer(instance, new Pos(0, 40, 0));
        PageEntity page = placePage(instance);
        PageNoteDisplay display = PageNoteDisplay.spawn(reader, page, PageNote.HELP_ME.getWorldComponent());

        display.hide();
        display.removeNow();
        display.removeNow();
        display.hide();

        assertTrue(display.entity().isRemoved());
        env.tick();
        env.tick();
        env.tick();
        env.tick();
        assertTrue(display.entity().isRemoved());
    }
}
