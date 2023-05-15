package com.revnomix.revseed.integration.staahMax.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class RateInventoryMapping {

    protected List<RateInventoryMapping.Data> data;
    protected Integer propertyid;
    protected Double room_id;
    protected Double rate_id;
    protected String currency;
    protected String trackingId;
    protected String version;

    @Getter
    @Setter
    public static class Data {
        protected String date;
        protected Integer inventory;
        protected String stopsell;
        protected String from_date;
        protected String to_date;
        protected RateInventoryMapping.Data.AmountAfterTax amountAfterTax;
        protected RateInventoryMapping.Data.AmountAfterTax amountBeforeTax;
        protected Integer minstay;
        protected Integer maxstay;
        protected String cta;
        protected String ctd;
        
        @Getter
        @Setter
        public static class AmountAfterTax {
        	
            @JsonProperty("Rate")
            protected Double Rate;
            protected Float extraadult;
            protected Float extrachild;
            protected RateInventoryMapping.Data.AmountAfterTax.Obp obp;
            
            @Getter
            @Setter
        	public static class Obp {
                protected Double person1;
                protected Double person2;
                protected Double person3;
                protected Double person4;
            }
        }

        @Getter
        @Setter
        public static class AmountBeforeTax {
        	
            @JsonProperty("Rate")
            protected Double Rate;
            protected Float extraadult;
            protected Float extrachild;
            protected RateInventoryMapping.Data.AmountAfterTax.Obp obp;
            
            @Getter
            @Setter
        	public static class Obp {
                protected Double person1;
                protected Double person2;
                protected Double person3;
            }
        }
        
    }
}
