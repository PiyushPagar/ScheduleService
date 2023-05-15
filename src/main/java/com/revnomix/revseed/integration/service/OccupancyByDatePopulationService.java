package com.revnomix.revseed.integration.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import com.revnomix.revseed.model.Bookings;
import com.revnomix.revseed.model.OccupancyByDate;
import com.revnomix.revseed.repository.OccupancyByDateRepository;
import com.revnomix.revseed.Util.DateUtil;

@Controller
public class OccupancyByDatePopulationService {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private OccupancyByDateRepository occupancyByDateRepository;

    public List<OccupancyByDate> updateOrInsertOccupancyByDate(Bookings bookings, Double diffNetAmount, Integer diffNoOfRooms) {
        List<OccupancyByDate> occupancyByDates = new ArrayList<>();
        for (int i = -1; i < bookings.getLos(); i++) {
        	try {
	            Date occupancyDate = DateUtil.localToDate(DateUtil.dateToLocalDate(bookings.getCheckinDate()).plusDays(i + 1));
	            OccupancyByDate occupancyByDate = null;
	            if(DateUtil.toDate("2019-08-28","yyyy-MM-dd").compareTo(DateUtil.setTimeToZero(occupancyDate))==0){
	                System.out.println();
	            }
	            occupancyByDate = occupancyByDateRepository.findByClientIdAndOccupancyDateAndOtaIdAndCmRoomId(bookings.getClientId(),occupancyDate, bookings.getOtaId(), bookings.getCmRoomId() );
	            if (occupancyByDate == null) {
	                occupancyByDate = new OccupancyByDate();
	            }
	            occupancyByDate.setClientId(bookings.getClientId());
	            occupancyByDate.setHotelId(bookings.getHotelId());
	            occupancyByDate.setCmRoomId(bookings.getCmRoomId());
	            if(bookings.getOtaId() != null){
	                occupancyByDate.setOtaId(bookings.getOtaId());
	            }
	            occupancyByDate.setOccupancyDate(occupancyDate);
	            if (bookings.getCmStatus().equalsIgnoreCase("B") ||
	                    bookings.getCmStatus().equalsIgnoreCase("cancel")) {
	                cancelledBooking(bookings, occupancyDate, occupancyByDate);
	            }else  if(bookings.getCmStatus().equalsIgnoreCase("M")){
	                modifyBooking(bookings, occupancyDate, occupancyByDate,diffNetAmount,diffNoOfRooms);
	            } else {
	                confirmBooking(bookings, occupancyDate, occupancyByDate);
	            }
	            occupancyByDate.setLastUpdate(new Date());
	            occupancyByDates.add(occupancyByDateRepository.save(occupancyByDate));
        	}catch(Exception ce) {
        		ce.printStackTrace();
        	}
        }
        return occupancyByDates;
    }

    private void confirmBooking(Bookings bookings, Date occupancyDate, OccupancyByDate occupancyByDate) {
        if ((compareTo(occupancyDate,bookings.getCheckinDate())>= 0 ) && (compareTo(occupancyDate,bookings.getCheckoutDate())< 0)){
            occupancyByDate.setNoOfRooms(checkIntNull(occupancyByDate.getNoOfRooms()) + bookings.getNoOfRooms());
            double netAmount = bookings.getNetAmount();
            if (bookings.getLos() > 0) {
                netAmount = bookings.getNetAmount() / bookings.getLos();
            }
            occupancyByDate.setNetAmount(checkDoubleNull(occupancyByDate.getNetAmount()) + netAmount);

            if (occupancyByDate.getNoOfRooms() > 0) {
                occupancyByDate.setAdr(checkDoubleNull(occupancyByDate.getNetAmount()) / checkIntNull(occupancyByDate.getNoOfRooms()));
            }else{
                occupancyByDate.setAdr(checkDoubleNull(occupancyByDate.getNetAmount()));
            }
        }
        if (DateUtil.setTimeToZero(occupancyDate).compareTo(bookings.getCheckinDate()) == 0) {
            occupancyByDate.setArrivals(checkIntNull(occupancyByDate.getArrivals()) + bookings.getNoOfRooms());
        }
        if (DateUtil.setTimeToZero(occupancyDate).compareTo(bookings.getCheckoutDate()) == 0) {
            occupancyByDate.setDepartures(checkIntNull(occupancyByDate.getDepartures()) + bookings.getNoOfRooms());
        }
    }

    private int compareTo(Date dateOne, Date dateTwo) {
       return DateUtil.setTimeToZero(dateOne).compareTo(dateTwo);
    }

    private void modifyBooking(Bookings bookings, Date occupancyDate, OccupancyByDate occupancyByDate, Double diffNetAmount, Integer diffNoOfRooms) {

        if ((compareTo(occupancyDate,bookings.getCheckinDate())>= 0 ) && (compareTo(occupancyDate,bookings.getCheckoutDate())< 0)){
            occupancyByDate.setNoOfRooms(occupancyByDate.getNoOfRooms() - diffNoOfRooms);
            occupancyByDate.setNetAmount(occupancyByDate.getNetAmount() - diffNetAmount);

            if (occupancyByDate.getNoOfRooms() > 0) {
                occupancyByDate.setAdr(occupancyByDate.getNetAmount() / occupancyByDate.getNoOfRooms());
            }else{
                occupancyByDate.setAdr(occupancyByDate.getNetAmount());
            }
            if (occupancyDate.compareTo(bookings.getCheckinDate()) == 0) {
                occupancyByDate.setArrivals(occupancyByDate.getArrivals() - diffNoOfRooms);
            }
        }

        if (DateUtil.setTimeToZero(occupancyDate).compareTo(bookings.getCheckoutDate()) == 0) {
            occupancyByDate.setDepartures(occupancyByDate.getDepartures() - diffNoOfRooms);
        }
    }

    private void cancelledBooking(Bookings bookings, Date occupancyDate, OccupancyByDate occupancyByDate) {
        if ((compareTo(occupancyDate,bookings.getCheckinDate())>= 0 ) && (compareTo(occupancyDate,bookings.getCheckoutDate())< 0)) {
            occupancyByDate.setNoOfRooms(occupancyByDate.getNoOfRooms() - bookings.getNoOfRooms());
            occupancyByDate.setNetAmount(occupancyByDate.getNetAmount() - bookings.getNetAmount());
            if (occupancyByDate.getNoOfRooms() > 0) {
                occupancyByDate.setAdr(occupancyByDate.getNetAmount() / occupancyByDate.getNoOfRooms());
            } else {
                occupancyByDate.setAdr(occupancyByDate.getNetAmount());
            }
            if (occupancyDate.compareTo(bookings.getCheckinDate()) == 0) {
                occupancyByDate.setArrivals(occupancyByDate.getArrivals() - bookings.getNoOfRooms());
            }
        }
        if (occupancyDate.compareTo(bookings.getCheckoutDate()) == 0) {
            occupancyByDate.setDepartures(occupancyByDate.getDepartures() - bookings.getNoOfRooms());
        }
    }
    private Integer checkIntNull(Integer value){
        return value == null ? 0 : value;
    }
    private Double checkDoubleNull(Double value){
        return value == null ? 0.0 : value;
    }
}
