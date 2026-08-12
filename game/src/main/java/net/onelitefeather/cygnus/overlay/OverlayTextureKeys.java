package net.onelitefeather.cygnus.overlay;

import net.kyori.adventure.key.Key;

import java.util.function.IntFunction;

/**
 * Builds the {@code cygnus:} {@link Key}s a full-screen overlay draws from the resource pack.
 * <p>
 * {@code OverlayTunnelVisionRenderer}, {@code SlenderGazeService} and {@code BloodSplatterService}
 * each hand-rolled their own {@code buildTextures()}, one per axis count: a flat table indexed by
 * stage, a two-dimensional one indexed by level and frame, and a three-dimensional one (flattened
 * into a single array) indexed by direction, variant and frame. All three follow the same shape once
 * written out: the texture path, followed by every axis's label joined with {@code _}. This type is
 * that shape, extracted once for every table rank the three renderers need.
 * </p>
 * <p>
 * An axis's labels come from an {@link IntFunction}, not a plain 1-based count, because the blood
 * splatter's outermost axis is a {@code BloodDirection} name such as {@code left} rather than a
 * number. {@link #ONE_BASED} covers the common case of a numbered axis.
 * </p>
 *
 * <p>Usage:</p>
 * <pre>{@code
 * Key[] stages = OverlayTextureKeys.flat("gui/tunnel_vision/stage_", 16, OverlayTextureKeys.ONE_BASED);
 * Key[][] glitch = OverlayTextureKeys.table(
 *         "gui/glitch/level_", LEVELS, FRAMES, OverlayTextureKeys.ONE_BASED, OverlayTextureKeys.ONE_BASED);
 * Key[][][] blood = OverlayTextureKeys.cube(
 *         "gui/blood/", DIRECTIONS, VARIANTS, FRAMES,
 *         direction -> BloodDirection.values()[direction].name().toLowerCase(Locale.ROOT),
 *         OverlayTextureKeys.ONE_BASED, OverlayTextureKeys.ONE_BASED);
 * }</pre>
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.7.0
 */
public final class OverlayTextureKeys {

    /** The namespace every overlay texture lives under. */
    public static final String NAMESPACE = "cygnus";

    /** Labels an axis {@code 1}, {@code 2}, {@code 3}, ... from its zero-based index. */
    public static final IntFunction<String> ONE_BASED = index -> Integer.toString(index + 1);

    private OverlayTextureKeys() {
        // Prevent instantiation of utility class
    }

    /**
     * Builds a flat table of texture keys, one per index.
     *
     * @param path  the texture path every key is built from, without a trailing separator
     * @param size  how many keys to build
     * @param label labels each index
     * @return the keys, indexed the same way
     */
    public static Key[] flat(String path, int size, IntFunction<String> label) {
        Key[] keys = new Key[size];
        for (int i = 0; i < size; i++) {
            keys[i] = Key.key(NAMESPACE, path + label.apply(i));
        }
        return keys;
    }

    /**
     * Builds a two-dimensional table of texture keys, one per row and column.
     *
     * @param path        the texture path every key is built from, without a trailing separator
     * @param rows        how many rows to build
     * @param columns     how many columns to build
     * @param rowLabel    labels each row
     * @param columnLabel labels each column
     * @return the keys, indexed {@code [row][column]}
     */
    public static Key[][] table(String path, int rows, int columns, IntFunction<String> rowLabel,
                                 IntFunction<String> columnLabel) {
        Key[][] keys = new Key[rows][columns];
        for (int row = 0; row < rows; row++) {
            for (int column = 0; column < columns; column++) {
                keys[row][column] = Key.key(NAMESPACE, path + rowLabel.apply(row) + "_" + columnLabel.apply(column));
            }
        }
        return keys;
    }

    /**
     * Builds a three-dimensional table of texture keys, one per plane, row and column.
     *
     * @param path        the texture path every key is built from, without a trailing separator
     * @param planes      how many planes to build
     * @param rows        how many rows to build
     * @param columns     how many columns to build
     * @param planeLabel  labels each plane
     * @param rowLabel    labels each row
     * @param columnLabel labels each column
     * @return the keys, indexed {@code [plane][row][column]}
     */
    public static Key[][][] cube(String path, int planes, int rows, int columns, IntFunction<String> planeLabel,
                                  IntFunction<String> rowLabel, IntFunction<String> columnLabel) {
        Key[][][] keys = new Key[planes][rows][columns];
        for (int plane = 0; plane < planes; plane++) {
            for (int row = 0; row < rows; row++) {
                for (int column = 0; column < columns; column++) {
                    keys[plane][row][column] = Key.key(NAMESPACE,
                            path + planeLabel.apply(plane) + "_" + rowLabel.apply(row) + "_" + columnLabel.apply(column));
                }
            }
        }
        return keys;
    }
}
