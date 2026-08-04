package net.onelitefeather.cygnus.spectator;

import net.kyori.adventure.text.Component;
import net.minestom.server.MinecraftServer;
import net.minestom.server.component.DataComponents;
import net.minestom.server.entity.Player;
import net.minestom.server.inventory.InventoryType;
import net.minestom.server.inventory.click.Click;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.network.player.ResolvableProfile;
import net.minestom.server.tag.Tag;
import net.theevilreaper.aves.inventory.GlobalInventoryBuilder;
import net.theevilreaper.aves.inventory.click.ClickHolder;
import net.theevilreaper.aves.inventory.layout.InventoryLayout;
import net.theevilreaper.aves.inventory.util.LayoutCalculator;
import net.theevilreaper.xerus.api.team.Team;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

public class SpectatorInventory extends GlobalInventoryBuilder {

    private static final ItemStack DECORATION_PANE = ItemStack.builder(Material.BLACK_STAINED_GLASS_PANE)
            .customName(Component.empty())
            .build();
    private static final Tag<UUID> TARGET_TAG = Tag.UUID("target");

    private static final List<Component> LORE_LINES = List.of(
            Component.empty(),
            Component.text("Click to spectate"),
            Component.empty()
    );

    /**
     * Creates a new instance from the builder with the given parameter values.
     *
     */
    public SpectatorInventory(Team survivorTeam) {
        super(Component.text("Spectate"), InventoryType.CHEST_4_ROW);

        InventoryLayout layout = InventoryLayout.fromType(getType());
        layout.setItems(LayoutCalculator.fillRow(InventoryType.CHEST_1_ROW), DECORATION_PANE);
        layout.setItems(LayoutCalculator.fillRow(getType()), DECORATION_PANE);

        this.setLayout(layout);

        this.setDataLayoutFunction(dataLFunction -> {
            InventoryLayout dataLayout = dataLFunction == null ? InventoryLayout.fromType(getType()) : dataLFunction;

            int startSlot = InventoryType.CHEST_1_ROW.getSize();
            int endSlot = InventoryType.CHEST_3_ROW.getSize() - 1;
            Iterator<Player> iterator = survivorTeam.getPlayers().iterator();

            while (startSlot < endSlot && iterator.hasNext()) {
                Player player = iterator.next();
                dataLayout.setItem(
                        startSlot,
                        ItemStack.builder(Material.PLAYER_HEAD)
                                .set(DataComponents.PROFILE, new ResolvableProfile(player.getSkin()))
                                .set(TARGET_TAG, player.getUuid())
                                .lore(LORE_LINES)
                                .build(),
                        this::handleClick
                );

                startSlot++;
            }

            return dataLayout;
        });

        this.register();
    }

    private void handleClick(Player player, int i, Click click, ItemStack stack, Consumer<ClickHolder> holder) {
        holder.accept(ClickHolder.cancelClick());

        UUID target = stack.getTag(TARGET_TAG);

        Player targetPlayer = MinecraftServer.getConnectionManager().getOnlinePlayerByUuid(target);

        if (targetPlayer == null) {

            return;
        }

        player.closeInventory();
        player.teleport(player.getPosition());
    }
}
