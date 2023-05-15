package com.revnomix.revseed.integration.eglobe.transformer;

import java.util.ArrayList;

import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.transformer.GenericTransformer;
import org.springframework.stereotype.Component;

import com.revnomix.revseed.model.BookingPaceOccupancyByDate;
import com.revnomix.revseed.model.BookingStatusMapping;
import com.revnomix.revseed.model.Bookings;
import com.revnomix.revseed.model.Clients;
import com.revnomix.revseed.model.OtaMappings;
import com.revnomix.revseed.model.Parameter;
import com.revnomix.revseed.repository.BookingPaceOccupancyByDateRepository;
import com.revnomix.revseed.repository.BookingStatusMappingRepository;
import com.revnomix.revseed.repository.BookingsRepository;
import com.revnomix.revseed.repository.ClientsRepository;
import com.revnomix.revseed.repository.OtaMappingsRepository;
import com.revnomix.revseed.repository.ParameterRepository;
import com.revnomix.revseed.Util.ConstantUtil;
import com.revnomix.revseed.Util.DateUtil;
import com.revnomix.revseed.integration.eglobe.dto.EGlobeBookingResponse;
import com.revnomix.revseed.integration.eglobe.dto.EGlobePopulationDto;
import com.revnomix.revseed.integration.eglobe.dto.EGlobeRoomDto;
import com.revnomix.revseed.integration.integration.IntegrationType;
import com.revnomix.revseed.integration.service.OccupancyByDatePopulationService;


@Component
public class EGlobeBookingTransformer implements GenericTransformer<EGlobePopulationDto, List<Bookings>> {

    @Autowired
    private OtaMappingsRepository otaMappingsRepository;

    @Autowired
    private BookingsRepository bookingsRepository;

    @Autowired
    private ClientsRepository clientsRepository;

    @Autowired
    private OccupancyByDatePopulationService occupancyByDatePopulationService;

    @Autowired
    private BookingPaceOccupancyByDateRepository bookingPaceOccupancyByDateRepository;
    
    @Autowired
    private ParameterRepository parameterRepository;
    
    @Autowired
    private BookingStatusMappingRepository bookingStatusMappingRepository;

