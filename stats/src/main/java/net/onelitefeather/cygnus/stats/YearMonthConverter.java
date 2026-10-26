package net.onelitefeather.cygnus.stats;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.time.YearMonth;

/**
 * Stores a {@link YearMonth} as text {@code YYYY-MM}, which sorts as a string and needs no date type in the database.
 *
 * @author TheMeinerLP
 * @version 1.0.0
 * @since 2.21.0
 */
@Converter
public class YearMonthConverter implements AttributeConverter<YearMonth, String> {

    /**
     * Writes the month as {@code YYYY-MM}.
     *
     * @param month the month, {@code null} for none
     * @return the text, {@code null} for none
     */
    @Override
    public String convertToDatabaseColumn(YearMonth month) {
        return month == null ? null : month.toString();
    }

    /**
     * Reads the month from {@code YYYY-MM}.
     *
     * @param column the text, {@code null} for none
     * @return the month, {@code null} for none
     */
    @Override
    public YearMonth convertToEntityAttribute(String column) {
        return column == null ? null : YearMonth.parse(column);
    }
}
