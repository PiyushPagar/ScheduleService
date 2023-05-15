package com.revnomix.revseed.integration.staah.gateway;

import static com.revnomix.revseed.integration.configuration.PopulateStaahConfig.*;
import static org.springframework.integration.context.IntegrationContextUtils.ERROR_CHANNEL_BEAN_NAME;

import org.springframework.integration.annotation.Gateway;
import org.springframework.integration.annotation.MessagingGateway;

import com.revnomix.revseed.integration.staah.transformer.StaahPopulationDto;

@MessagingGateway
public interface PopulateStaahGateway {

    @Gateway(requestChannel = STAAH_GET_RATE_INVENTORY_DATA_CHANNEL)
    void populateStahInventory(StaahPopulationDto staahPopulationDto);

    @Gateway(requestChannel = STAAH_GET_RATE_DATA_CHANNEL)
    void populateStahRate(StaahPopulationDto staahPopulationDto);

    @Gateway(requestChannel = STAAH_GET_ROOM_MAPPING_DATA_CHANNEL)
    void populateStaahRoomMapping(StaahPopulationDto staahPopulationDto);

    @Gateway(requestChannel = STAAH_RESERVATION_DATA_CHANNEL)
    void populateStaahReservation(StaahPopulationDto staahPopulationDto);

    @Gateway(requestChannel = ERROR_CHANNEL_BEAN_NAME)
    void errorWhileFetchingData();
}
