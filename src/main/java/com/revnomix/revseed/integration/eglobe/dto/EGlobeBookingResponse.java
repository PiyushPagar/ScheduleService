package com.revnomix.revseed.integration.eglobe.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
@JsonIgnoreProperties
public class EGlobeBookingResponse {
	
    @JsonProperty("ChannelId") 
    public Integer channelId;
    @JsonProperty("ChannelName") 
    public String channelName;
    @JsonProperty("BookingCode") 
    public String bookingCode;
    @JsonProperty("CheckIn") 
    public String checkIn;
    @JsonProperty("CheckOut") 
    public String checkOut;
    @JsonProperty("NumNights") 
    public Integer numNights;
    @JsonProperty("NumRooms") 
    public Integer numRooms;
    @JsonProperty("Rooms") 
    public List<EGlobeRoomDto> rooms;
    @JsonProperty("BookingStatus") 
    public String bookingStatus;
    @JsonProperty("DateBooked") 
    public String dateBooked;
    @JsonProperty("CustomerName") 
    public String customerName;
    @JsonProperty("CustomerEmail") 
    public String customerEmail;
    @JsonProperty("CustomerMobile") 
    public String customerMobile;
    @JsonProperty("CustomerSpecialRequest") 
    public Object customerSpecialRequest;
    @JsonProperty("TotalAdults") 
    public Integer totalAdults;
    @JsonProperty("TotalChildren") 
    public Integer totalChildren;
    @JsonProperty("BilledAmount") 
    public Double billedAmount;
    @JsonProperty("TaxAmount") 
    public Double taxAmount;
    @JsonProperty("PaymentType") 
    public String paymentType;
    @JsonProperty("BookingDetailUrl") 
    public String bookingDetailUrl;
    @JsonProperty("BookingId") 
    public Integer bookingId;
    @JsonProperty("BookingWaitListedType")
    public Object bookingWaitListedType;
    @JsonProperty("IsWaitListedBooking")
    public Object isWaitListedBooking;
    @JsonProperty("BookingSource")
    public String bookingSource;
}