    @SuppressWarnings("deprecation")
	@Override
    public List<Bookings> transform(EGlobePopulationDto eGlobePopulationDto) {

        List<Bookings> bookingsList = new ArrayList<>();
    	try {
        Date today = new Date();
        List<EGlobeBookingResponse> eglobeBookingReponseList = eGlobePopulationDto.getEGlobeBookingResponseList();       
        for(EGlobeBookingResponse eGlobeBookingResponse : eglobeBookingReponseList) {
        	List<Bookings> bookings = bookingsRepository.findAllByBookingNoAndClientId(eGlobeBookingResponse.getBookingCode(),eGlobePopulationDto.getClient().getId());
            if (bookings != null && !bookings.isEmpty()) {
                bookings.forEach(b->{
                    bookingPaceOccupancyByDateRepository.deleteByBookingIdAndClientId(b.getId(),b.getClientId());
                });
                bookingsRepository.deleteByBookingNo(eGlobeBookingResponse.getBookingCode());
            }
        }      
        for (EGlobeBookingResponse reservation : eglobeBookingReponseList) {
        	if(reservation.getRooms()!=null) {
        	    EGlobeRoomDto room = reservation.getRooms().get(0);
                Bookings bookings = bookingsRepository.findByBookingNoAndClientId(reservation.getBookingCode(),eGlobePopulationDto.getClient().getId());
                if (bookings == null) {
                    bookings = new Bookings();
                }
                Date bookingDate = DateUtil.toDateByAllFormat(reservation.getDateBooked(), ConstantUtil.SPACEDDMMMYYYYHHMMA);
                Date bookingDatePlusOne = DateUtil.addDays(bookingDate,(1));
                Date checkinDate = DateUtil.toDateByAllFormat(reservation.getCheckIn(), ConstantUtil.SPACEDDMMMYYYY);
                bookings.setBookingNo(reservation.getBookingCode());
                bookings.setClientId(eGlobePopulationDto.getClient().getId());
                bookings.setHotelId(eGlobePopulationDto.getClient().getHotelId());
                bookings.setCmId(eGlobePopulationDto.getClient().getCmHotel());
                bookings.setChannelManager(IntegrationType.EGLOBE.name());
                bookings.setBookingDateTime(bookingDate);
                if(bookingDatePlusOne.before(today) && (bookingDatePlusOne.before(checkinDate)||bookingDatePlusOne.equals(checkinDate)) 
                		&& (ConstantUtil.CANCELLED_STATUS.contains(reservation.getBookingStatus()) || ConstantUtil.MODIFIED_STATUS.contains(reservation.getBookingStatus()))) {
                	bookings.setModDateTime(bookingDatePlusOne);
                }else {
                	bookings.setModDateTime(bookingDate);
                }
                if(reservation.getChannelName()!=null && !reservation.getChannelName().trim().equals("")) {
	                bookings.setChannelString(reservation.getChannelName());
                }else {
                	 bookings.setChannelString(ConstantUtil.WEBDIRECT);
                }               
                if(reservation.getBookingCode()!=null && !reservation.getBookingCode().trim().equals("")) {
	                bookings.setChannelRef(reservation.getBookingCode());
                }else {
 	                bookings.setChannelRef("");
                }
                bookings.setCmStatus(reservation.getBookingStatus());
                List<BookingStatusMapping> bookingStatusMappingList = bookingStatusMappingRepository.findByBookingStatusAndChannelManager(reservation.getBookingStatus(),IntegrationType.EGLOBE.toString());
                if(bookingStatusMappingList!=null && bookingStatusMappingList.size()<1) {
                	BookingStatusMapping bookingStatusMapping = new BookingStatusMapping();
                	bookingStatusMapping.setBookingStatus(reservation.getBookingStatus());
                	bookingStatusMapping.setChannelManager(IntegrationType.EGLOBE.toString());
                	bookingStatusMapping.setStatus(ConstantUtil.ACTIVE);
                	bookingStatusMappingRepository.save(bookingStatusMapping);
                }
                OtaMappings otas = getOtas(reservation, eGlobePopulationDto.getClient());
                bookings.setOtaId(otas.getId());
                bookings.setCheckinDate(checkinDate);
                bookings.setCheckoutDate(DateUtil.toDateByAllFormat(reservation.getCheckOut(),ConstantUtil.SPACEDDMMMYYYY));
                bookings.setLos(getLos(bookings.getCheckoutDate(),bookings.getCheckinDate()));
                bookings.setNoOfRooms(reservation.getRooms().size()); //by default 1 and sum of multiple booking
                bookings.setCmRoomId(Long.valueOf(room.getRoomTypeCode())); //by default 1 and sum of multiple booking
                //bookings.setCmRoomTypeString(room.getRoom_type()); //by default 1 and sum of multiple booking
                bookings.setRateId(Long.parseLong(room.getRoomRates().get(0).getRatePlanCode()));
                //bookings.setRatePlan(room.getPrice().get(0).getRate_code_group().longValue());
                Parameter paramOptCurrency =parameterRepository.findByClientIdAndParamName(eGlobePopulationDto.getClient().getId(), ConstantUtil.CURRENCY).orElse(null);
                if(paramOptCurrency!=null) {
                	bookings.setCurrency(paramOptCurrency.getParamValue());
                }               
                double billedAmount = reservation.getBilledAmount();
                bookings.setRateValue(new Double(billedAmount).floatValue());
                bookings.setPriceDate(bookingDate);
                bookings.setTotalAmount(billedAmount);
                bookings.setNetAmount(billedAmount);
                bookings.setTotalAdults(reservation.getTotalAdults());
                bookings.setTotalChildren(reservation.getTotalChildren());
                bookings.setRegdateRate(today);
                bookings.setRegDate(today);
                bookings.setStatus(ConstantUtil.ACTIVE);
                occupancyByDatePopulationService.updateOrInsertOccupancyByDate(bookings, bookings.getNetAmount(), bookings.getNoOfRooms());
                Bookings save = bookingsRepository.save(bookings);
                saveBookingPaceRecord(save);
                bookingsList.add(bookings);
        	}
        }
        Clients client = eGlobePopulationDto.getClient();
        client.setSystemToday(DateUtil.setTimeToZero(new Date()));
        clientsRepository.save(client);
    	}catch(Exception ce) {
    		ce.printStackTrace();
    	}
        return bookingsList;
    }

