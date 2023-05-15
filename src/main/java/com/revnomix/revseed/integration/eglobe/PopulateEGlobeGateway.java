package com.revnomix.revseed.integration.eglobe;

import org.springframework.integration.annotation.Gateway;
import org.springframework.integration.annotation.MessagingGateway;

import com.revnomix.revseed.integration.eglobe.dto.EGlobePopulationDto;

import static com.revnomix.revseed.integration.configuration.PopulateEGlobeConfig.E_GLOBE_BOOKING_DATA_CHANNEL;
import static com.revnomix.revseed.integration.configuration.PopulateEGlobeConfig.E_GLOBE_GET_RATE_INVENTORY_DATA_CHANNEL;
import static com.revnomix.revseed.integration.configuration.PopulateEGlobeConfig.E_GLOBE_GET_RATE_MAPPING_DATA_CHANNEL;
import static com.revnomix.revseed.integration.configuration.PopulateEGlobeConfig.E_GLOBE_GET_ROOM_MAPPING_DATA_CHANNEL;
import static com.revnomix.revseed.integration.configuration.PopulateEGlobeConfig.E_GLOBE_PUSH_BOOKING_DATA_CHANNEL;


@MessagingGateway
public interface PopulateEGlobeGateway {

    @Gateway(requestChannel = E_GLOBE_GET_RATE_INVENTORY_DATA_CHANNEL)
    void populateEGlobeInventoryAndRate(EGlobePopulationDto staahPopulationDto);

    @Gateway(requestChannel = E_GLOBE_GET_ROOM_MAPPING_DATA_CHANNEL)
    void populateEGlobeRoomMapping(EGlobePopulationDto staahPopulationDto);

    @Gateway(requestChannel = E_GLOBE_GET_RATE_MAPPING_DATA_CHANNEL)
    void populateEGlobeReservation(EGlobePopulationDto staahPopulationDto);

    @Gateway(requestChannel = E_GLOBE_BOOKING_DATA_CHANNEL)
    void populateEGlobeBooking(EGlobePopulationDto staahPopulationDto);
    
    @Gateway(requestChannel = E_GLOBE_PUSH_BOOKING_DATA_CHANNEL)
    void populateEGlobePushBooking(EGlobePopulationDto staahPopulationDto);

}
	
