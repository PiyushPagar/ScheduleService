package com.revnomix.revseed.integration.staahMax.gateway;

import static com.revnomix.revseed.integration.configuration.PopulateStaahMaxConfig.STAAH_MAX_GET_RATE_INVENTORY_DATA_CHANNEL;
import static com.revnomix.revseed.integration.configuration.PopulateStaahMaxConfig.STAAH_MAX_GET_ROOM_MAPPING_DATA_CHANNEL;
import static com.revnomix.revseed.integration.configuration.PopulateStaahMaxConfig.STAAH_MAX_PUSH_BOOKING_DATA_CHANNEL;
import static com.revnomix.revseed.integration.configuration.PopulateStaahMaxConfig.STAAH_MAX_RESERVATION_DATA_CHANNEL;
import static com.revnomix.revseed.integration.configuration.PopulateStaahMaxConfig.STAAH_MAX_BOOKING_DATA_CHANNEL;

import java.util.List;

import org.springframework.integration.annotation.Gateway;
import org.springframework.integration.annotation.MessagingGateway;

import com.revnomix.revseed.integration.staahMax.dto.StaahMaxPopulationDto;

@MessagingGateway
public interface PopulateStaahMaxGateway {

	@Gateway(requestChannel = STAAH_MAX_GET_RATE_INVENTORY_DATA_CHANNEL)
	void populateStaahMaxInventoryAndRate(List<StaahMaxPopulationDto> staahPopulationDto);

	@Gateway(requestChannel = STAAH_MAX_GET_ROOM_MAPPING_DATA_CHANNEL)
	void populateStaahMaxRoomMapping(StaahMaxPopulationDto staahPopulationDto);

	@Gateway(requestChannel = STAAH_MAX_RESERVATION_DATA_CHANNEL)
	void populateStaahMaxReservation(StaahMaxPopulationDto staahPopulationDto);

	@Gateway(requestChannel = STAAH_MAX_BOOKING_DATA_CHANNEL)
	void populateStaahMaxBooking(StaahMaxPopulationDto staahPopulationDto);

	@Gateway(requestChannel = STAAH_MAX_PUSH_BOOKING_DATA_CHANNEL)
	void populateStaahMaxPushBooking(StaahMaxPopulationDto staahPopulationDto);

}
