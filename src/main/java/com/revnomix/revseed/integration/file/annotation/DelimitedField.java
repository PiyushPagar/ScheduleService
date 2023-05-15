package com.revnomix.revseed.integration.file.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Retention(RUNTIME)
@Target({ FIELD })
public @interface DelimitedField {
    public static final String NULL_DEFAULT_VALUE = "NULL_DEFAULT_VALUE";

    String name();

    String defaultValue() default NULL_DEFAULT_VALUE;
}
