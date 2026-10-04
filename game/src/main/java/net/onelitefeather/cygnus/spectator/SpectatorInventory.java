package net.onelitefeather.cygnus.spectator;

import net.kyori.adventure.text.Component;
import net.minestom.server.MinecraftServer;
import net.minestom.server.component.DataComponents;
import net.minestom.server.entity.Player;
import net.minestom.server.inventory.Inventory;
import net.minestom.server.inventory.InventoryType;
import net.minestom.server.inventory.click.Click;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.network.player.GameProfile;
import net.minestom.server.network.player.ResolvableProfile;
import net.minestom.server.tag.Tag;
import net.theevilreaper.aves.inventory.GlobalInventoryBuilder;
import net.theevilreaper.aves.inventory.click.ClickHolder;
import net.theevilreaper.aves.inventory.layout.InventoryLayout;
import net.theevilreaper.aves.inventory.util.LayoutCalculator;
import net.theevilreaper.xerus.api.team.Team;

import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Global "Spectate" inventory which lists every survivor as a clickable player head.
 *
 * @author OneLiteFeather
 * @version 1.0.1
 * @since 1.0.0
 */
public class SpectatorInventory extends GlobalInventoryBuilder {

    private static final ItemStack DECORATION_PANE = ItemStack.builder(Material.BLACK_STAINED_GLASS_PANE)
            .customName(Component.empty())
            .build();
    static final Tag<UUID> TARGET_TAG = Tag.UUID("target");

    private static final List<Component> LORE_LINES = List.of(
            Component.empty(),
            Component.text("Click to spectate"),
            Component.empty()
    );

    private static final int[] SLOTS = LayoutCalculator
            .quad(InventoryType.CHEST_1_ROW.getSize(), InventoryType.CHEST_3_ROW.getSize() - 1);

    private final BiConsumer<Player, Player> teleportCallback;
    private volatile int populatedSlots;

    /**
     * Creates a new instance from the builder with the given parameter values.
     *
     */
    public SpectatorInventory(Team survivorTeam, BiConsumer<Player, Player> teleportCallback) {
        super(Component.text("Spectate"), InventoryType.CHEST_4_ROW);
        this.teleportCallback = teleportCallback;

        InventoryLayout layout = InventoryLayout.fromType(getType());
        layout.setItems(LayoutCalculator.fillRow(InventoryType.CHEST_1_ROW), DECORATION_PANE);
        layout.setItems(LayoutCalculator.fillRow(getType()), DECORATION_PANE);

        this.setLayout(layout);

        this.setDataLayoutFunction(dataLFunction -> {
            InventoryLayout dataLayout = dataLFunction == null ? InventoryLayout.fromType(getType()) : dataLFunction;

            dataLayout.blank(SLOTS);
            Iterator<Player> iterator = survivorTeam.getPlayers().iterator();
            int index = 0;

            while (index < SLOTS.length && iterator.hasNext()) {
                Player player = iterator.next();
                ResolvableProfile profile = player.getSkin() != null
                        ? new ResolvableProfile(player.getSkin())
                        : new ResolvableProfile(new GameProfile(player.getUuid(), player.getUsername()));
                dataLayout.setItem(
                        SLOTS[index],
                        ItemStack.builder(Material.PLAYER_HEAD)
                                .customName(Component.text(player.getUsername()))
                                .set(DataComponents.PROFILE, profile)
                                .set(TARGET_TAG, player.getUuid())
                                .lore(LORE_LINES)
                                .build(),
                        this::handleClick
                );

                index++;
            }
            this.populatedSlots = index;

            return dataLayout;
        });

        this.register();
    }

    /**
     * Contains the logic what should happen when a player clicks on a player.
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

        player.closeInventory();
        teleportCallback.accept(player, targetPlayer);
    }

    /**
     * Applies the data layout and clears every head slot beyond the current survivor count.
     * The framework only writes non-empty stacks, so a shrinking list would otherwise leave a stale head behind.
     */
    @Override
    protected void applyDataLayout() {
        super.applyDataLayout();
        Inventory inventory = getInventory();
        for (int i = populatedSlots; i < SLOTS.length; i++) {
            inventory.setItemStack(SLOTS[i], ItemStack.AIR);
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
