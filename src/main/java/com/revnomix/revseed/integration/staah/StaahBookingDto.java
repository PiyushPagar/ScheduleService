package com.revnomix.revseed.integration.staah;

import com.revnomix.revseed.integration.file.annotation.DelimitedField;

public class StaahBookingDto {
    @DelimitedField(name ="Property Id")
    private String propertyId;
    @DelimitedField(name = "Date/ Time Booked (GMT)")
    private String timeBooked;
    @DelimitedField(name = "Date/ Time Modified (GMT)")
    private String timeModified;
    @DelimitedField(name = "Booking No")
    private String bookingNo;
    @DelimitedField(name = "Channel")
    private String channel;
    @DelimitedField(name = "Channel Ref")
    private String channelRef;
    @DelimitedField(name = "Status")
    private String status;
    @DelimitedField(name = "CheckIn Date")
    private String checkinDate;
    @DelimitedField(name = "CheckOut Date")
    private String checkoutDate;
    @DelimitedField(name = "No Of Rooms")
    private String noOfRooms;
    @DelimitedField(name = "Room Id")
    private String roomId;
    @DelimitedField(name = "Room Type")
    private String roomType;
    @DelimitedField(name = "Rate Id")
    private String rateId;
    @DelimitedField(name = "Rate Plan")
    private String ratePlan;
    @DelimitedField(name = "Currency")
    private String currency;
    @DelimitedField(name = "Rate Value")
    private String rateValue;
    @DelimitedField(name = "Commission")
    private String commision;
    @DelimitedField(name = "Tax Value")
    private String taxValue;
    @DelimitedField(name = "Total Amount")
    private String totalAmount;
    @DelimitedField(name = "Net Amount")
    private String netAmount;
    @DelimitedField(name = "NoExtraAdult")
    private String noOfExtraAdult;
    @DelimitedField(name = "Extra Adult Rate")
    private String extraAdultRate;
    @DelimitedField(name = "NoExtraChild")
    private String noOfChild;
    @DelimitedField(name = "Extra Child Rate")
    private String extraChildRate;
    @DelimitedField(name = "Addon")
    private String addon;
    @DelimitedField(name = "Addon Rate")
    private String addonRate;

    public String getPropertyId() {
        return propertyId;
    }

    public void setPropertyId(String propertyId) {
        this.propertyId = propertyId;
    }

    public String getTimeBooked() {
        return timeBooked;
    }

    public void setTimeBooked(String timeBooked) {
        this.timeBooked = timeBooked;
    }

    public String getTimeModified() {
        return timeModified;
    }

    public void setTimeModified(String timeModified) {
        this.timeModified = timeModified;
    }

    public String getBookingNo() {
        return bookingNo;
    }

    public void setBookingNo(String bookingNo) {
        this.bookingNo = bookingNo;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public String getChannelRef() {
        return channelRef;
    }

    public void setChannelRef(String channelRef) {
        this.channelRef = channelRef;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCheckinDate() {
        return checkinDate;
    }

    public void setCheckinDate(String checkinDate) {
        this.checkinDate = checkinDate;
    }

    public String getCheckoutDate() {
        return checkoutDate;
    }

    public void setCheckoutDate(String checkoutDate) {
        this.checkoutDate = checkoutDate;
    }

    public String getNoOfRooms() {
        return noOfRooms;
    }

    public void setNoOfRooms(String noOfRooms) {
        this.noOfRooms = noOfRooms;
    }

    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    public String getRoomType() {
        return roomType;
    }

    public void setRoomType(String roomType) {
        this.roomType = roomType;
    }

    public String getRateId() {
        return rateId;
    }

    public void setRateId(String rateId) {
        this.rateId = rateId;
    }

    public String getRatePlan() {
        return ratePlan;
    }

    public void setRatePlan(String ratePlan) {
        this.ratePlan = ratePlan;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getRateValue() {
        return rateValue;
    }

    public void setRateValue(String rateValue) {
        this.rateValue = rateValue;
    }

    public String getCommision() {
        return commision;
    }

    public void setCommision(String commision) {
        this.commision = commision;
    }

    public String getTaxValue() {
        return taxValue;
    }

    public void setTaxValue(String taxValue) {
        this.taxValue = taxValue;
    }

    public String getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(String totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getNetAmount() {
        return netAmount;
    }

    public void setNetAmount(String netAmount) {
        this.netAmount = netAmount;
    }

    public String getNoOfExtraAdult() {
        return noOfExtraAdult;
    }

    public void setNoOfExtraAdult(String noOfExtraAdult) {
        this.noOfExtraAdult = noOfExtraAdult;
    }

    public String getExtraAdultRate() {
        return extraAdultRate;
    }

    public void setExtraAdultRate(String extraAdultRate) {
        this.extraAdultRate = extraAdultRate;
    }

    public String getNoOfChild() {
        return noOfChild;
    }

    public void setNoOfChild(String noOfChild) {
        this.noOfChild = noOfChild;
    }

    public String getExtraChildRate() {
        return extraChildRate;
    }

    public void setExtraChildRate(String extraChildRate) {
        this.extraChildRate = extraChildRate;
    }

    public String getAddon() {
        return addon;
    }

    public void setAddon(String addon) {
        this.addon = addon;
    }

    public String getAddonRate() {
        return addonRate;
    }

    public void setAddonRate(String addonRate) {
        this.addonRate = addonRate;
    }

    public boolean isCanceled(){
        return "B".equalsIgnoreCase(getStatus());
    }

    public boolean isConfirmed(){
        return "C".equalsIgnoreCase(getStatus());
    }

    public boolean isModified(){
        return "M".equalsIgnoreCase(getStatus());
    }
}
