package com.revnomix.revseed.integration.staahMax.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class RateUpdate {

    protected RateUpdate.UpdateRequest updaterequest;

    @Getter
    @Setter
    public static class UpdateRequest {
        protected String username;
        protected String password;
        protected String version;
        protected Integer hotel_id;
        protected List<RateUpdate.UpdateRequest.Room> room;

        @Getter
        @Setter
        public static class Room {
            protected Long room_id;
            protected List<RateUpdate.UpdateRequest.Room.Rate> rate;

            @Getter
            @Setter
            public static class Rate {
                protected Long rate_id;
                protected List<RateUpdate.UpdateRequest.Room.Rate.Dates> dates;

                @Getter
                @Setter
                public static class Dates {
                    protected String start_date;
                    protected String end_date;
                    protected Integer price;
                }
            }
        }
    }
}
