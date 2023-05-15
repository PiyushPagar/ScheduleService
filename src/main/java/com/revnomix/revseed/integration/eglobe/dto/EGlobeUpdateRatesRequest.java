package com.revnomix.revseed.integration.eglobe.dto;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EGlobeUpdateRatesRequest {
	
	protected String RoomCode;
	protected String DateFrom;
	protected String DateTill;
	protected List<EGlobeUpdateRatesRequest.RatePlanWiseRates> RatePlanWiseRates;
	
	@Getter
	@Setter
	public static class RatePlanWiseRates {
		protected String RatePlanCode;
		protected Double Rate;
	}

}
