package com.revnomix.revseed.integration.staahMax.transformer;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.IOException;
import java.io.StringReader;
import java.util.List;

public class Test {

    public static void main(String[] args) throws IOException {
        String DATA = "435139,305057,7,2018-09-14,2019-02-27,1,1,\"Junior Suite with Breakfast\",3024.00,4199.00,INR,\"The Taj Mahal Hotel, Abids, boasts of magnificent Suites and Rooms, which are unique by themselves and has been carefully guarded through the years to give a timeless feel. Be it a leisure or a business trip, the hotel promises a sumptuously indulgent stay because of its spacious suites and rooms. As a guest, you can be sure of being satisfied with our in room facilities and guest services.roomsize:4.27x3.66 sq.mtr bedtype:queen\",\"https://www.goibibo.com/hotels/taj-mahal-hotel-in-hyderabad-7252755828165789804/?hquery=%7B\"ci\"\",\"[FREE Breakfast, WiFi, Accommodation, TV, Newspaper, Bathroom, Geyser in the Bathroom, Hot/cold Water]\",\"Non Refundable\",3,Y,N,2019-02-27,2019-02-28,27.98,\"\",7,1,6,0.00,2,\"Not Included\",\"BreakFast\",\"200\"";
        CSVParser csvParser =
                CSVFormat.newFormat(',')
                        .withIgnoreEmptyLines()
                        .withIgnoreHeaderCase()
                        .withRecordSeparator('\n').withQuote('"')
                        .withTrim()
                        .parse(new StringReader(DATA));
        List<CSVRecord> records = csvParser.getRecords();
        System.out.println(records);
    }
}

