package net.onelitefeather.cygnus.setup.dialog.settings;

import net.kyori.adventure.key.Key;
import net.onelitefeather.cygnus.common.config.CreekSettings;
import net.onelitefeather.cygnus.common.config.PageSettings;
import net.onelitefeather.cygnus.common.map.GameMapBuilder;

import java.util.List;
import java.util.Map;

/**
 * The settings forms of a game map.
 *
 * @author theEvilReaper
 * @version 1.0.0
 * @since 2.17.0
 */
public final class SettingsForms {

    /** The page values of the map, stored in its pages.json. */
    public static final SettingsForm<PageSettings> PAGES = new SettingsForm<>(
            "Page settings",
            Key.key("cygnus", "page_settings_dialog"),
            Key.key("cygnus", "page_settings_reset_dialog"),
            List.of(
                    new SettingsField<>("proximityRange", "Chime range", "blocks", 1, PageSettings.MAX_PROXIMITY_RANGE, 1, PageSettings::proximityRange),
                    new SettingsField<>("minActivePages", "Pages at once", "", 1, PageSettings.MAX_MIN_ACTIVE_PAGES, 1, PageSettings::minActivePages),
                    new SettingsField<>("ttlSeconds", "Page moves after", "s", PageSettings.MIN_TTL_SECONDS, PageSettings.MAX_TTL_SECONDS, 5, PageSettings::ttlSeconds),
                    new SettingsField<>("respawnDelaySeconds", "Found page returns after", "s", PageSettings.MIN_RESPAWN_DELAY_SECONDS, PageSettings.MAX_RESPAWN_DELAY_SECONDS, 1, PageSettings::respawnDelaySeconds)
            ),
            PageSettings.DEFAULT,
            values -> new PageSettings(
                    whole(values, "proximityRange"),
                    whole(values, "minActivePages"),
                    whole(values, "ttlSeconds"),
                    whole(values, "respawnDelaySeconds")),
            GameMapBuilder::getPageSettings,
            GameMapBuilder::setPageSettings
    );

    /** The creek distances of the map, stored in its creek.json. */
    public static final SettingsForm<CreekSettings> CREEK = new SettingsForm<>(
            "Creek settings",
            Key.key("cygnus", "creek_settings_dialog"),
            Key.key("cygnus", "creek_settings_reset_dialog"),
            List.of(
                    new SettingsField<>("sightRange", "Seen from", "blocks", 1, CreekSettings.MAX_SIGHT_RANGE, 1, CreekSettings::sightRange),
                    new SettingsField<>("stalkMinDistance", "Stalks from at least", "blocks", 1, CreekSettings.MAX_STALK_DISTANCE, 1, CreekSettings::stalkMinDistance),
                    new SettingsField<>("stalkMaxDistance", "Stalks from at most", "blocks", 1, CreekSettings.MAX_STALK_DISTANCE, 1, CreekSettings::stalkMaxDistance),
                    new SettingsField<>("respawnMinDistance", "Returns at least", "blocks", 1, CreekSettings.MAX_RESPAWN_MIN_DISTANCE, 1, CreekSettings::respawnMinDistance),
                    new SettingsField<>("routeLinkDistance", "Links routes within", "blocks", CreekSettings.MIN_ROUTE_LINK_DISTANCE, CreekSettings.MAX_ROUTE_LINK_DISTANCE, 0.5, CreekSettings::routeLinkDistance),
                    new SettingsField<>("personalSpace", "Keeps away by", "blocks", 1, CreekSettings.MAX_PERSONAL_SPACE, 1, CreekSettings::personalSpace),
                    new SettingsField<>("launchHeight", "Throws a catch", "blocks", 0, CreekSettings.MAX_LAUNCH_HEIGHT, 0.5, CreekSettings::launchHeight)
            ),
            CreekSettings.DEFAULT,
            values -> new CreekSettings(
                    whole(values, "sightRange"),
                    whole(values, "stalkMinDistance"),
                    whole(values, "stalkMaxDistance"),
                    whole(values, "respawnMinDistance"),
                    values.get("routeLinkDistance"),
                    whole(values, "personalSpace"),
                    values.get("launchHeight")),
            GameMapBuilder::getCreekSettings,
            GameMapBuilder::setCreekSettings
    );

    private SettingsForms() {
        // Nothing to do here
    }

    private static int whole(Map<String, Double> values, String key) {
        return (int) Math.round(values.get(key));
    }
}
