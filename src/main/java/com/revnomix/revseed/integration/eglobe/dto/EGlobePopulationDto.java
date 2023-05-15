package com.revnomix.revseed.integration.eglobe.dto;

import java.util.List;


import com.revnomix.revseed.model.Clients;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Data
public class EGlobePopulationDto {

	 private Clients client;
	 private List<EglobeRoomMapping> eglobeRoomMappingList;
	 private Root root;
	 private List<EGlobeBookingResponse> eGlobeBookingResponseList;
	 private EGlobeBookingResponse eGlobeBookingResponse;
}
