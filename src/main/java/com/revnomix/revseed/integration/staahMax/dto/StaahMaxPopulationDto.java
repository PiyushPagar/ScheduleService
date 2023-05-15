package com.revnomix.revseed.integration.staahMax.dto;

import com.revnomix.revseed.model.Clients;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StaahMaxPopulationDto {
    private Clients client;
    private RoomMapping roomMapping;
    private RateInventoryMapping rateInventoryMapping;
    private BookingRequest bookingRequest;
    private DailyBookingResponse dailyBookingResponse;
}
