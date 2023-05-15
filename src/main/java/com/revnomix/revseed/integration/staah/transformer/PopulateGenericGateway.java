package com.revnomix.revseed.integration.staah.transformer;

import static com.revnomix.revseed.integration.configuration.PopulateGenericConfig.GENERIC_BOOKING_DATA_CHANNEL;
import static com.revnomix.revseed.integration.configuration.PopulateGenericConfig.GENERIC_GET_RATE_INVENTORY_DATA_CHANNEL;
import static com.revnomix.revseed.integration.configuration.PopulateGenericConfig.GENERIC_GET_RATE_MAPPING_DATA_CHANNEL;
import static com.revnomix.revseed.integration.configuration.PopulateGenericConfig.GENERIC_GET_ROOM_MAPPING_DATA_CHANNEL;
import static com.revnomix.revseed.integration.configuration.PopulateGenericConfig.GENERIC_PUSH_BOOKING_DATA_CHANNEL;

import org.springframework.integration.annotation.Gateway;
import org.springframework.integration.annotation.MessagingGateway;

import com.revnomix.revseed.integration.staahMax.dto.GenericIntegrationDto;
@MessagingGateway
public interface PopulateGenericGateway {

    @Gateway(requestChannel = GENERIC_GET_RATE_INVENTORY_DATA_CHANNEL)
    void populateGenericInventoryAndRate(GenericIntegrationDto genericPopulationDto);

    @Gateway(requestChannel = GENERIC_GET_ROOM_MAPPING_DATA_CHANNEL)
    void populateGenericRoomMapping(GenericIntegrationDto genericPopulationDto);

    @Gateway(requestChannel = GENERIC_GET_RATE_MAPPING_DATA_CHANNEL)
    void populateGenericRateMapping(GenericIntegrationDto genericPopulationDto);

    @Gateway(requestChannel = GENERIC_BOOKING_DATA_CHANNEL)
    void populateGenericBooking(GenericIntegrationDto genericPopulationDto);
    
    @Gateway(requestChannel = GENERIC_PUSH_BOOKING_DATA_CHANNEL)
    void populateGenericPushBooking(GenericIntegrationDto genericPopulationDto);

}
