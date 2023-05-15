package com.revnomix.revseed.integration.utility;

import java.math.BigDecimal;
import java.util.Date;

import org.apache.commons.beanutils.BeanUtilsBean;
import org.apache.commons.beanutils.ConvertUtilsBean;
import org.apache.commons.beanutils.converters.BigDecimalConverter;
import org.apache.commons.beanutils.converters.BooleanConverter;
import org.apache.commons.beanutils.converters.IntegerConverter;

public class ConvertUtils {

    protected static ConvertUtilsBean CONVERT_UTILS_BEAN = new ConvertUtilsBean();
    protected static BeanUtilsBean BEAN_UTILS_BEAN;

    static {
        EmptyStringToNullConverter stringConverter = new EmptyStringToNullConverter();
        CONVERT_UTILS_BEAN.register(stringConverter, String.class);

        BooleanConverter booleanConverter = new BooleanConverter(null);
        CONVERT_UTILS_BEAN.register(booleanConverter, Boolean.class);

        IntegerConverter integerConverter = new IntegerConverter(null);
        CONVERT_UTILS_BEAN.register(integerConverter, Integer.class);

        LenientAwareDateConverter lenientAwareDateConverter = new LenientAwareDateConverter(new String[] {"yyyy-MM-dd", "yyyyMMdd", "dd-MMM-yy"}, new String[] {"yyyy-MM-dd HH:mm:ss"});
        CONVERT_UTILS_BEAN.register(lenientAwareDateConverter, Date.class);

        BigDecimalConverter bigDecimalConverter = new BigDecimalConverter(null);
        CONVERT_UTILS_BEAN.register(bigDecimalConverter, BigDecimal.class);
        BEAN_UTILS_BEAN = new BeanUtilsBean(CONVERT_UTILS_BEAN);
    }

    private ConvertUtils() {
    }

    public static <T> T convert(String value, Class<T> clazz) {
        return convert(value, clazz, null);
    }

    @SuppressWarnings("unchecked")
    public static <T> T convert(String valueStr, Class<T> clazz, T defaultValue) {
        T value = (T) CONVERT_UTILS_BEAN.convert(valueStr, clazz);
        if (value == null && defaultValue != null) {
            return defaultValue;
        }

        return value;
    }
}
