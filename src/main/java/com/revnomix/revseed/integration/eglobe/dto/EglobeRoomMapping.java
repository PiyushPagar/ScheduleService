package com.revnomix.revseed.integration.eglobe.dto;

import java.util.List;



import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class EglobeRoomMapping {
	
		    @JsonProperty("RoomCode") 
		    public String roomCode;
		    @JsonProperty("RoomName") 
		    public String roomName;
		    @JsonProperty("RatePlans") 
		    public List<RatePlans> ratePlans;
	
}
