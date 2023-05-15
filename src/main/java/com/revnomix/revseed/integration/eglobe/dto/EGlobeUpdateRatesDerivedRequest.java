package com.revnomix.revseed.integration.eglobe.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EGlobeUpdateRatesDerivedRequest {

	protected Double BasePrice;
	protected String DateFrom;
	protected String DateTill;
}
