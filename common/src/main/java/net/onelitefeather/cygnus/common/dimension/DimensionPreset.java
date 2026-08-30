package net.onelitefeather.cygnus.common.dimension;

/**
 * A named {@link DimensionAtmosphere} - an atmosphere that can be referred to by key instead of by
 * object reference, e.g. from a config or when registering the dimension.
 * <p>
 * It contains only data about the fog and sky colors, and nothing else. If you also want to adjust
 * your biome, that is an additional step that is not covered here.
 * </p>
 *
 * @author thEvilReaper
 * @version 2.0.0
 * @since 2.6.6
 */
public interface DimensionPreset extends DimensionAtmosphere {

    /**
     * A short, unique, lowercase identifier for this preset, used wherever
     * the preset needs to be referenced by name instead of by object
     * reference, e.g. in configs or when registering the dimension.
     *
     * @return the identifier of this preset
     */
    String getKey();
}
