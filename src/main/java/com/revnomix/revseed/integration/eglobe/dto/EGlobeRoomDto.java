package com.revnomix.revseed.integration.eglobe.dto;

import java.util.List;


import com.fasterxml.jackson.annotation.JsonProperty;
import com.revnomix.revseed.integration.eglobe.dto.EGlobeRoomRateDto;

import lombok.Data;

@Data
public class EGlobeRoomDto {
	
	@JsonProperty("RoomTypeCode") 
    public String roomTypeCode;
    @JsonProperty("MasterRoom") 
    public EGlobeMasterRoomDto masterRoom;
    @JsonProperty("RoomRates") 
    public List<EGlobeRoomRateDto> roomRates;


}
