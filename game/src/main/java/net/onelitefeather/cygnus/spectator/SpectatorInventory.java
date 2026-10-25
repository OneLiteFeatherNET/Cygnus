package net.onelitefeather.cygnus.spectator;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.minestom.server.MinecraftServer;
import net.minestom.server.component.DataComponents;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventDispatcher;
import net.minestom.server.inventory.Inventory;
import net.minestom.server.inventory.InventoryType;
import net.minestom.server.inventory.click.Click;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.network.player.GameProfile;
import net.minestom.server.network.player.ResolvableProfile;
import net.minestom.server.tag.Tag;
import net.onelitefeather.cygnus.player.event.SpectatorStatsRequestEvent;
import net.onelitefeather.cygnus.utils.HealthDisplay;
import net.theevilreaper.aves.inventory.GlobalInventoryBuilder;
import net.theevilreaper.aves.inventory.click.ClickHolder;
import net.theevilreaper.aves.inventory.layout.InventoryLayout;
import net.theevilreaper.aves.inventory.util.LayoutCalculator;
import net.theevilreaper.xerus.api.team.Team;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Global "Spectate" inventory which lists every survivor and every spectator as a player head.
 * <p>
 * The survivors fill the upper part and show the hearts they have left. A left click on one of them
 * teleports the viewer to that survivor, a right click on any head asks for the statistics of that player
 * through a {@link SpectatorStatsRequestEvent}.
 * The spectators fill the lower part: they are out of the round, so there is nothing to teleport to.
 * </p>
 *
 * @author OneLiteFeather
 * @version 1.1.0
 * @since 1.0.0
 */
public class SpectatorInventory extends GlobalInventoryBuilder {

    private static final ItemStack DECORATION_PANE = ItemStack.builder(Material.BLACK_STAINED_GLASS_PANE)
            .customName(Component.empty())
            .build();
    static final Tag<UUID> TARGET_TAG = Tag.UUID("target");

    private static final Component TELEPORT_HINT = hint("Left click to teleport");
    private static final Component STATS_HINT = hint("Right click for statistics");
    private static final Component OUT_OF_ROUND = hint("Out of the round");

    /** The two rows below the top decoration, one head per survivor. */
    static final int[] SURVIVOR_SLOTS = LayoutCalculator
            .quad(InventoryType.CHEST_1_ROW.getSize(), InventoryType.CHEST_3_ROW.getSize() - 1);

    /** The row between the survivors and the spectators, kept as a decoration to split the two. */
    private static final int[] SEPARATOR_SLOTS = LayoutCalculator
            .quad(InventoryType.CHEST_3_ROW.getSize(), InventoryType.CHEST_4_ROW.getSize() - 1);

    /** The row above the bottom decoration, one head per spectator. */
    static final int[] SPECTATOR_SLOTS = LayoutCalculator
            .quad(InventoryType.CHEST_4_ROW.getSize(), InventoryType.CHEST_5_ROW.getSize() - 1);

    private final Team survivorTeam;
    private final BiConsumer<Player, Player> teleportCallback;
    private volatile int populatedSurvivorSlots;
    private volatile int populatedSpectatorSlots;
    private Map<UUID, Float> shownHealth = Map.of();

    /**
     * Creates the inventory for the given teams.
     *
     * @param survivorTeam     the team whose players can be teleported to
     * @param spectatorTeam    the team whose players are listed as out of the round
     * @param teleportCallback called with the viewer and the survivor they want to teleport to
     */
    public SpectatorInventory(Team survivorTeam, Team spectatorTeam, BiConsumer<Player, Player> teleportCallback) {
        super(Component.text("Spectate"), InventoryType.CHEST_6_ROW);
        this.survivorTeam = survivorTeam;
        this.teleportCallback = teleportCallback;

        InventoryLayout layout = InventoryLayout.fromType(getType());
        layout.setItems(LayoutCalculator.fillRow(InventoryType.CHEST_1_ROW), DECORATION_PANE);
        layout.setItems(SEPARATOR_SLOTS, DECORATION_PANE);
        layout.setItems(LayoutCalculator.fillRow(getType()), DECORATION_PANE);

        this.setLayout(layout);

        this.setDataLayoutFunction(dataLFunction -> {
            InventoryLayout dataLayout = dataLFunction == null ? InventoryLayout.fromType(getType()) : dataLFunction;

            dataLayout.blank(SURVIVOR_SLOTS);
            dataLayout.blank(SPECTATOR_SLOTS);
            Collection<Player> survivors = survivorTeam.getPlayers();
            this.shownHealth = healthOf(survivors);
            this.populatedSurvivorSlots = fill(dataLayout, SURVIVOR_SLOTS, survivors, true);
            this.populatedSpectatorSlots = fill(dataLayout, SPECTATOR_SLOTS, spectatorTeam.getPlayers(), false);

            return dataLayout;
        });

        this.register();
    }

