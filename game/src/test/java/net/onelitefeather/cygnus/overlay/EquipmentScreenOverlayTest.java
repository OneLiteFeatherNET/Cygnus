package net.onelitefeather.cygnus.overlay;

import net.kyori.adventure.key.Key;
import net.minestom.server.component.DataComponents;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.component.Equippable;
import net.minestom.testing.Env;
import net.onelitefeather.cygnus.CygnusPlayerTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies that the overlay reaches the client as a camera overlay on the player's head, which is
 * the only way to have it scale with the screen rather than with a font size.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.7.0
 */
class EquipmentScreenOverlayTest extends CygnusPlayerTestBase {

    private static final Key TUNNEL = Key.key("cygnus", "gui/tunnel_vision/stage_7");
    private static final Key BLOOD = Key.key("cygnus", "gui/blood/left_1_1");

    private final EquipmentScreenOverlay overlay = new EquipmentScreenOverlay();

    @Test
    @DisplayName("A layer becomes a camera overlay on the player's head")
    void layerBecomesCameraOverlay(Env env) {
        Player player = spawn(env);

        this.overlay.set(player, OverlayLayer.TUNNEL_VISION, TUNNEL);

        assertEquals(TUNNEL.asString(), equippable(player).cameraOverlay());
    }

    @Test
    @DisplayName("The blood takes the screen while it is up")
    void bloodWinsOverTheTunnelVision(Env env) {
        Player player = spawn(env);
        this.overlay.set(player, OverlayLayer.TUNNEL_VISION, TUNNEL);

        this.overlay.set(player, OverlayLayer.BLOOD, BLOOD);

        assertEquals(BLOOD.asString(), equippable(player).cameraOverlay(),
                "only one camera overlay exists, and a hit is what matters most");
    }

    @Test
    @DisplayName("Once the blood is gone the tunnel vision comes back")
    void tunnelVisionReturnsAfterTheBlood(Env env) {
        Player player = spawn(env);
        this.overlay.set(player, OverlayLayer.TUNNEL_VISION, TUNNEL);
        this.overlay.set(player, OverlayLayer.BLOOD, BLOOD);

        this.overlay.set(player, OverlayLayer.BLOOD, null);

        assertEquals(TUNNEL.asString(), equippable(player).cameraOverlay());
    }

    @Test
    @DisplayName("The last layer leaving empties the head slot")
    void lastLayerEmptiesTheSlot(Env env) {
        Player player = spawn(env);
        this.overlay.set(player, OverlayLayer.TUNNEL_VISION, TUNNEL);

        this.overlay.set(player, OverlayLayer.TUNNEL_VISION, null);

        assertTrue(player.getHelmet().isAir(), "an item left behind would keep the overlay up");
    }

    @Test
    @DisplayName("Clearing empties the head slot and forgets the layers")
    void clearingEmptiesTheSlot(Env env) {
        Player player = spawn(env);
        this.overlay.set(player, OverlayLayer.TUNNEL_VISION, TUNNEL);
        this.overlay.set(player, OverlayLayer.BLOOD, BLOOD);

        this.overlay.clear(player);

        assertTrue(player.getHelmet().isAir());
    }

    @Test
    @DisplayName("The carrier item cannot be taken off or seen")
    void carrierItemStaysPutAndInvisible(Env env) {
        Player player = spawn(env);

        this.overlay.set(player, OverlayLayer.TUNNEL_VISION, TUNNEL);

        Equippable equippable = equippable(player);
        assertFalse(equippable.swappable(), "right-clicking must not strip the overlay");
        assertFalse(equippable.dispensable(), "a dispenser must not hand out overlays");
        assertFalse(equippable.damageOnHurt(), "the carrier is not armour");
        assertNotNull(equippable.assetId(), "without an asset the item is drawn on the player's head");
    }

    @Test
    @DisplayName("Setting the same layer twice does not churn the slot")
    void repeatedSetKeepsTheSameItem(Env env) {
        Player player = spawn(env);
        this.overlay.set(player, OverlayLayer.TUNNEL_VISION, TUNNEL);
        ItemStack first = player.getHelmet();

        this.overlay.set(player, OverlayLayer.TUNNEL_VISION, TUNNEL);

        assertEquals(first, player.getHelmet(), "an unchanged overlay must not be re-sent");
    }

    /**
     * Connects a player into a fresh instance.
     *
     * @param env the test environment
     * @return the connected player
     */
    private Player spawn(Env env) {
        Instance instance = env.createFlatInstance();
        return env.createConnection().connect(instance, new Pos(0, 40, 0));
    }

    /**
     * Reads the equippable component off the player's head slot.
     *
     * @param player the player to read
     * @return the component
     */
    private Equippable equippable(Player player) {
        Equippable equippable = player.getHelmet().get(DataComponents.EQUIPPABLE);
        assertNotNull(equippable, "nothing is carrying an overlay");
        return equippable;
    }
}
