package net.onelitefeather.cygnus.setup.util;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * Helper class to share the same decimal format for all positions.
 *
 * @author theEvilReaper
 * @version 1.0.0
 * @since 2.6.0
 */
public final class PositionFormat {

    public static final DecimalFormat DECIMAL_FORMAT;

    static {
        DECIMAL_FORMAT = new DecimalFormat("#.##");
        DECIMAL_FORMAT.setRoundingMode(RoundingMode.CEILING);
        DECIMAL_FORMAT.setDecimalFormatSymbols(DecimalFormatSymbols.getInstance(Locale.ROOT));
    }

    private PositionFormat() {
        // Nothing to do here
    }
}
