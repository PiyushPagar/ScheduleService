package com.revnomix.revseed.integration.staahMax.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DailyBookingRequest {

    @JsonProperty("HotelCode")
    protected Integer hotelCode;
    @JsonProperty("CheckIn_At")
    protected DailyBookingRequest.CheckIn checkIn_At;

    @Getter
    @Setter
    public static class CheckIn {

        @JsonProperty("Start")
        protected String start;
        @JsonProperty("End")
        protected String end;
    }
}