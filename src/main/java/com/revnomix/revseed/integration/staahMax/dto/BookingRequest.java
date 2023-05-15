package com.revnomix.revseed.integration.staahMax.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class BookingRequest {

    protected Integer propertyid;
    protected String apikey;
    protected String action;
    protected BookingRequest.Reservations reservations;

    @Getter
    @Setter
    public static class Reservations {
        protected List<BookingRequest.Reservations.Reservation> reservation;

        @Getter
        @Setter
        public static class Reservation {
            protected String reservation_datetime;
            protected String reservation_id;
            protected String commissionamount;
            protected String deposit;
            protected String totalamountaftertax;
            protected String totaltax;
            protected Integer payment_required;
            protected String payment_type;
            protected String currencycode;
            protected String status;
            protected String POS;
            protected BookingRequest.Reservations.Reservation.Customer customer;
            protected BookingRequest.Reservations.Reservation.Paymentcarddetail paymentcarddetail;
            protected List<BookingRequest.Reservations.Reservation.Room> room;

            @Getter
            @Setter
            public static class Customer {
                protected String address;
                protected String city;
                protected String country;
                protected String email;
                protected String salutation;
                protected String first_name;
                protected String last_name;
                protected String remarks;
                protected String telephone;
                protected Integer zip;
            }

            @Getter
            @Setter
            public static class Paymentcarddetail {
                protected String CardHolderName;
                protected String CardType;
                protected String ExpireDate;
                protected Long CardNumber;
                protected Integer cvv;
            }

            @Getter
            @Setter
            public static class Room {
                protected String arrival_date;
                protected String departure_date;
                protected String room_id;
                protected String room_name;
                protected String salutation;
                protected String first_name;
                protected String last_name;
                protected List<BookingRequest.Reservations.Reservation.Room.Price> price;
                protected List<BookingRequest.Reservations.Reservation.Room.Taxes> taxes;
                protected List<BookingRequest.Reservations.Reservation.Room.Addons> Addons;
                protected String amountaftertax;
                protected String remarks;
                protected List<BookingRequest.Reservations.Reservation.Room.GuestCount> GuestCount;

                @Getter
                @Setter
                public static class Price {
                    protected String date;
                    protected String rate_id;
                    protected String rate_name;
                    protected String amountaftertax;
                    protected BookingRequest.Reservations.Reservation.Room.Price.ExtraGuest extraGuests;

                    @Getter
                    @Setter
                    public static class ExtraGuest {
                        protected String extraAdult;
                        protected String extraChild;
                        protected String extraAdultRate;
                        protected String extraChildRate;

                    }
                }

                @Getter
                @Setter
                public static class Taxes {
                    protected String name;
                    protected String value;
                }

                @Getter
                @Setter
                public static class Addons {
                    protected String Name;
                    protected String Price;
                    protected String Type;
                    protected String Nights;
                    protected String Persons;
                    protected String Priceperunit;
                    protected String PriceMode;
                }

                @Getter
                @Setter
                public static class GuestCount {
                    protected Integer AgeQualifyingCode;
                    protected Integer Count;
                }
            }
        }

    }
}

