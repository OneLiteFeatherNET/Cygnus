package net.onelitefeather.cygnus.setup.util;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * The {@link FormatHelper} class provides a {@link DecimalFormat} instance which is used to format numbers.
 * The class is used to avoid multiple instances of the same {@link DecimalFormat} instance.
 * The class is final and cannot be instantiated.
 *
 * @author theEvilReaper
 * @version 1.0.0
 * @since 1.0.0
 */
public final class FormatHelper {

    public static final DecimalFormat DECIMAL_FORMAT;

    static {
        DECIMAL_FORMAT = new DecimalFormat("#.##");
        DECIMAL_FORMAT.setRoundingMode(RoundingMode.CEILING);
        DECIMAL_FORMAT.setDecimalFormatSymbols(DecimalFormatSymbols.getInstance(Locale.ROOT));
    }

    private FormatHelper() {
        throw new UnsupportedOperationException("This class cannot be instantiated");
    }
}