    /**
     * Puts one head per player into the given slots, as many as fit.
     *
     * @param layout  the layout to fill
     * @param slots   the slots to fill in order
     * @param players the players to show
     * @param alive   whether the players are survivors, who show their hearts and can be teleported to
     * @return how many slots got a head
     */
    private int fill(InventoryLayout layout, int[] slots, Collection<Player> players, boolean alive) {
        int index = 0;
        for (Player player : players) {
            if (index >= slots.length) break;
            layout.setItem(slots[index], head(player, alive), this::handleClick);
            index++;
        }
        return index;
    }

    /**
     * Builds the head of a player.
     *
     * @param player the player to show
     * @param alive  whether the player is a survivor
     * @return the head
     */
    private static ItemStack head(Player player, boolean alive) {
        ResolvableProfile profile = player.getSkin() != null
                ? new ResolvableProfile(player.getSkin())
                : new ResolvableProfile(new GameProfile(player.getUuid(), player.getUsername()));
        List<Component> lore = alive
                ? List.of(Component.empty(), HealthDisplay.hearts(player), Component.empty(), TELEPORT_HINT, STATS_HINT, Component.empty())
                : List.of(Component.empty(), OUT_OF_ROUND, Component.empty(), STATS_HINT, Component.empty());
        return ItemStack.builder(Material.PLAYER_HEAD)
                .customName(Component.text(player.getUsername()))
                .set(DataComponents.PROFILE, profile)
                .set(TARGET_TAG, player.getUuid())
                .lore(lore)
                .build();
    }

    private static Component hint(String text) {
        return Component.text(text, NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false);
    }

    /**
     * Reads the health of every survivor.
     *
     * @param survivors the survivors
     * @return the health per survivor
     */
    private static Map<UUID, Float> healthOf(Collection<Player> survivors) {
        Map<UUID, Float> health = HashMap.newHashMap(survivors.size());
        for (Player survivor : survivors) {
            health.put(survivor.getUuid(), survivor.getHealth());
        }
        return health;
    }

    /**
     * Rebuilds the heads once the health of a survivor changed since they were built.
     * <p>
     * Nothing is rebuilt while nobody looks at the inventory, and nothing while the hearts are the
     * same, so a quiet round sends no packets at all.
     * </p>
     *
     * @return {@code false} once nobody has the inventory open any more
     */
    boolean refreshHealth() {
        if (!isOpen()) return false;
        if (!healthOf(this.survivorTeam.getPlayers()).equals(this.shownHealth)) {
            invalidateDataLayout();
        }
        return true;
    }

    /**
     * Contains the logic what should happen when a player clicks on a player.
     * <p>
     * A right click asks for the statistics of the clicked player. Any other click teleports the viewer,
     * but only to a survivor: a spectator is out of the round and flies wherever they like.
     * </p>
     *
     * @param player who is involved in the click
     * @param slot   of the click
     * @param click  reference
     * @param stack  which is clicked
     * @param holder the result of the click
     */
    void handleClick(Player player, int slot, Click click, ItemStack stack, Consumer<ClickHolder> holder) {
        holder.accept(ClickHolder.cancelClick());

        UUID target = stack.getTag(TARGET_TAG);

        if (target == null) {
            return;
        }

        Player targetPlayer = MinecraftServer.getConnectionManager().getOnlinePlayerByUuid(target);

        if (targetPlayer == null) {

            return;
        }

        if (click instanceof Click.Right || click instanceof Click.RightShift) {
            EventDispatcher.call(new SpectatorStatsRequestEvent(player, targetPlayer));
            return;
        }

        if (!survivorTeam.getPlayers().contains(targetPlayer)) {
            return;
        }

        player.closeInventory();
        teleportCallback.accept(player, targetPlayer);
    }

    /**
     * Applies the data layout and clears every head slot beyond the current survivor and spectator count.
     * The framework only writes non-empty stacks, so a shrinking list would otherwise leave a stale head behind.
     */
    @Override
    protected void applyDataLayout() {
        super.applyDataLayout();
        Inventory inventory = getInventory();
        for (int i = populatedSurvivorSlots; i < SURVIVOR_SLOTS.length; i++) {
            inventory.setItemStack(SURVIVOR_SLOTS[i], ItemStack.AIR);
        }
        for (int i = populatedSpectatorSlots; i < SPECTATOR_SLOTS.length; i++) {
            inventory.setItemStack(SPECTATOR_SLOTS[i], ItemStack.AIR);
        }
    }

    /**
     * Opens the inventory for a specific {@link Player}.
     *
     * @param player who should get it
     */
    public void open(Player player) {
        player.openInventory(getInventory());
    }
}