    private Integer getLos(Date checkoutDate, Date checkInDate) {
        long difference = checkoutDate.getTime() - checkInDate.getTime();
        return Math.toIntExact((difference / (1000 * 60 * 60 * 24)));
    }
    private OtaMappings getOtas(EGlobeBookingResponse reservation, Clients clients) {
        if (reservation.getChannelName() == null || reservation.getChannelName().equals("-1") || reservation.getChannelName().trim().equals("")) {
            reservation.setChannelName("WEBDIRECT");
        }
        OtaMappings otaMappings = otaMappingsRepository.findFirstByClientIdAndTypeAndClientOta(clients.getId(), ConstantUtil.CHANNELMAN, reservation.getChannelName().toLowerCase());
        if (otaMappings == null) {
            otaMappings = new OtaMappings();
            otaMappings.setClientOta(reservation.getChannelName());
            otaMappings.setClientId(clients.getId());
            otaMappings.setOtaId(1);
            otaMappings.setRegdate(new Date());
            otaMappings.setType(ConstantUtil.CHANNELMAN);
            otaMappings = otaMappingsRepository.save(otaMappings);
        }
        return otaMappings;
    }

    private void saveBookingPaceRecord(Bookings bookings) {
        for (int i = 0; i <= bookings.getLos(); i++) {
            Date occupancyDate = DateUtil.addDays(bookings.getCheckinDate(), i);
            BookingPaceOccupancyByDate paceOccupancyByDate = bookingPaceOccupancyByDateRepository.findByBookingIdAndOccupancyDate(bookings.getId(), occupancyDate);
            if (paceOccupancyByDate == null) {
                paceOccupancyByDate = new BookingPaceOccupancyByDate();
            }
            paceOccupancyByDate.setBookingId(bookings.getId());
            paceOccupancyByDate.setOccupancyDate(occupancyDate);
            paceOccupancyByDate.setClientId(bookings.getClientId());
            paceOccupancyByDate.setOtaId(bookings.getOtaId());
            paceOccupancyByDate.setNoOfRooms(0);
            paceOccupancyByDate.setRpd(0d);
            paceOccupancyByDate.setPace(0l);
            paceOccupancyByDate.setArrival(0);
            paceOccupancyByDate.setDeparture(0);
            if ((compareTo(occupancyDate, bookings.getCheckinDate()) >= 0) && (compareTo(occupancyDate, bookings.getCheckoutDate()) < 0)) {
                paceOccupancyByDate.setNoOfRooms(bookings.getNoOfRooms());
                Parameter  parameter =  parameterRepository.findByClientIdAndParamName(bookings.getClientId(),ConstantUtil.ISTOTALVALUE).orElse(null);
                if(parameter.getParamValue().equalsIgnoreCase("YES")||parameter.getParamValue().equalsIgnoreCase("")||parameter.getParamValue()==null) {
                if (bookings.getTotalAmount() != null && bookings.getLos() != null) {
                    paceOccupancyByDate.setRpd(bookings.getTotalAmount() / bookings.getLos());
                }
                }else {
               	 if (bookings.getNetAmount() != null && bookings.getLos() != null) {
                        paceOccupancyByDate.setRpd(bookings.getNetAmount() / bookings.getLos());
                    }
               }
                if (bookings.getBookingDateTime() != null && bookings.getCheckinDate() != null) {
                    paceOccupancyByDate.setPace(DateUtil.daysBetween(bookings.getBookingDateTime(), bookings.getCheckinDate()));
                }
            }
            if (DateUtil.setTimeToZero(occupancyDate).compareTo(bookings.getCheckinDate()) == 0) {
                paceOccupancyByDate.setArrival(bookings.getNoOfRooms());
            }
            if (DateUtil.setTimeToZero(occupancyDate).compareTo(bookings.getCheckoutDate()) == 0) {
                paceOccupancyByDate.setDeparture(bookings.getNoOfRooms());
            }
            bookingPaceOccupancyByDateRepository.save(paceOccupancyByDate);
        }
    }

    private int compareTo(Date dateOne, Date dateTwo) {
        return DateUtil.setTimeToZero(dateOne).compareTo(dateTwo);
    }
}
