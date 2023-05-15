package com.revnomix.revseed.integration.staahMax.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RoomMapping {

    protected List<RoomMapping.RoomType> roomtypes;
    protected List<RoomMapping.RatePlans> rateplans;
    protected List<RoomMapping.RoomRateMapping> room_rate_mapping;
    protected String trackingId;
    protected Integer version;

    @Getter
    @Setter
    public static class RoomType {
        protected Long room_id;
        protected String roomname;
        protected String roomtype;
        protected String roomtype_view;
        protected String dormitory;
        protected Integer rackrate;
        protected String description;
        protected Integer default_inventory;
        protected String roomsize;
        protected List<RoomMapping.RoomType.Beeding> bedding;
        protected Integer max_adult;
        protected Integer max_guest;
        protected Integer no_of_child;
        protected Integer total_room;
        protected Long parentRoomID;
        protected String linkRoom;
        protected List<RoomMapping.RoomType.Facilities> facilities;
        protected Long master_rate_id;
        protected RoomMapping.RoomType.ContactInfo contactinfo;
        protected List<RoomMapping.RoomType.RoomImages> room_images;

        @Getter
        @Setter
        public static class ContactInfo {
        	protected String addressline;
            protected String city;
            protected String country;
            protected String email;
            protected String fax;
            protected String latitude;
            protected String location;
            protected String longitude;
            protected String state;
            protected String telephone;
            protected String zip;
        }

        @Getter
        @Setter
        public static class Beeding {
        	protected String id;
            protected String bed_type;
            protected Integer beds;
            @JsonProperty("No_ofBeds")
            protected String no_ofBeds;
            protected String active;
            protected String icon;
        }
        
        @Getter
        @Setter
        public static class Facilities {
            private String amenitytype;
            private List<String> amenities;
        }

        @Getter
        @Setter
        public static class RoomImages {
            private String name;
            private Integer priority;
            private String url;
        }
    }

    @Getter
    @Setter
    public static class RatePlans {
        protected Long rate_id;
        protected String ratename;
        protected String description;
        protected Integer free_night;
        protected String cancellation_policy;
        protected String mealplan;
        protected String default_minimum_night;
        protected Integer default_maximum_stay;
        protected Integer extra_adult_rate;
        protected Integer extra_child_rate;
        protected Integer applicable_guest;
        protected List<RoomMapping.RatePlans.Validity> validity;

        @Getter
        @Setter
        public static class Validity {
        	protected String cancellation_policy_id;
            protected String to_date;
            protected String from_date;
        }
    }

    @Getter
    @Setter
    public static class RoomRateMapping {
        protected Long room_id;
        protected Long rate_id;
        protected String mapping_name;
    }

}
