package com.revnomix.revseed.integration.file;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import com.revnomix.revseed.integration.exception.RevseedException;
import com.revnomix.revseed.integration.file.annotation.DelimitedField;
import com.revnomix.revseed.integration.file.annotation.DelimitedFields;
import com.revnomix.revseed.integration.utility.ConvertUtils;
import com.revnomix.revseed.integration.utility.ReflectionUtil;


public class DelimitedFileIterator<T> implements Iterator<T>, AutoCloseable {

    private final CSVParser csvParser;
    private final Iterator<CSVRecord> iterator;
    private final Class<T> convertToClass;
    private final List<Field> delimitedFields;
    private int recordsRead = 0;

    public DelimitedFileIterator(String fileContents, DelimitedFileType delimitedFileType, Class<T> convertToClass) {
        this.convertToClass = convertToClass;
        this.delimitedFields = ReflectionUtil.getFieldsWithAnnotations(convertToClass, DelimitedFields.class, DelimitedField.class);

        try {
            csvParser = CSVParser.parse(fileContents, delimitedFileType.getCSVFormat());
            iterator = csvParser.iterator();
        } catch (IOException ioe) {
            throw new RevseedException("Unable to parse contents: " + fileContents, ioe);
        }
    }

    public DelimitedFileIterator(File file, DelimitedFileType delimitedFileType, Class<T> convertToClass) {

        this.convertToClass = convertToClass;
        this.delimitedFields = ReflectionUtil.getFieldsWithAnnotations(convertToClass, DelimitedFields.class, DelimitedField.class);

        try {
            csvParser = CSVParser.parse(file, Charset.forName("utf-8"), delimitedFileType.getCSVFormat());
            iterator = csvParser.iterator();
        } catch (IOException ioe) {
            throw new RevseedException("Unable to parse file: " + file, ioe);
        }
    }

    @Override
    public boolean hasNext() {
        boolean hasNext = iterator.hasNext();
        if (!hasNext) {
            close();
        }

        return hasNext;
    }

    @Override
    public T next() {
        CSVRecord csvRecord = iterator.next();
        recordsRead++;

        T obj = null;
        List<DelimitedField> delimitedFieldAnnotations = null;

        try {
            obj = (T) convertToClass.newInstance();

            for (Field delimitedField : delimitedFields) {
                delimitedField.setAccessible(true);

                // Get the annotation off the field
                delimitedFieldAnnotations = getDelimitedFields(delimitedField);

                // For all DelimitedField annotations, try to set the field value
                for (DelimitedField delimitedFieldAnnotation : delimitedFieldAnnotations) {
                    setFieldsOnObject(csvRecord, obj, delimitedField, delimitedFieldAnnotation);
                }
            }

            maybeSetPosition(obj);
        } catch (InstantiationException | IllegalAccessException e) {
            throw new RevseedException("Unable to create object of type: " + convertToClass, e);
        }

        return obj;
    }

    public Boolean stream() {
        try {
            while (hasNext()) {
                T next = next();
            }
        } catch (Exception e) {
            return false;
        } finally {
            close();
        }
        return true;
    }

    public List<T> all() {
        List<T> allRecords = new ArrayList<>();

        try {
            while (hasNext()) {
                allRecords.add(next());
            }
        } finally {
            close();
        }

        return allRecords;
    }

    public void close() {
        try {
            csvParser.close();
        } catch (IOException ioe) {
            throw new RevseedException("Unable to close Parser: " + csvParser, ioe);
        }
    }

    public int recordsRead() {
        return recordsRead;
    }

    private void setFieldsOnObject(CSVRecord csvRecord, T obj, Field delimitedField,
                                   DelimitedField delimitedFieldAnnotation) throws IllegalAccessException {
        // If the field is mapped, get/set the value
        if (csvRecord.isSet(delimitedFieldAnnotation.name())) {
            delimitedField.set(obj, ConvertUtils.convert(csvRecord.get(delimitedFieldAnnotation.name()), delimitedField.getType()));
        }

        // If the fieldValue is null and the annotation has a non-default value defaultValue, we should set it
        if (delimitedField.get(obj) == null && !DelimitedField.NULL_DEFAULT_VALUE.equals(delimitedFieldAnnotation.defaultValue())) {

            // If the field is a String, the defaultValue could be "" and using the convert utils will convert it into a 'null' value
            if (delimitedField.getType().isAssignableFrom(String.class)) {
                delimitedField.set(obj, delimitedFieldAnnotation.defaultValue());
            } else {
                delimitedField.set(obj, ConvertUtils.convert(delimitedFieldAnnotation.defaultValue(), delimitedField.getType()));
            }
        }
    }

    private List<DelimitedField> getDelimitedFields(Field field) {
        if (field.isAnnotationPresent(DelimitedFields.class)) {
            DelimitedFields delimitedFields = field.getAnnotation(DelimitedFields.class);
            return Arrays.asList(delimitedFields.value());
        } else if (field.isAnnotationPresent(DelimitedField.class)) {
            return Arrays.asList(field.getAnnotation(DelimitedField.class));
        }

        return new ArrayList<DelimitedField>();
    }

    private void maybeSetPosition(Object obj) {
        if (obj instanceof PositionAware) {
            ((PositionAware) obj).setPosition(csvParser.getCurrentLineNumber());
        }
    }
}

