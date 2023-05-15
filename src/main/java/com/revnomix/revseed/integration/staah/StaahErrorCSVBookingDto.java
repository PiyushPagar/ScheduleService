package com.revnomix.revseed.integration.staah;

import org.apache.camel.dataformat.bindy.annotation.CsvRecord;
import org.apache.camel.dataformat.bindy.annotation.DataField;

@CsvRecord(separator = ",", skipFirstLine = false,allowEmptyStream = true)
public class StaahErrorCSVBookingDto {


	@DataField(pos =1)
    private String propertyId;
	@DataField(pos =2)
    private String timeBooked;
	@DataField(pos =3)
    private String timeModified;
    @DataField(pos =4)
    private String bookingNo;
    @DataField(pos =5)
    private String channel;
    @DataField(pos =6)
    private String channelRef;
    @DataField(pos =7)
    private String status;
    @DataField(pos =8)
    private String checkinDate;
    @DataField(pos =9)
    private String checkoutDate;
    @DataField(pos =10)
    private String noOfRooms;
    @DataField(pos =11)
    private String roomId;
    @DataField(pos =12)
    private String roomType;
    @DataField(pos =13)
    private String rateId;
    @DataField(pos =14)
    private String ratePlan;
    @DataField(pos =15)
    private String currency;
    @DataField(pos =16)
    private String rateValue;
    @DataField(pos =17)
    private String commision;
    @DataField(pos =18)
    private String taxValue;
    @DataField(pos =19)
    private String totalAmount;
    @DataField(pos =20)
    private String netAmount;
    @DataField(pos =21)
    private String noOfExtraAdult;
    @DataField(pos =22)
    private String extraAdultRate;
    @DataField(pos =23)
    private String noOfChild;
    @DataField(pos =24)
    private String extraChildRate;
    @DataField(pos =25)
    private String addon;
    @DataField(pos =26)
    private String addonRate;
    @DataField(pos =27)
    private String error;
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
	public String getError() {
		return error;
	}
	public void setError(String error) {
		this.error = error;
	}


}
