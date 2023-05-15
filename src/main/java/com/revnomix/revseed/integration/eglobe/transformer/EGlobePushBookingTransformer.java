package com.revnomix.revseed.integration.eglobe.transformer;

import java.util.ArrayList;

import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.transformer.GenericTransformer;
import org.springframework.stereotype.Component;

import com.revnomix.revseed.model.BookingPaceOccupancyByDate;
import com.revnomix.revseed.model.Bookings;
import com.revnomix.revseed.model.Clients;
import com.revnomix.revseed.model.OtaMappings;
import com.revnomix.revseed.model.Parameter;
import com.revnomix.revseed.repository.BookingPaceOccupancyByDateRepository;
import com.revnomix.revseed.repository.BookingsRepository;
import com.revnomix.revseed.repository.ClientsRepository;
import com.revnomix.revseed.repository.OtaMappingsRepository;
import com.revnomix.revseed.repository.ParameterRepository;
import com.revnomix.revseed.repository.RoomTypeMappingsRepository;
import com.revnomix.revseed.repository.RoomTypesRepository;
import com.revnomix.revseed.repository.StaahRoomTypesRepository;
import com.revnomix.revseed.Util.ConstantUtil;
import com.revnomix.revseed.Util.DateUtil;
import com.revnomix.revseed.integration.service.OccupancyByDatePopulationService;
import com.revnomix.revseed.integration.staahMax.dto.DailyBookingResponse;
import com.revnomix.revseed.integration.staahMax.dto.StaahMaxPopulationDto;


@Component
public class EGlobePushBookingTransformer implements GenericTransformer<StaahMaxPopulationDto, List<Bookings>> {
	
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
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
    private StaahRoomTypesRepository staahRoomTypesRepository;

    @Autowired
    private OccupancyByDatePopulationService occupancyByDatePopulationService;

    @Autowired
    private BookingPaceOccupancyByDateRepository bookingPaceOccupancyByDateRepository;

