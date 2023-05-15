package com.revnomix.revseed.integration.staah.transformer;

import com.revnomix.revseed.model.Clients;
import com.revnomix.revseed.schema.staah.AvailRequestSegments;
import com.revnomix.revseed.schema.staah.AvailResponseType;
import com.revnomix.revseed.schema.staah.RateRequestSegments;
import com.revnomix.revseed.schema.staah.Reservations;
import com.revnomix.revseed.schema.staah.Roomresponse;


public class StaahPopulationDto {
    private Clients client;
    private Roomresponse roomresponse;
    private AvailResponseType availResponseType;
    private Reservations reservations;
    private RateRequestSegments rateRequestSegmentsResponse;
    private AvailRequestSegments availRequestSegments;

    public StaahPopulationDto(Clients client, Roomresponse roomresponse) {
        this.client = client;
        this.roomresponse = roomresponse;
    }

    public StaahPopulationDto() {

    }

    public RateRequestSegments getRateRequestSegmentsResponse() {
        return rateRequestSegmentsResponse;
    }

    public void setRateRequestSegmentsResponse(RateRequestSegments rateRequestSegmentsResponse) {
        this.rateRequestSegmentsResponse = rateRequestSegmentsResponse;
    }

    public Reservations getReservations() {
        return reservations;
    }

    public void setReservations(Reservations reservations) {
        this.reservations = reservations;
    }

    public Clients getClient() {
        return client;
    }

    public void setClient(Clients client) {
        this.client = client;
    }

    public Roomresponse getRoomresponse() {
        return roomresponse;
    }

    public void setRoomresponse(Roomresponse roomresponse) {
        this.roomresponse = roomresponse;
    }

    public AvailResponseType getAvailResponseType() {
        return availResponseType;
    }

    public void setAvailResponseType(AvailResponseType availResponseType) {
        this.availResponseType = availResponseType;
    }

	public AvailRequestSegments getAvailRequestSegments() {
		return availRequestSegments;
	}

	public void setAvailRequestSegments(AvailRequestSegments availRequestSegments) {
		this.availRequestSegments = availRequestSegments;
	}
    
    
}
