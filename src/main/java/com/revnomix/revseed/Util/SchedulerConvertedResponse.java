package com.revnomix.revseed.Util;

import java.util.List;


import com.revnomix.revseed.schema.ezee.RESResponse;
import com.revnomix.revseed.schema.staah.AvailRequestSegments;
import com.revnomix.revseed.schema.staah.RateRequestSegments;
import com.revnomix.revseed.schema.staah.Reservations;
import com.revnomix.revseed.schema.staah.Roomresponse;
import com.revnomix.revseed.integration.eglobe.dto.EGlobeBookingResponse;
import com.revnomix.revseed.integration.eglobe.dto.EglobeRoomMapping;
import com.revnomix.revseed.integration.eglobe.dto.Root;
import com.revnomix.revseed.integration.service.Response;
import com.revnomix.revseed.integration.staahMax.dto.DailyBookingResponse;
import com.revnomix.revseed.integration.staahMax.dto.RateInventoryMapping;
import com.revnomix.revseed.integration.staahMax.dto.RoomMapping;

import lombok.Data;

@Data
public class SchedulerConvertedResponse {

	private List<EglobeRoomMapping> eGlobeRoomMapping;
	private List<EGlobeBookingResponse> eGlobeBooking;
	private Root eGlobeInventory;
	private RoomMapping staahMaxRoomMapping;
	private RateInventoryMapping staahMaxInventory;
	private DailyBookingResponse staahMaxBooking;
	private Roomresponse staahRoomMapping;
	private AvailRequestSegments staahInventory;
	private RateRequestSegments staahRateSync;
	private Reservations staahBooking;
	private RESResponse ezeeRoomMapping;
	private RESResponse ezeeInventory;
	private RESResponse ezeeRateSync;
	private RESResponse ezeeBooking;
	private Response response;
}
