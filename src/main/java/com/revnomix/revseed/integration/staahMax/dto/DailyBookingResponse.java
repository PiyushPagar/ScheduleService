package com.revnomix.revseed.integration.staahMax.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class DailyBookingResponse {

    @JsonProperty("Reservations")
    protected List<DailyBookingResponse.Reservations> reservations;

    @Getter
    @Setter
    public static class Reservations {

        protected String checkin_at;
        protected String checkout_at;
        protected String company;
        protected String booking_source;
        protected String reservation_code;
        protected Integer property_id;
        protected String last_name;
        protected String email;
        protected String status;
        protected Integer room_count;
        protected String first_name;
        protected String birthday;
        protected String phone_number;
        protected String language;
        protected String market_code;
        protected String salutation;
        protected String travel_agent;
        protected String meal_plan_code;
        protected String nationality;
        protected String date;
        protected Double totalprice;
        protected String currency_code;
        protected List<DailyBookingResponse.Reservations.RoomInfo> room_info;

        @Getter
        @Setter
        public static class RoomInfo {

            protected Integer room_number;
            protected Integer number_adults;
            protected String room_type;
            protected Double totalprice;
            protected Integer number_children;
            protected List<DailyBookingResponse.Reservations.RoomInfo.Price> price;

            @Getter
            @Setter
            public static class Price {

                protected String rate_code;
                protected String date;
                protected Integer rate_code_group;
                protected Float amount;

            }
        }

    }
}
