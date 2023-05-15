package com.revnomix.revseed.integration.utility;

import org.apache.commons.beanutils.ConversionException;
import org.apache.commons.beanutils.Converter;
import org.apache.commons.beanutils.locale.converters.DateLocaleConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;


public class LenientAwareDateConverter implements Converter {

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    private List<DateLocaleConverter> dateLocaleConverters;

    public LenientAwareDateConverter(String[] nonLenientDatePatterns, String[] lenientDatePatterns) {
        dateLocaleConverters = new ArrayList<DateLocaleConverter>();

        for (String nonLenientDatePattern : nonLenientDatePatterns) {
            dateLocaleConverters.add(buildDateLocaleConverter(nonLenientDatePattern, false));
        }

        for (String lenientDatePattern : lenientDatePatterns) {
            dateLocaleConverters.add(buildDateLocaleConverter(lenientDatePattern, true));
        }
    }

    @Override
    public <T> T convert(Class<T> type, Object value) {
        if (value != null) {
            for (DateLocaleConverter dateLocaleConverter : dateLocaleConverters) {
                try {
                    T convertedValue = dateLocaleConverter.convert(type, value);
                    if (convertedValue != null) {
                        return convertedValue;
                    }
                } catch (ConversionException ce) {
                    logger.trace("Unable to parse date: " + value, ce);
                    continue;
                }
            }
        }

        return null;
    }

    private DateLocaleConverter buildDateLocaleConverter(String pattern, boolean lenient) {
        DateLocaleConverter dateLocaleConverter = new DateLocaleConverter(Locale.getDefault(), pattern);
        dateLocaleConverter.setLenient(lenient);
        return dateLocaleConverter;
    }


}
