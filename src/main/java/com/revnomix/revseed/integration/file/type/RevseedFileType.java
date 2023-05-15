package com.revnomix.revseed.integration.file.type;

import org.apache.commons.csv.CSVFormat;


import com.revnomix.revseed.schema.staah.Customer;
import com.revnomix.revseed.integration.file.DelimitedFileType;


public enum RevseedFileType implements DelimitedFileType {

    BOOKING_RAW_DATA(Customer.class,','),
    MOVIE_RAW_INFORMATION(Customer.class,'|'),
    THEATER_RAW_INFORMATION(Customer.class,',');

    private static final String RECORD_SEPARATOR = "\r\n";
    private final CSVFormat csvFormat;

    public static final String DATE_FORMAT_YYYYMMDD_WITHOUT_HYPHENS = "yyyyMMdd";

    RevseedFileType(Class<?> recordType, char delimeter) {
        csvFormat = CSVFormat.newFormat(delimeter).withHeader().withSkipHeaderRecord(true).withRecordSeparator(RECORD_SEPARATOR).withIgnoreEmptyLines(true);
    }

    @Override
    public CSVFormat getCSVFormat() {
        return csvFormat;
    }
}