    @Override
    public List<Bookings> transform(StaahMaxPopulationDto staahPopulationDto) {
        List<Bookings> bookingsList = new ArrayList<>();
        Date today = new Date();
        //logger.error("Staah All push booking system StaahMax Push Booking transformer");
        List<DailyBookingResponse.Reservations> reservations = staahPopulationDto.getDailyBookingResponse().getReservations();
        //List<DailyBookingResponse.Reservations> bookingsMergeDtoList = getMergedStaahBookingDtos(reservations);
        for (DailyBookingResponse.Reservations staahBookingDto : reservations) {
            staahBookingDto.setReservation_code(subString(staahBookingDto.getReservation_code()));
            
            List<Bookings> bookings = bookingsRepository.findAllByBookingNoAndClientId(staahBookingDto.getReservation_code(),staahPopulationDto.getClient().getId());
            if (bookings != null && !bookings.isEmpty()) {
                bookings.forEach(b->{
                	//logger.error("Staah All push booking system deleted bookings"+ b.getId() );
                    bookingPaceOccupancyByDateRepository.deleteByBookingIdAndClientId(b.getId(),b.getClientId());
                });

                bookingsRepository.deleteByBookingNo(staahBookingDto.getReservation_code());
            }

        }
        for (DailyBookingResponse.Reservations reservation : reservations) {
            reservation.getRoom_info().forEach(room -> {
                Bookings bookings = bookingsRepository.findByBookingNoAndClientId(reservation.getReservation_code(),staahPopulationDto.getClient().getId());
                if (bookings == null) {
                    bookings = new Bookings();
                }
                bookings.setClientId(staahPopulationDto.getClient().getId());
                bookings.setHotelId(staahPopulationDto.getClient().getHotelId());
                bookings.setCmId(staahPopulationDto.getClient().getCmHotel());
                bookings.setChannelManager("staah All");
                bookings.setBookingDateTime(DateUtil.toDateByAllFormat(reservation.getDate(), "yyyy-MM-dd"));
                bookings.setModDateTime(today);
                bookings.setBookingNo(reservation.getReservation_code());
                if(reservation.getTravel_agent()!=null && !reservation.getReservation_code().trim().equals("")) {
	                bookings.setChannelString(reservation.getTravel_agent());
	                bookings.setChannelRef(reservation.getReservation_code());
                }else {
                	 bookings.setChannelString(ConstantUtil.WEBDIRECT);
 	                bookings.setChannelRef(reservation.getReservation_code());
                }
                bookings.setCmStatus(reservation.getStatus());
                OtaMappings otas = getOtas(reservation, staahPopulationDto.getClient());
                bookings.setOtaId(otas.getId());
                bookings.setCheckinDate(DateUtil.toDateByAllFormat(reservation.getCheckin_at(), "yyyy-MM-dd"));
                bookings.setCheckoutDate(DateUtil.toDateByAllFormat(reservation.getCheckout_at(), "yyyy-MM-dd"));
                bookings.setLos(getLos(bookings.getCheckoutDate(),bookings.getCheckinDate()));
                bookings.setNoOfRooms(reservation.getRoom_count()); //by default 1 and sum of multiple booking
                bookings.setCmRoomId(Long.valueOf(room.getRoom_number())); //by default 1 and sum of multiple booking
                bookings.setCmRoomTypeString(room.getRoom_type()); //by default 1 and sum of multiple booking
                if(room.getPrice().get(0).getRate_code_group()!=null) {
                	bookings.setRateId(room.getPrice().get(0).getRate_code_group().longValue());
                }
                //bookings.setRoomType(roomTypemapping.getRoomTypeId());
                bookings.setRatePlan(room.getPrice().get(0).getRate_code_group().longValue());
                bookings.setCurrency(reservation.getNationality());
                bookings.setRateValue(room.getPrice().get(0).getAmount());
                bookings.setPriceDate(DateUtil.toDateByAllFormat(reservation.getDate(), "yyyy-MM-dd"));
                bookings.setTotalAmount(reservation.getTotalprice());
                bookings.setNetAmount(room.getTotalprice());

                for (DailyBookingResponse.Reservations.RoomInfo room1 : reservation.getRoom_info()){
                    //bookings.setTotalGuests(checkInteger(bookings.getTotalGuests()) + checkInteger(room1.getCount()));
                    bookings.setTotalAdults(checkInteger(bookings.getTotalAdults()) + checkInteger(room1.getNumber_adults()));
                    bookings.setTotalChildren(checkInteger(bookings.getTotalChildren()) + checkInteger(room1.getNumber_children()));
                }
                
                bookings.setRegdateRate(today);
                bookings.setRegDate(today);

                bookings.setStatus(ConstantUtil.ACTIVE);
                occupancyByDatePopulationService.updateOrInsertOccupancyByDate(bookings, bookings.getNetAmount(), bookings.getNoOfRooms());
                Bookings save = bookingsRepository.save(bookings);
                saveBookingPaceRecord(save);
                bookingsList.add(bookings);
            });
        }
        Clients client = staahPopulationDto.getClient();
        client.setSystemToday(DateUtil.setTimeToZero(new Date()));
        clientsRepository.save(client);
        return bookingsList;
    }

    private Integer getLos(Date checkoutDate, Date checkInDate) {
        long difference = checkoutDate.getTime() - checkInDate.getTime();
        return Math.toIntExact((difference / (1000 * 60 * 60 * 24)));
    }
    private OtaMappings getOtas(DailyBookingResponse.Reservations reservation, Clients clients) {
        if (reservation.getTravel_agent() == null || reservation.getTravel_agent().equals("-1")) {
            reservation.setTravel_agent("WEBDIRECT");
        }
        OtaMappings otaMappings = otaMappingsRepository.findFirstByClientIdAndTypeAndClientOta(clients.getId(), ConstantUtil.CHANNELMAN, reservation.getTravel_agent().toLowerCase());
        if (otaMappings == null) {
            otaMappings = new OtaMappings();
            otaMappings.setClientOta(reservation.getTravel_agent());
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


    private Double convertToDouble(String value) {
        return value == null ? 0d : Double.valueOf(value);
    }

    private Integer checkInteger(Integer value) {
        return value == null ? 0 : value;
    }

    private String subString(String bookingNo){
        bookingNo = bookingNo.replace("'", "");
        return bookingNo;
    }
    private int compareTo(Date dateOne, Date dateTwo) {
        return DateUtil.setTimeToZero(dateOne).compareTo(dateTwo);
    }
}
