package net.onelitefeather.cygnus.common.rank;

import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.NodeType;
import net.luckperms.api.node.types.InheritanceNode;
import net.minestom.server.entity.Player;
import net.onelitefeather.cygnus.common.permission.LuckPermsSupport;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry and resolver for mapping LuckPerms ranks/groups to {@link RankTag}s.
 *
 * @author theEvilReaper
 * @version 1.0.0
 * @since 2.8.0
 */
public final class RankTagRegistry {

    private static final RankTagRegistry STANDARD = new RankTagRegistry();

    static {
        STANDARD.registerDefaults();
    }

    private final Map<String, RankTag> tagsById = new ConcurrentHashMap<>();
    private final Map<String, RankTag> tagsByGroup = new ConcurrentHashMap<>();
    private volatile RankTag fallbackTag = RankTag.PLAYER;

    /**
     * Returns the global default {@link RankTagRegistry} instance pre-configured with standard tags.
     *
     * @return the shared standard registry
     */
    public static RankTagRegistry standard() {
        return STANDARD;
    }

    /**
     * Registers the default OLF rank tags and standard group aliases.
     */
    public void registerDefaults() {
        register(RankTag.ADMINISTRATOR, "administrator", "admin", "owner");
        register(RankTag.ASSISTENT, "assistent", "assistant", "sr_mod", "srmod");
        register(RankTag.MOD, "mod", "moderator");
        register(RankTag.CONTENT, "content", "developer", "dev", "builder");
        register(RankTag.MEDIA, "media", "creator", "youtube", "twitch");
        register(RankTag.LITE, "lite", "vip", "premium");
        register(RankTag.PLAYER, "player", "default");
    }

    /**
     * Registers a {@link RankTag} along with associated LuckPerms group aliases.
     *
     * @param tag          the rank tag to register
     * @param groupAliases group names that map to this tag (case-insensitive)
     */
    public void register(RankTag tag, String... groupAliases) {
        Objects.requireNonNull(tag, "tag must not be null");
        this.tagsById.put(tag.id().toLowerCase(Locale.ROOT), tag);

        for (String alias : groupAliases) {
            if (!alias.isBlank()) {
                this.tagsByGroup.put(alias.toLowerCase(Locale.ROOT), tag);
            }
        }
    }

    /**
     * Finds a registered tag by its unique identifier.
     *
     * @param id the tag id
     * @return the optional rank tag
     */
    public Optional<RankTag> findById(@Nullable String id) {
        if (id == null) return Optional.empty();
        return Optional.ofNullable(this.tagsById.get(id.toLowerCase(Locale.ROOT)));
    }

    /**
     * Resolves a rank tag for a specific group name.
     *
     * @param groupName the name of the group
     * @return the matching {@link RankTag}, or empty if unmapped
     */
    public Optional<RankTag> resolveByGroup(@Nullable String groupName) {
        if (groupName == null || groupName.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(this.tagsByGroup.get(groupName.toLowerCase(Locale.ROOT)));
    }

    /**
     * Resolves the primary {@link RankTag} for a player with highest priority.
     *
     * @param player the player to resolve for
     * @return the resolved {@link RankTag}, or {@link #getFallbackTag()} if unresolvable
     */
    public RankTag resolvePrimary(Player player) {
        return resolvePrimary(player.getUuid());
    }

    /**
     * Resolves the primary {@link RankTag} for a player's UUID by checking LuckPerms groups.
     *
     * @param uuid the unique id of the player
     * @return the resolved {@link RankTag} with highest priority, or {@link #getFallbackTag()}
     */
    public RankTag resolvePrimary(UUID uuid) {
        if (!LuckPermsSupport.isPresent()) {
            return this.fallbackTag;
        }

        try {
            User user = LuckPermsProvider.get().getUserManager().getUser(uuid);
            if (user == null) {
                return this.fallbackTag;
            }

            List<RankTag> matchedTags = resolveUserTags(user);
            if (matchedTags.isEmpty()) {
                return resolveByGroup(user.getPrimaryGroup()).orElse(this.fallbackTag);
            }

            return matchedTags.getFirst();
        } catch (Exception _) {
            return this.fallbackTag;
        }
    }

    /**
     * Resolves all available {@link RankTag}s a player is eligible for, sorted by priority descending.
     * Useful for allowing the player to select/rotate between multiple unlocked tags.
     *
     * @param uuid the unique id of the player
     * @return a list of all matching rank tags, sorted by priority (highest first)
     */
    public List<RankTag> resolveAvailable(UUID uuid) {
        if (uuid == null || !LuckPermsSupport.isPresent()) {
            return List.of(this.fallbackTag);
        }

        try {
            User user = LuckPermsProvider.get().getUserManager().getUser(uuid);
            if (user == null) {
                return List.of(this.fallbackTag);
            }

            List<RankTag> matched = resolveUserTags(user);
            return matched.isEmpty() ? List.of(this.fallbackTag) : matched;
        } catch (Exception _) {
            return List.of(this.fallbackTag);
        }
    }

    /**
     * Internal helper to collect all distinct mapped {@link RankTag}s for a LuckPerms {@link User}.
     */
    private List<RankTag> resolveUserTags(User user) {
        Set<RankTag> matched = new HashSet<>();

        // Check primary group
        resolveByGroup(user.getPrimaryGroup()).ifPresent(matched::add);

        // Check all inherited groups
        Collection<InheritanceNode> nodes = user.getNodes(NodeType.INHERITANCE);
        for (InheritanceNode node : nodes) {
            resolveByGroup(node.getGroupName()).ifPresent(matched::add);
        }

        return matched.stream()
                .sorted(Comparator.comparingInt(RankTag::priority).reversed())
                .toList();
    }

    /**
     * Returns the fallback tag when no matching group is found or LuckPerms is absent.
     *
     * @return the fallback rank tag
     */
    public RankTag getFallbackTag() {
        return this.fallbackTag;
    }

    /**
     * Sets the fallback tag used when no matching group is found.
     *
     * @param fallbackTag the new fallback tag
     */
    public void setFallbackTag(RankTag fallbackTag) {
        this.fallbackTag = Objects.requireNonNull(fallbackTag, "fallbackTag must not be null");
    }

    /**
     * Returns an unmodifiable collection of all registered tags.
     *
     * @return collection of registered rank tags
     */
    public Collection<RankTag> getAllTags() {
        return Collections.unmodifiableCollection(this.tagsById.values());
    }
}
