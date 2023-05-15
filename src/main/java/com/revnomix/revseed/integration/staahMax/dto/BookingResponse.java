package com.revnomix.revseed.integration.staahMax.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class BookingResponse {
    protected List<BookingResponse> bookingResponses;
    protected Integer BookingId;
    protected String status;
    protected String error_desc;
    protected String trackingId;

}
