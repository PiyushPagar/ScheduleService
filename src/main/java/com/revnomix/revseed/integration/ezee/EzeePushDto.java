package com.revnomix.revseed.integration.ezee;

import com.revnomix.revseed.schema.ezee.PushBookingError;
import com.revnomix.revseed.schema.ezee.PushBookingResponse;
import com.revnomix.revseed.schema.ezee.PushBookingSucess;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EzeePushDto {
	
    	private PushBookingResponse pushBookingResponse;
//	    private PushBookingSucess pushBookingSucess;
//	    private PushBookingError pushBookingError;

}
