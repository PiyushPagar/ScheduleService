package com.revnomix.revseed.integration.staahMax.dto;

import java.util.List;

import com.revnomix.revseed.model.Clients;
import com.revnomix.revseed.schema.ezee.RESRequest;
import com.revnomix.revseed.schema.ezee.RESResponse;
import com.revnomix.revseed.integration.eglobe.dto.EGlobeBookingResponse;
import com.revnomix.revseed.integration.eglobe.dto.EglobeRoomMapping;
import com.revnomix.revseed.integration.eglobe.dto.Root;
import com.revnomix.revseed.integration.staah.StaahBookingDto;
import com.revnomix.revseed.integration.staah.transformer.StaahPopulationDto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GenericIntegrationDto {
	//COMMON
	private String integerationType;
	private Clients client;
	//SAAHALL
	private List<StaahMaxPopulationDto> list;
	private RoomMapping roomMapping;
	private RateInventoryMapping rateInventoryMapping;
	private BookingRequest bookingRequest;
	private DailyBookingResponse dailyBookingResponse;
	//EZEE
	private RESResponse resResponse;
	private RESRequest resRequest;
	//STAAH
	private List<StaahBookingDto> staahBookingDtos; 
	private StaahPopulationDto staahPopulationDto;
	//EGLOBE
	private List<EglobeRoomMapping> eglobeRoomMappingList;
	private Root root;
	private List<EGlobeBookingResponse> eGlobeBookingResponseList;
	private EGlobeBookingResponse eGlobeBookingResponse;
}
