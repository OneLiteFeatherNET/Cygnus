package net.onelitefeather.cygnus.page;

import net.kyori.adventure.text.Component;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.metadata.display.AbstractDisplayMeta;
import net.minestom.server.entity.metadata.display.TextDisplayMeta;
import net.minestom.server.timer.TaskSchedule;
import net.minestom.server.utils.Direction;
import net.onelitefeather.cygnus.common.page.PageEntity;

import java.util.UUID;

/**
 * One handwritten note floating at one page, shown to exactly one player.
 *
 * <p>The note is a text display rather than a title: it stays where the page is, does not share the
 * title slot with anything else, and display entities have no hit box, so clicks still reach the
 * page's interaction entity right behind it.</p>
 *
 * <p>It fades by scale, since the client interpolates a display's transformation but not its text
 * opacity.</p>
 *
 * @author theEvilReaper
 * @version 1.0.0
 * @since 2.15.0
 */
public final class PageNoteDisplay {

    static final float NOTE_SCALE = 0.35f;
    static final int SHOW_TICKS = 4;
    static final int HIDE_TICKS = 3;
    static final double FACE_OFFSET = 0.15;
    static final double LIFT = 0.45;
    static final int LINE_WIDTH = 1000;
    static final int NOTE_LIGHT = 15;

    private final Entity entity;
    private final UUID pageId;
    private final Pos pagePosition;
    private boolean hiding;

    private PageNoteDisplay(Entity entity, UUID pageId, Pos pagePosition) {
        this.entity = entity;
        this.pageId = pageId;
        this.pagePosition = pagePosition;
    }

    /**
     * Spawns the note at the given page for the given player and starts growing it in.
     *
     * @param viewer the only player who will see the note
     * @param page   the page the note belongs to
     * @param note   the note box, see {@code PageNote#getWorldComponent()}
     * @return the spawned note
     */
    public static PageNoteDisplay spawn(Player viewer, PageEntity page, Component note) {
        Entity entity = new Entity(EntityType.TEXT_DISPLAY);
        entity.setNoGravity(true);
        entity.setAutoViewable(false);

        TextDisplayMeta meta = (TextDisplayMeta) entity.getEntityMeta();
        meta.setText(note);
        meta.setBackgroundColor(0);
        meta.setShadow(false);
        meta.setSeeThrough(false);
        meta.setLineWidth(LINE_WIDTH);
        meta.setBillboardRenderConstraints(AbstractDisplayMeta.BillboardConstraints.CENTER);
        meta.setBrightness(NOTE_LIGHT, NOTE_LIGHT);
        meta.setScale(Vec.ZERO);

        Pos pagePosition = page.getPosition();
        // A page's direction is the way the player looked when it was set, so it hangs on the
        // opposite face of its block: the side it is read from lies against that direction.
        Direction look = page.getResource().face();
        Pos position = pagePosition.add(
                -look.normalX() * FACE_OFFSET,
                LIFT,
                -look.normalZ() * FACE_OFFSET
        );
        entity.setInstance(page.getInstance(), position).join();
        entity.addViewer(viewer);

        // A tick between spawn and target scale: set in the same tick, the client would take the
        // target without interpolating towards it.
        entity.scheduleNextTick(ignored -> {
            meta.setTransformationInterpolationStartDelta(0);
            meta.setTransformationInterpolationDuration(SHOW_TICKS);
            meta.setScale(new Vec(NOTE_SCALE));
        });
        return new PageNoteDisplay(entity, page.getUuid(), pagePosition);
    }

    /**
     * Shrinks the note away and removes it once the shrink has played.
     */
    public void hide() {
        if (this.hiding || this.entity.isRemoved()) {
            return;
        }
        this.hiding = true;
        TextDisplayMeta meta = (TextDisplayMeta) this.entity.getEntityMeta();
        meta.setTransformationInterpolationStartDelta(0);
        meta.setTransformationInterpolationDuration(HIDE_TICKS);
        meta.setScale(Vec.ZERO);
        this.entity.scheduler().buildTask(this::removeNow).delay(TaskSchedule.tick(HIDE_TICKS)).schedule();
    }

    /**
     * Removes the note at once, without any animation.
     */
    public void removeNow() {
        if (!this.entity.isRemoved()) {
            this.entity.remove();
        }
    }

    /**
     * Returns the id of the page this note belongs to.
     *
     * @return the page id
     */
    public UUID pageId() {
        return this.pageId;
    }

    /**
     * Returns where the page stood when the note was spawned.
     *
     * @return the page position at spawn time
     */
    public Pos pagePosition() {
        return this.pagePosition;
    }

    Entity entity() {
        return this.entity;
    }
}
