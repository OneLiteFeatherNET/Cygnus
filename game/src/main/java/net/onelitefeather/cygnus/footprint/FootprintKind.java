package net.onelitefeather.cygnus.footprint;

import net.minestom.server.instance.block.Block;

/**
 * Whose print it is, and the petal states that show it.
 * <p>
 * The resource pack turns {@code pink_petals} into prints: the flower amount picks the model,
 * 1 and 2 for the bright slender and survivor print, 3 and 4 for their faded stages.
 * </p>
 *
 * @author Joltra
 * @version 1.0.0
 * @since 2.16.0
 */
enum FootprintKind {

    SLENDER(1, 3),
    SURVIVOR(2, 4);

    private final int brightAmount;
    private final int fadedAmount;

    FootprintKind(int brightAmount, int fadedAmount) {
        this.brightAmount = brightAmount;
        this.fadedAmount = fadedAmount;
    }

    /**
     * Returns the petal state that shows this kind. The resource pack maps the flower amount to the
     * print model, the facing only turns it.
     *
     * @param facing one of {@link FootprintSpawner#FACINGS}
     * @param faded  whether the faded stage is wanted
     * @return the block state to send
     */
    Block block(String facing, boolean faded) {
        int amount = faded ? this.fadedAmount : this.brightAmount;
        return Block.PINK_PETALS.withProperty("facing", facing).withProperty("flower_amount", String.valueOf(amount));
    }
}
