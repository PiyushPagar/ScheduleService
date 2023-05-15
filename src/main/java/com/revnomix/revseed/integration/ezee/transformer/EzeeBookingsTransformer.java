package com.revnomix.revseed.integration.ezee.transformer;

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
import com.revnomix.revseed.model.Otas;
import com.revnomix.revseed.model.Parameter;
import com.revnomix.revseed.model.RoomTypeMappings;
import com.revnomix.revseed.model.RoomTypes;
import com.revnomix.revseed.model.StaahRoomTypes;
import com.revnomix.revseed.repository.BookingPaceOccupancyByDateRepository;
import com.revnomix.revseed.repository.BookingStatusMappingRepository;
import com.revnomix.revseed.repository.BookingsRepository;
import com.revnomix.revseed.repository.ClientsRepository;
import com.revnomix.revseed.repository.OtaMappingsRepository;
import com.revnomix.revseed.repository.OtasRepository;
import com.revnomix.revseed.repository.ParameterRepository;
import com.revnomix.revseed.repository.RoomTypeMappingsRepository;
import com.revnomix.revseed.repository.RoomTypesRepository;
import com.revnomix.revseed.repository.StaahRoomTypesRepository;
import com.revnomix.revseed.schema.ezee.RESResponse;
import com.revnomix.revseed.schema.ezee.RESResponse.Reservations.CancelReservation;
import com.revnomix.revseed.Util.ConstantUtil;
import com.revnomix.revseed.Util.DateUtil;
import com.revnomix.revseed.integration.ezee.EzeePopulationDto;
import com.revnomix.revseed.integration.integration.IntegrationType;
import com.revnomix.revseed.integration.service.OccupancyByDatePopulationService;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class EzeeBookingsTransformer implements GenericTransformer<EzeePopulationDto, List<Bookings>> {
    @Autowired
	ParameterRepository parameterRepository;
	
    @Autowired
    private OtaMappingsRepository otaMappingsRepository;

    @Autowired
    private RoomTypeMappingsRepository roomTypeMappingsRepository;

    @Autowired
    private RoomTypesRepository roomTypesRepository;

    @Autowired
    private BookingsRepository bookingsRepository;

    @Autowired
    private ClientsRepository clientsRepository;

    @Autowired
    private OtasRepository otasRepository;

    @Autowired
    private StaahRoomTypesRepository staahRoomTypesRepository;

    @Autowired
    private OccupancyByDatePopulationService occupancyByDatePopulationService;

    @Autowired
    private BookingPaceOccupancyByDateRepository bookingPaceOccupancyByDateRepository;
    
    @Autowired
    private BookingStatusMappingRepository bookingStatusMappingRepository;

    @Override
    public List<Bookings> transform(EzeePopulationDto ezeePopulationDto) {

        List<Bookings> bookingsList = new ArrayList<>();
        Date today = new Date();
        if (ezeePopulationDto.getResResponse().getReservations().getReservation() == null) {
            return bookingsList;
        }
        for(CancelReservation cr :ezeePopulationDto.getResResponse().getReservations().getCancelReservation()) {
        	List<Bookings> cancelBookingList = bookingsRepository.findAllByChannelRefAndChannelManager(cr.getVoucherNo(),"ezee");
        	for(Bookings bookings :cancelBookingList) {
        		bookings.setCmStatus("Cancel");
        		bookings.setRegdateRate(today);
                bookings.setRegDate(today);
        		bookingsRepository.save(bookings);
        	}
        }
        for( RESResponse.Reservations.Reservation reserve : ezeePopulationDto.getResResponse().getReservations().getReservation()) {
            RESResponse.Reservations.Reservation.BookByInfo reservation = reserve.getBookByInfo();
            List<RESResponse.Reservations.Reservation.BookByInfo.BookingTran> reservations = reserve.getBookByInfo().getBookingTran();
            for (RESResponse.Reservations.Reservation.BookByInfo.BookingTran rev : reservations){
            	List<Bookings> bookingList = bookingsRepository.findAllByBookingNoAndClientId(String.valueOf(rev.getTransactionId()), ezeePopulationDto.getClients().getId());
                if(bookingList.size()>1) {
                	List<BookingPaceOccupancyByDate> duplicateList = bookingPaceOccupancyByDateRepository.findByBookingIdAndClientId(bookingList.get(1).getId(),ezeePopulationDto.getClients().getId());
                	if(duplicateList!=null && duplicateList.size()>0) {
                		bookingPaceOccupancyByDateRepository.deleteAll(duplicateList);
                	}
                	bookingsRepository.delete(bookingList.get(1));
                }
            	Bookings bookings = null;
            	if(bookingList!=null && bookingList.size()>0) {
            		bookings = bookingList.get(0);
            	}
                if (bookings == null) {
                    bookings = new Bookings();	
                }
                bookings.setClientId(ezeePopulationDto.getClients().getId());
                bookings.setHotelId(ezeePopulationDto.getClients().getHotelId());
                bookings.setCmId(ezeePopulationDto.getClients().getCmHotel());
                bookings.setChannelManager("ezee");
                if (rev.getCreatedatetime() != null) {
                    bookings.setBookingDateTime(DateUtil.toDate(rev.getCreatedatetime(), "yyyy-MM-dd"));
                    if (rev.getModifydatetime() != null) {
                        bookings.setModDateTime(DateUtil.toDate(rev.getModifydatetime(), "yyyy-MM-dd"));
                    } else {
                        bookings.setModDateTime(DateUtil.toDate(rev.getCreatedatetime(), "yyyy-MM-dd"));
                    }
                } else if (bookings.getBookingDateTime() != null) {
                    bookings.setModDateTime(today);
                } else {
                    bookings.setBookingDateTime(today);
                    bookings.setModDateTime(today);
                }
                bookings.setBookingNo(String.valueOf(rev.getTransactionId()));
                bookings.setChannelString(reservation.getBookedBy());
                bookings.setChannelRef(rev.getVoucherNo());
                
                if(reservation.getBookedBy()!=null && !reservation.getBookedBy().trim().equals("")) {
	                bookings.setChannelString(reservation.getBookedBy());
                }else {
                	 bookings.setChannelString(ConstantUtil.WEBDIRECT);
                }               
                if(rev.getVoucherNo()!=null && !rev.getVoucherNo().trim().equals("")) {
	                bookings.setChannelRef(rev.getVoucherNo());
                }else {
 	                bookings.setChannelRef(String.valueOf(rev.getTransactionId()));
                }
                
                bookings.setCmStatus(rev.getStatus());
                List<BookingStatusMapping> bookingStatusMappingList = bookingStatusMappingRepository.findByBookingStatusAndChannelManager(rev.getStatus(),IntegrationType.EZEE.toString());
                if(bookingStatusMappingList!=null && bookingStatusMappingList.size()<1) {
                	BookingStatusMapping bookingStatusMapping = new BookingStatusMapping();
                	bookingStatusMapping.setBookingStatus(rev.getStatus());
                	bookingStatusMapping.setChannelManager(IntegrationType.EZEE.toString());
                	bookingStatusMapping.setStatus(ConstantUtil.ACTIVE);
                	bookingStatusMappingRepository.save(bookingStatusMapping);
                }
                OtaMappings otas = getOtas(reservation, ezeePopulationDto.getClients());
                bookings.setOtaId(otas.getId());
                bookings.setCheckinDate(rev.getStart().toGregorianCalendar().getTime());
                bookings.setCheckoutDate(rev.getEnd().toGregorianCalendar().getTime());
                bookings.setLos(getLos(bookings.getCheckoutDate(), bookings.getCheckinDate()));
                bookings.setNoOfRooms(1); //by default 1 and sum of multiple booking
                Long roomId = rev.getRoomTypeCode().longValue();
                bookings.setCmRoomId(roomId); //by default 1 and sum of multiple booking
                bookings.setCmRoomTypeString(rev.getRoomTypeName()); //by default 1 and sum of multiple booking

                StaahRoomTypes staahRoomTypes = staahRoomTypesRepository.findByStaahIdAndClientId(roomId, ezeePopulationDto.getClients().getId());
                if (staahRoomTypes == null) {
                    staahRoomTypes = new StaahRoomTypes();
                    staahRoomTypes.setRegDate(today);
                    staahRoomTypes.setName(rev.getRoomTypeName());
                    staahRoomTypes.setStaahId(roomId);
                    staahRoomTypes.setClientId(ezeePopulationDto.getClients().getId());
                    staahRoomTypes = staahRoomTypesRepository.save(staahRoomTypes);
                }
                RoomTypeMappings roomTypemapping = roomTypeMappingsRepository.findByClientRoomTypeAndClientId(roomId, ezeePopulationDto.getClients().getId());
                if (roomTypemapping == null) {
                    roomTypemapping = new RoomTypeMappings();
                    roomTypemapping.setClientId(ezeePopulationDto.getClients().getId());
                    roomTypemapping.setStatus("active");
                    roomTypemapping.setType("ezee");
                    roomTypemapping.setClientRoomType((long) staahRoomTypes.getId());
                    RoomTypes roomTypes = roomTypesRepository.findByName("Uncategorized");
                    roomTypemapping.setRoomTypeId(roomTypes.getId());
                    roomTypemapping.setCapacity(1);

                    roomTypeMappingsRepository.save(roomTypemapping);
                }

                //bookings.setRateId((int)rev.getRateplanCode());
                bookings.setRoomType(roomTypemapping.getRoomTypeId());
                if(rev.getRateplanCode()!=null) {
                	bookings.setRatePlan(rev.getRateplanCode().longValue());
                }
                bookings.setCurrency(rev.getCurrencyCode());
                bookings.setRateValue(rev.getTotalRate().floatValue());
                //bookings.setPriceDate(DateUtil.toDate(rev.getCreatedatetime(), "yyyy-MM-dd"));

                bookings.setCommission(rev.getTACommision().doubleValue());
                bookings.setTotalAmount(rev.getTotalRate().doubleValue());
                //bookings.setTotalGuests(room.getNumberofguests());
                bookings.setTotalAdults(0);
                bookings.setTotalChildren(0);
                bookings.setNetAmount(0.0);
                for (RESResponse.Reservations.Reservation.BookByInfo.BookingTran.RentalInfo bookByInfo : rev.getRentalInfo()) {
                    bookings.setTotalAdults(bookByInfo.getAdult() + bookings.getTotalAdults());
                    bookings.setTotalChildren(bookByInfo.getChild() + bookings.getTotalChildren());
                    bookings.setNetAmount(bookByInfo.getRentBeforeTax().doubleValue() + bookings.getNetAmount());
                }
                bookings.setRegdateRate(today);
                bookings.setRegDate(today);

                bookings.setStatus("active");
                occupancyByDatePopulationService.updateOrInsertOccupancyByDate(bookings, bookings.getNetAmount(), bookings.getNoOfRooms());
                bookings = bookingsRepository.save(bookings);
                saveBookingPaceRecord(bookings);
                bookingsList.add(bookings);
            }

        }
        Clients client = ezeePopulationDto.getClients();
        client.setSystemToday(new Date());
        clientsRepository.save(client);
        return bookingsList;
    }

    private Integer getLos(Date checkoutDate, Date checkInDate) {
        long difference = checkoutDate.getTime() - checkInDate.getTime();
        return Math.toIntExact((difference / (1000 * 60 * 60 * 24)));
    }

    private OtaMappings getOtas(RESResponse.Reservations.Reservation.BookByInfo reservation, Clients clients) {
        if (reservation.getBookedBy() == null || reservation.getBookedBy().equals("-1") || reservation.getBookedBy().trim().equals("")) {
            reservation.setBookedBy("WEBDIRECT");
        }
        Otas ota;
        if (reservation.getBookedBy().toLowerCase().equalsIgnoreCase("Internet Booking Engine".toLowerCase())){
            ota = otasRepository.findByNameOrDomainName("YOURWEB", "YOURWEB").orElse(otasRepository.findById(1).get());
        }else{
            ota = otasRepository.findByNameOrDomainName(reservation.getBookedBy().toUpperCase(), reservation.getBookedBy().toUpperCase()).orElse(otasRepository.findById(1).get());
        }
        OtaMappings otaMappings = otaMappingsRepository.findFirstByClientIdAndTypeAndClientOta(clients.getId(), ConstantUtil.CHANNELMAN, reservation.getBookedBy());
        if (otaMappings == null) {
            otaMappings = new OtaMappings();
            otaMappings.setClientOta(reservation.getBookedBy());
            otaMappings.setClientId(clients.getId());
            otaMappings.setOtaId(ota.getId());
            otaMappings.setRegdate(new Date());
            otaMappings.setType(ConstantUtil.CHANNELMAN);
            otaMappings = otaMappingsRepository.save(otaMappings);
        }
        return otaMappings;
    }

    private void saveBookingPaceRecord(Bookings bookings) {
    	List<BookingPaceOccupancyByDate> occupancyDateList = bookingPaceOccupancyByDateRepository.findByBookingIdAndClientId(bookings.getId(), bookings.getClientId());
        if(occupancyDateList!=null) {
	    	for(BookingPaceOccupancyByDate occupancyDt : occupancyDateList) {
	        	occupancyDt.setBookingId(occupancyDt.getBookingId());
	        	occupancyDt.setOccupancyDate(occupancyDt.getOccupancyDate());
	        	occupancyDt.setClientId(occupancyDt.getClientId());
	        	occupancyDt.setOtaId(occupancyDt.getOtaId());
	        	occupancyDt.setNoOfRooms(0);
	        	occupancyDt.setRpd(0d);
	        	occupancyDt.setPace(0l);
	        	occupancyDt.setArrival(0);
	            occupancyDt.setDeparture(0);
	            bookingPaceOccupancyByDateRepository.save(occupancyDt);
	        }
        }
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
            if ((compareTo(occupancyDate, bookings.getCheckinDate()) >= 0) && 
            		(compareTo(occupancyDate, bookings.getCheckoutDate()) < 0)) {
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
