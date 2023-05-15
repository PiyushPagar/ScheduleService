package com.revnomix.revseed.integration.utility;

import org.apache.commons.beanutils.converters.AbstractConverter;
import org.apache.commons.beanutils.converters.StringConverter;
import org.apache.commons.lang3.StringUtils;

public class EmptyStringToNullConverter extends AbstractConverter {

    // Couldn't extend the StringConverter, so utilzing it to do the conversions
    private StringConverter stringConverter = new StringConverter(null);

    @Override
    protected <T> T convertToType(Class<T> type, Object value) throws Throwable {
        return stringConverter.convert(type, value);
    }

    @Override
    protected String convertToString(Object value) throws Throwable {
        return StringUtils.trimToNull(super.convertToString(value));
    }

    @Override
    protected Class<?> getDefaultType() {
        return String.class;
    }
}
