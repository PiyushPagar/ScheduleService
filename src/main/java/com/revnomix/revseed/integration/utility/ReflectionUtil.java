package com.revnomix.revseed.integration.utility;


import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ReflectionUtils;

import com.revnomix.revseed.integration.exception.RevseedException;

public class ReflectionUtil {
    private static Logger LOGGER = LoggerFactory.getLogger(ReflectionUtil.class);

    private ReflectionUtil() {
    }

    public static List<String> getFieldNames(Class<?> clazz) {
        List<String> fieldNames = new ArrayList<String>();

        List<Field> fields = findFields(clazz);
        for (Field field : fields) {
            fieldNames.add(field.getName());
        }

        return fieldNames;
    }

    public static Object getField(Object obj, String fieldName) {
        try {
            List<Field> fields = findFields(obj.getClass());
            for (Field field : fields) {
                if (field.getName().equals(fieldName)) {
                    return field.get(obj);
                }
            }

            throw new RevseedException("Unable to get field: " + fieldName + " on Object: " + obj);
        } catch (Exception e) {
            throw new RevseedException("Unable to get field: " + fieldName + " on Object: " + obj, e);
        }
    }

    public static Object getValueWithAnnotation(Object obj, Class<? extends Annotation> annotation) {
        List<Field> fieldsWithAnnotation = getFieldsWithAnnotations(obj.getClass(), annotation);

        if (CollectionUtils.isNotEmpty(fieldsWithAnnotation)) {
            try {
                return fieldsWithAnnotation.get(0).get(obj);
            } catch (Exception e) {
                LOGGER.trace("Unable to get field value with Annotation:" + annotation, e);
                return null;
            }
        }

        return null;
    }

    @SafeVarargs
    public static List<Field> getFieldsWithAnnotations(Class<?> clazz, Class<? extends Annotation>... annotations) {
        List<Field> fieldsWithAnnotations = new ArrayList<Field>();

        List<Field> fields = findFields(clazz);
        for (Field field : fields) {
            for (Class<? extends Annotation> annotation : annotations) {
                if (field.isAnnotationPresent(annotation)) {
                    fieldsWithAnnotations.add(field);
                }
            }
        }

        return fieldsWithAnnotations;
    }

    private static List<Field> findFields(Class<?> clazz) {
        final List<Field> fields = new ArrayList<Field>();

        List<Class<?>> classes = findClassHierarchy(clazz);
        for (Class<?> clazzz : classes) {
            ReflectionUtils.doWithFields(clazzz, new ReflectionUtils.FieldCallback() {
                @Override
                public void doWith(Field field) throws IllegalArgumentException, IllegalAccessException {
                    field.setAccessible(true);
                    fields.add(field);
                }
            }, ReflectionUtils.COPYABLE_FIELDS);
        }

        return fields;
    }

    private static List<Class<?>> findClassHierarchy(Class<?> clazz) {
        // Determine the class hierarchy
        List<Class<?>> classHierarchy = new ArrayList<Class<?>>();
        Class<?> currentClass = clazz;
        while (!currentClass.equals(Object.class)) {
            classHierarchy.add(currentClass);
            currentClass = currentClass.getSuperclass();
        }

        // Reverse the classes so that super class steps would come before their subclass
        Collections.reverse(classHierarchy);
        return classHierarchy;
    }
}