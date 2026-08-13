package net.onelitefeather.cygnus.overlay;

import net.kyori.adventure.key.Key;
import net.minestom.server.component.DataComponents;
import net.minestom.server.entity.EquipmentSlot;
import net.minestom.server.entity.Player;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.item.component.Equippable;
import net.minestom.server.sound.SoundEvent;
import net.onelitefeather.cygnus.common.util.PlayerState;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.Map;

/**
 * Puts the overlay on screen as the {@code camera_overlay} of an item worn on the head.
 * <p>
 * This is the one mechanism in vanilla that draws a texture across the whole screen and scales it
 * with the viewport — the same one the carved pumpkin uses. A font glyph cannot do that: its size
 * is fixed in the pack, so it has to be calibrated against a resolution and drifts on every other.
 * </p>
 * <p>
 * A player has one head, so only one layer can be shown at a time. The topmost one wins, which
 * means a splatter of blood takes the screen for as long as it lasts and the tunnel vision comes
 * back underneath it afterwards.
 * </p>
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.7.0
 */
public final class EquipmentScreenOverlay implements ScreenOverlay {

    /**
     * What the overlay rides on. The item itself is never seen — {@link #EMPTY_ASSET} makes sure of
     * that — so the material only has to exist.
     */
    private static final Material CARRIER = Material.PAPER;

    /**
     * An equipment model with no layers, from the resource pack. Without an asset id Minecraft
     * falls back to drawing the item itself on the player's head.
     */
    private static final String EMPTY_ASSET = "cygnus:empty";

    /** Vanilla's silent sound; the default equip sound would click on every stage change. */
    private static final SoundEvent SILENT = SoundEvent.of(Key.key("minecraft:intentionally_empty"), null);

    private final PlayerState<Map<OverlayLayer, Key>> layers = new PlayerState<>();
    private final PlayerState<Key> shown = new PlayerState<>();

    /**
     * {@inheritDoc}
     */
    @Override
    public void set(Player player, OverlayLayer layer, @Nullable Key texture) {
        Map<OverlayLayer, Key> current = this.layers
                .computeIfAbsent(player, () -> new EnumMap<>(OverlayLayer.class));

        if (texture == null) {
            current.remove(layer);
        } else {
            current.put(layer, texture);
        }

        this.apply(player, current);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void clear(Player player) {
        this.layers.remove(player);
        this.shown.remove(player);
        player.setHelmet(ItemStack.AIR);
    }

    /**
     * Works out which layer is on top and puts it on the player's head.
     *
     * @param player  the player to draw for
     * @param current the layers currently set for them
     */
    private void apply(Player player, Map<OverlayLayer, Key> current) {
        Key topmost = this.topmost(current);

        if (topmost == null) {
            if (this.shown.remove(player) == null) return;
            player.setHelmet(ItemStack.AIR);
            return;
        }

        // The overlay is refreshed many times a second; re-sending an unchanged item would put an
        // equipment update on the wire for every viewer each time.
        if (topmost.equals(this.shown.get(player))) return;

        this.shown.put(player, topmost);
        player.setHelmet(carrierFor(topmost));
    }

    /**
     * Picks the layer that is drawn on top of the others.
     *
     * @param current the layers currently set
     * @return the texture to show, or {@code null} if nothing is set
     */
    private @Nullable Key topmost(Map<OverlayLayer, Key> current) {
        Key topmost = null;
        // Declaration order of OverlayLayer is drawing order, so the last hit wins.
        for (OverlayLayer layer : OverlayLayer.values()) {
            Key texture = current.get(layer);
            if (texture != null) topmost = texture;
        }
        return topmost;
    }

    /**
     * Builds the item that carries a given overlay texture.
     *
     * @param texture the texture to show
     * @return the item to put in the head slot
     */
    private static ItemStack carrierFor(Key texture) {
        Equippable equippable = new Equippable(
                EquipmentSlot.HELMET,
                SILENT,
                EMPTY_ASSET,
                texture.asString(),
                null,
                false,
                false,
                false,
                false,
                false,
                SILENT
        );
        return ItemStack.builder(CARRIER).set(DataComponents.EQUIPPABLE, equippable).build();
    }
}
