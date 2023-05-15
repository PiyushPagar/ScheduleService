package com.revnomix.revseed.integration.ezee.gateway;


import static com.revnomix.revseed.integration.configuration.PopulateEzeeConfig.*;

import org.springframework.integration.annotation.Gateway;
import org.springframework.integration.annotation.MessagingGateway;

import com.revnomix.revseed.integration.ezee.EzeePopulationDto;

@MessagingGateway
public interface PopulateEseeGateway {

    @Gateway(requestChannel = EZEE_GET_ROOM_MAPPING_DATA_CHANNEL)
    void populateEzeeRoomMapping(EzeePopulationDto ezeePopulationDto);

    @Gateway(requestChannel = EZEE_GET_ROOM_INFO_CHANNEL)
    void populateEzeeRoomInfo(EzeePopulationDto ezeePopulationDto);

    @Gateway(requestChannel = EZEE_GET_INVENTORY_CHANNEL)
    void populateEzeeInventory(EzeePopulationDto ezeePopulationDto);

    @Gateway(requestChannel = EZEE_GET_INVENTORY_UPDATE_CHANNEL)
    void populateEzeeInventoryUpdate(EzeePopulationDto ezeePopulationDto);

    @Gateway(requestChannel = EZEE_GET_BOOKINGS_CHANNEL)
    void populateEzeeBookings(EzeePopulationDto ezeePopulationDto);

    @Gateway(requestChannel = EZEE_GET_RATE_CHANNEL)
    void populateEzeeRate(EzeePopulationDto ezeePopulationDto);

    @Gateway(requestChannel = EZEE_GET_RATE_UPDATE_CHANNEL)
    void populateEzeeRateUpdate(EzeePopulationDto ezeePopulationDto);
}
