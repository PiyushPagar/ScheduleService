package com.revnomix.revseed.integration.utility;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import javax.xml.datatype.XMLGregorianCalendar;

import com.revnomix.revseed.integration.exception.RevseedException;

public class DateTimeUtil {

    public static final String DATE_FORMAT = "yyyy-MM-dd";
    public static final String DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss.SSS";
    public static final String UTC = "UTC";
    public static final String DATE_TIME_TIMEZONE_FORMAT = "yyyy-MM-dd HH:mm:ss.SSS'Z'";

    public static Instant getInstantFromString(String timeString) {
        timeString = timeString.replace(" ", "T");
        timeString = timeString + ":00Z";

        return Instant.parse(timeString);
    }

    public static Date parseDate(final String dateString, final String format) {
        assert dateString != null : "Date can't be null";
        assert format != null : "Format can't be null";

        try {
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat(format);
            return simpleDateFormat.parse(dateString);
        } catch (ParseException e) {
            throw new RevseedException("There was an issue parsing the date from " + dateString + " with the format "
                    + format, e);
        }
    }

    public static Date buildDateFromDateAndTime(String dateString, String timeString) {
        LocalDate localDate = LocalDate.parse(dateString);
        return buildDateFromDateAndTime(localDate, timeString);
    }

    public static Date buildDateFromDateAndTime(Date date, String timeString) {
        LocalDate localDate = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        return buildDateFromDateAndTime(localDate, timeString);
    }

    private static Date buildDateFromDateAndTime(LocalDate localDate, String timeString) {
        DateTimeFormatter formatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
        ZonedDateTime time = ZonedDateTime.parse(timeString, formatter);
        LocalDateTime dateTime = localDate.atTime(time.toLocalTime());
        return Date.from(dateTime.toInstant(ZoneOffset.UTC));
    }

    public static Date parseDateTime(final String dateString) {
        DateTimeFormatter formatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
        ZonedDateTime dateTime = ZonedDateTime.parse(dateString, formatter);
        return Date.from(dateTime.toInstant());
    }

    public static String formatDate(final Date date, final String format) {
        assert date != null : "Date can't be null";
        assert format != null : "Format can't be null";

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(format);
        return simpleDateFormat.format(date);
    }

    public static Date toDate(final LocalDate localDate) {
        return Date.from(localDate.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant());
    }

    public static Date buildDate(int year, int month, int dayOfMonth) {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.YEAR, year);
        cal.set(Calendar.MONTH, month);
        cal.set(Calendar.DAY_OF_MONTH, dayOfMonth);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }

    public static Date buildDate(int year, int month, int dayOfMonth, int hour, int minute, int second) {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.YEAR, year);
        cal.set(Calendar.MONTH, month);
        cal.set(Calendar.DAY_OF_MONTH, dayOfMonth);
        cal.set(Calendar.HOUR_OF_DAY, hour);
        cal.set(Calendar.MINUTE, minute);
        cal.set(Calendar.SECOND, second);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }

    public static String buildDateWithTimeZone(Date date, String timeZone, final String format) {
        SimpleDateFormat dateFormat = new SimpleDateFormat(format);
        dateFormat.setTimeZone(TimeZone.getTimeZone(timeZone));
        return dateFormat.format(date);
    }

    public static boolean isDateBetweenOrEqualToDates(String beginDate, String endDate, String dateInQuestion, String format){
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format);
        return isDateBetweenOrEqualToDates(LocalDate.parse(beginDate, formatter), LocalDate.parse(endDate, formatter),
                LocalDate.parse(dateInQuestion, formatter)
        );
    }

    public static boolean isDateBetweenOrEqualToDates(LocalDate beginDate, LocalDate endDate, LocalDate dateInQuestion){
        return isDateBetweenDates(beginDate, endDate, dateInQuestion) || dateInQuestion.equals(beginDate) ||
                dateInQuestion.equals(endDate);
    }

    public static boolean isDateBetweenDates(String beginDate, String endDate, String dateInQuestion, String format){
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format);
        return isDateBetweenDates(LocalDate.parse(beginDate, formatter), LocalDate.parse(endDate, formatter),
                LocalDate.parse(dateInQuestion, formatter)
        );
    }

    public static boolean isDateBetweenDates(LocalDate beginDate, LocalDate endDate, LocalDate dateInQuestion){
        return dateInQuestion.isAfter(beginDate) && (dateInQuestion.isBefore(endDate));
    }


    public static String removeTime(XMLGregorianCalendar timeStamp) {
        Calendar calendar = timeStamp.toGregorianCalendar();
        SimpleDateFormat format = new SimpleDateFormat(DATE_FORMAT);
        format.setTimeZone(calendar.getTimeZone());
        return format.format(calendar.getTime());
    }

    private DateTimeUtil() {
    }
}
