package net.onelitefeather.cygnus.common.player;

import net.kyori.adventure.permission.PermissionChecker;
import net.kyori.adventure.pointer.Pointers;
import net.kyori.adventure.util.TriState;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import net.luckperms.api.query.QueryOptions;
import net.minestom.server.entity.Player;
import net.minestom.server.network.player.GameProfile;
import net.minestom.server.network.player.PlayerConnection;
import net.onelitefeather.cygnus.common.permission.LuckPermsSupport;
import net.onelitefeather.cygnus.common.permission.TriStates;
import org.jetbrains.annotations.NotNull;

/**
 * A {@link Player} that answers permission questions through LuckPerms.
 * <p>
 * Minestom has no permission system of its own — it only carries Adventure's
 * {@link PermissionChecker#POINTER}, and everything that asks about permissions reads it from
 * there: LuckPerms' own command sender factory, our {@code /stop} command, and the CloudNet bridge
 * extension. Neither Minestom nor LuckPerms ever <em>installs</em> that pointer, though; the server
 * implementation has to supply it. Without it every permission check silently resolves to
 * {@code false}, which would lock staff out of CloudNet maintenance mode just like everyone else.
 * <p>
 * The pointer is dynamic, so no LuckPerms class is touched until a permission is actually queried.
 * <p>
 * Without LuckPerms on the class path every check answers {@link TriState#TRUE} instead, so local
 * runs and tests reach permission-gated paths at all. See {@code LuckPermsSupport}.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.6.7
 **/
public abstract class PermissionAwarePlayer extends Player implements PermissionChecker {

    private final @NotNull Pointers pointers = PermissionAwarePlayer.super.pointers()
            .toBuilder()
            .withDynamic(PermissionChecker.POINTER, () -> this)
            .build();

    /**
     * {@inheritDoc}
     */
    protected PermissionAwarePlayer(PlayerConnection playerConnection, GameProfile gameProfile) {
        super(playerConnection, gameProfile);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Pointers pointers() {
        return this.pointers;
    }

    /**
     * Resolves a permission for this player through LuckPerms, honouring the contexts LuckPerms
     * has calculated for them.
     *
     * @param permission the permission node to check
     * @return {@link TriState#TRUE} when LuckPerms is absent, the value LuckPerms holds for the
     * node otherwise, or {@link TriState#FALSE} when LuckPerms has no user data for this player
     */
    @Override
    public @NotNull TriState value(@NotNull String permission) {
        if (!LuckPermsSupport.isPresent()) {
            LuckPermsSupport.noteFallbackGrant(permission);
            return TriState.TRUE;
        }
        User user = LuckPermsProvider.get().getUserManager().getUser(getUuid());
        if (user == null) {
            return TriState.FALSE;
        }
        QueryOptions queryOptions = LuckPermsProvider.get().getContextManager().getQueryOptions(this);
        return TriStates.fromLuckPerms(user.getCachedData().getPermissionData(queryOptions).checkPermission(permission));
    }
}
