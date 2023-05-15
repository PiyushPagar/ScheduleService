package com.revnomix.revseed.integration.eglobe.dto;

import java.util.List;

import lombok.Data;

@Data
public class EGlobeUpdateInventoryRequest {

	private String DateFrom;
	private String DateTill;
	private List<EGlobeUpdateInventoryRequest.RoomWiseInventory> RoomWiseInventory;
	
	@Data
	public class RoomWiseInventory {
		private String RoomCode;
		private Integer Availability;
	}
}
