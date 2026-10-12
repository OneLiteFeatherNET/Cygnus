package net.onelitefeather.cygnus.clientguard;

import net.onelitefeather.cygnus.common.config.ClientGuardConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The rules of the client check without a server: brands, channels, the whitelist and the mode.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.16.0
 */
class ClientInspectorTest {

    private static final ClientGuardConfig DEFAULTS = ClientGuardConfig.DEFAULT;

    private static ClientGuardConfig config(ClientGuardConfig.Mode mode, List<String> allowedBrands) {
        return new ClientGuardConfig(mode, DEFAULTS.blockedBrands(), DEFAULTS.blockedChannels(), allowedBrands);
    }

    private static ClientProfile brand(String brand) {
        return ClientProfile.EMPTY.withBrand(brand);
    }

    private static Verdict inspect(ClientProfile profile) {
        return ClientInspector.inspect(profile, DEFAULTS);
    }

    @ParameterizedTest(name = "brand \"{0}\" is blocked")
    @ValueSource(strings = {"meteor", "meteor-client", "Wurst 7.15", "Aristois", "LiquidBounce", "impact",
            "Future", "rusherhack", "inertia", "BleachHack"})
    @DisplayName("A brand containing a blocked fragment is blocked, whatever its case")
    void blockedBrandIsBlocked(String clientBrand) {
        Verdict verdict = inspect(brand(clientBrand));

        assertInstanceOf(Verdict.Blocked.class, verdict, "brand '" + clientBrand + "' must be blocked");
    }

    @ParameterizedTest(name = "brand \"{0}\" is allowed")
    @ValueSource(strings = {"vanilla", "fabric", "forge", "neoforge", "lunarclient:vanilla", "labymod"})
    @DisplayName("Vanilla and the common mod loaders and clients are allowed")
    void commonBrandIsAllowed(String clientBrand) {
        Verdict verdict = inspect(brand(clientBrand));

        assertInstanceOf(Verdict.Allowed.class, verdict, "brand '" + clientBrand + "' must stay");
    }

    @Test
    @DisplayName("The blocked reason names the brand the client sent")
    void reasonNamesTheBrand() {
        Verdict verdict = inspect(brand("Meteor-Client"));

        Verdict.Blocked blocked = assertInstanceOf(Verdict.Blocked.class, verdict, "meteor must be blocked");
        assertTrue(blocked.reason().contains("Meteor-Client"), "the reason must name the brand: " + blocked.reason());
    }

    @ParameterizedTest(name = "channel \"{0}\" is blocked")
    @ValueSource(strings = {"wdl:init", "wdl:request", "WDL:Control"})
    @DisplayName("A channel starting with a blocked prefix is blocked, whatever its case")
    void blockedChannelIsBlocked(String channel) {
        ClientProfile profile = ClientProfile.EMPTY.withChannels(Set.of(channel));

        assertInstanceOf(Verdict.Blocked.class, inspect(profile), "channel '" + channel + "' must be blocked");
    }

    @ParameterizedTest(name = "channel \"{0}\" is allowed")
    @ValueSource(strings = {"mywdl:init", "minimap:radar", "minecraft:brand", "labymod:neo"})
    @DisplayName("A channel only matches a blocked prefix at its start")
    void channelMatchesOnlyAtTheStart(String channel) {
        ClientProfile profile = ClientProfile.EMPTY.withChannels(Set.of(channel));

        assertInstanceOf(Verdict.Allowed.class, inspect(profile), "channel '" + channel + "' must stay");
    }

    @Test
    @DisplayName("A client with no brand and no channels yet is not blocked")
    void silentClientIsAllowed() {
        assertInstanceOf(Verdict.Allowed.class, inspect(ClientProfile.EMPTY), "a client that has not spoken yet must stay");
    }

    @Test
    @DisplayName("The off mode lets even a known cheat client through")
    void offModeAllowsEverything() {
        ClientGuardConfig off = config(ClientGuardConfig.Mode.OFF, List.of("vanilla"));

        Verdict verdict = ClientInspector.inspect(brand("meteor"), off);

        assertInstanceOf(Verdict.Allowed.class, verdict, "the off mode must not check anything");
    }

    @Test
    @DisplayName("A listed brand may join when a whitelist is configured")
    void whitelistAllowsListedBrand() {
        ClientGuardConfig whitelist = config(ClientGuardConfig.Mode.KICK, List.of("vanilla"));

        Verdict verdict = ClientInspector.inspect(brand("Vanilla"), whitelist);

        assertInstanceOf(Verdict.Allowed.class, verdict, "a listed brand must pass the whitelist");
    }

    @Test
    @DisplayName("A brand missing from the whitelist is blocked")
    void whitelistBlocksUnlistedBrand() {
        ClientGuardConfig whitelist = config(ClientGuardConfig.Mode.KICK, List.of("vanilla"));

        Verdict verdict = ClientInspector.inspect(brand("fabric"), whitelist);

        assertInstanceOf(Verdict.Blocked.class, verdict, "an unlisted brand must not pass the whitelist");
    }

    @Test
    @DisplayName("A client without a brand yet is not blocked by the whitelist")
    void whitelistWaitsForMissingBrand() {
        ClientGuardConfig whitelist = config(ClientGuardConfig.Mode.KICK, List.of("vanilla"));

        Verdict verdict = ClientInspector.inspect(ClientProfile.EMPTY, whitelist);

        assertInstanceOf(Verdict.Allowed.class, verdict, "the whitelist must wait until the client has named its brand");
    }

    @Test
    @DisplayName("A blocked channel still blocks a client that has not named its brand yet")
    void blockedChannelAppliesWithoutBrandUnderWhitelist() {
        ClientGuardConfig whitelist = config(ClientGuardConfig.Mode.KICK, List.of("vanilla"));
        ClientProfile profile = ClientProfile.EMPTY.withChannels(Set.of("wdl:init"));

        Verdict verdict = ClientInspector.inspect(profile, whitelist);

        assertInstanceOf(Verdict.Blocked.class, verdict, "a blocked channel must not wait for the brand");
    }

    @Test
    @DisplayName("The blocked brand is reported before the whitelist is consulted")
    void blockedBrandIsReportedFirst() {
        ClientGuardConfig whitelist = config(ClientGuardConfig.Mode.KICK, List.of("meteor"));

        Verdict.Blocked blocked = assertInstanceOf(Verdict.Blocked.class,
                ClientInspector.inspect(brand("meteor"), whitelist), "meteor must be blocked");

        assertEquals("brand 'meteor' matches the blocked brand 'meteor'", blocked.reason(),
                "the blocklist must decide before the whitelist does");
    }
}
