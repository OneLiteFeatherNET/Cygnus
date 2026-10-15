package net.onelitefeather.cygnus.common.config;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * The client mods whose presence the shader probe looks for, with the translation keys each of them adds.
 * <p>
 * A mod that is installed resolves its keys on the client, so a translatable text using one of them shows the
 * translated text instead of its fallback. The keys were checked against the vanilla language file of 1.21.4,
 * 1.21.10 and 26.1, where none of them exist, and against the language files the mods ship. Iris also covers
 * Oculus, and Sodium also covers Rubidium: the mod ids name the mods that ship the keys, not every fork.
 * </p>
 * <p>
 * A mod that is not listed here cannot be probed. The config names mods by their id, see
 * {@link ClientGuardConfig#read(ConfigSection)}.
 * </p>
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.16.0
 */
public enum ShaderMod {

    /** Iris, the shader loader that the resource pack's core shaders do not survive. */
    IRIS("iris", "options.iris.apply", "options.iris.shaderPackSettings"),

    /** OptiFine, which replaces the rendering pipeline with its own. */
    OPTIFINE("optifine", "of.options.capeOF.title"),

    /** Canvas, the renderer for Indigo and Sodium-based Fabric setups. */
    CANVAS("canvas", "info.canvas.reloading"),

    /** Sodium, the renderer that only loses the cosmetic terrain fog of the resource pack. */
    SODIUM("sodium", "sodium.options.pages.general", "sodium.options.fullscreen_mode.name"),

    /** Embeddium, the Forge port of Sodium. */
    EMBEDDIUM("embeddium", "embeddium.search_bar_empty"),

    /** Nvidium, the terrain renderer that rides on Sodium. */
    NVIDIUM("nvidium", "nvidium.options.pages.nvidium");

    private final String id;
    private final List<String> translationKeys;

    ShaderMod(String id, String... translationKeys) {
        this.id = id;
        this.translationKeys = List.of(translationKeys);
    }

    /**
     * Returns the id the config names this mod by.
     *
     * @return the lower case mod id
     */
    public String id() {
        return this.id;
    }

    /**
     * Returns the translation keys this mod adds to the client's language.
     *
     * @return the keys, never empty
     */
    public List<String> translationKeys() {
        return this.translationKeys;
    }

    /**
     * Finds the mod with the given id. The lookup ignores case and surrounding whitespace.
     *
     * @param id the id as written in the config
     * @return the mod, or empty if no mod of the registry has that id
     */
    public static Optional<ShaderMod> byId(String id) {
        if (id == null) {
            return Optional.empty();
        }
        String wanted = id.trim().toLowerCase(Locale.ROOT);
        return Arrays.stream(values()).filter(mod -> mod.id.equals(wanted)).findFirst();
    }
}
