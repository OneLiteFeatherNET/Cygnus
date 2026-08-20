package net.onelitefeather.cygnus.common.block;

import net.minestom.server.MinecraftServer;
import net.minestom.server.instance.block.BlockHandler;
import net.minestom.server.instance.block.BlockManager;

/**
 * Utility class for registering standard {@link BlockHandler}s.
 */
public final class BlockHandlers {

    private BlockHandlers() {
    }

    /**
     * Registers default BlockHandlers on the global {@link BlockManager}.
     */
    public static void registerAll() {
        registerAll(MinecraftServer.getBlockManager());
    }

    /**
     * Registers default BlockHandlers on the specified {@link BlockManager}.
     *
     * @param blockManager the block manager to register handlers on
     */
    public static void registerAll(BlockManager blockManager) {
        register(blockManager, new SignBlockHandler());
        register(blockManager, new HangingSignBlockHandler());
        register(blockManager, new ShelfBlockHandler());
        register(blockManager, new ChiseledBookshelfBlockHandler());
        register(blockManager, new BannerBlockHandler());
        register(blockManager, new LecternBlockHandler());
        register(blockManager, new CampfireBlockHandler());
        register(blockManager, new SoulCampfireBlockHandler());
    }

    private static void register(BlockManager blockManager, BlockHandler handler) {
        blockManager.registerHandler(handler.getKey(), () -> handler);
    }
}
