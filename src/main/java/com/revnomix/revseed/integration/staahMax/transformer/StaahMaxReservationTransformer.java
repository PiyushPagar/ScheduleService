package com.revnomix.revseed.integration.staahMax.transformer;

import java.util.ArrayList;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.transformer.GenericTransformer;
import org.springframework.stereotype.Component;

import com.revnomix.revseed.model.BookingPaceOccupancyByDate;
import com.revnomix.revseed.model.Bookings;
import com.revnomix.revseed.model.Clients;
import com.revnomix.revseed.model.OtaMappings;
import com.revnomix.revseed.model.Parameter;
import com.revnomix.revseed.model.RoomTypeMappings;
import com.revnomix.revseed.model.RoomTypes;
import com.revnomix.revseed.model.StaahRates;
import com.revnomix.revseed.model.StaahRoomTypes;
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
import com.revnomix.revseed.integration.staahMax.dto.BookingRequest;
import com.revnomix.revseed.integration.staahMax.dto.StaahMaxPopulationDto;

@Component
public class StaahMaxReservationTransformer implements GenericTransformer<StaahMaxPopulationDto, List<Bookings>> {

    @Autowired
    private OtaMappingsRepository otaMappingsRepository;

    @Autowired
    private RoomTypeMappingsRepository roomTypeMappingsRepository;

    @Autowired
    ParameterRepository parameterRepository;
    
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
        List<BookingRequest.Reservations.Reservation> reservations = staahPopulationDto.getBookingRequest().getReservations().getReservation();
        List<BookingRequest.Reservations.Reservation> bookingsMergeDtoList = getMergedStaahBookingDtos(reservations);
        for (BookingRequest.Reservations.Reservation staahBookingDto : bookingsMergeDtoList) {
            staahBookingDto.setReservation_id(subString(staahBookingDto.getReservation_id()));
            List<Bookings> bookings = bookingsRepository.findAllByBookingNoAndClientId(staahBookingDto.getReservation_id(),staahPopulationDto.getClient().getId());
            if (bookings != null && !bookings.isEmpty()) {
                bookings.forEach(b->{
                    bookingPaceOccupancyByDateRepository.deleteByBookingIdAndClientId(b.getId(),b.getClientId());
                });
                bookingsRepository.deleteByBookingNo(staahBookingDto.getReservation_id());
            }
        }
        for (BookingRequest.Reservations.Reservation reservation : bookingsMergeDtoList) {
            reservation.getRoom().stream().forEach(room -> {
                Bookings bookings = bookingsRepository.findByBookingNoAndClientId(reservation.getReservation_id(),staahPopulationDto.getClient().getId());
                if (bookings == null) {
                    bookings = new Bookings();
                }
                bookings.setClientId(staahPopulationDto.getClient().getId());
                bookings.setHotelId(staahPopulationDto.getClient().getHotelId());
                bookings.setCmId(staahPopulationDto.getClient().getCmHotel());
                bookings.setChannelManager("staah All");
                bookings.setBookingDateTime(DateUtil.toDateByAllFormat(reservation.getReservation_datetime(), "yyyy-MM-dd"));
                bookings.setModDateTime(today);
                bookings.setBookingNo(reservation.getReservation_id());
                if(reservation.getPOS()!=null && !reservation.getPOS().trim().equals("")) {
	                bookings.setChannelString(reservation.getPOS());
	                bookings.setChannelRef(reservation.getPOS());
                }else {
                	bookings.setChannelString(ConstantUtil.WEBDIRECT);
 	                bookings.setChannelRef(reservation.getReservation_id());
                }
                bookings.setCmStatus(reservation.getStatus());
                OtaMappings otas = getOtas(reservation, staahPopulationDto.getClient());
                bookings.setOtaId(otas.getId());
                bookings.setCheckinDate(DateUtil.toDateByAllFormat(reservation.getRoom().get(0).getArrival_date(), "yyyy-MM-dd"));
                bookings.setCheckoutDate(DateUtil.toDateByAllFormat(reservation.getRoom().get(0).getFirst_name(), "yyyy-MM-dd"));
                bookings.setLos(getLos(bookings.getCheckoutDate(),bookings.getCheckinDate()));
                bookings.setNoOfRooms((int)Double.parseDouble(room.getRemarks())); //by default 1 and sum of multiple booking
                bookings.setCmRoomId(Long.valueOf(room.getRoom_id())); //by default 1 and sum of multiple booking
                bookings.setCmRoomTypeString(room.getRoom_name()); //by default 1 and sum of multiple booking

                StaahRoomTypes staahRoomTypes = staahRoomTypesRepository.findByStaahIdAndClientId(Long.valueOf(room.getRoom_id()),staahPopulationDto.getClient().getId());
                if (staahRoomTypes == null){
                    staahRoomTypes = new StaahRoomTypes();
                    staahRoomTypes.setRegDate(today);
                    staahRoomTypes.setName(room.getRoom_name());
                    staahRoomTypes.setStaahId(Long.valueOf(room.getRoom_id()));
                    staahRoomTypes.setClientId(staahPopulationDto.getClient().getId());
                    staahRoomTypes=staahRoomTypesRepository.save(staahRoomTypes);
                }
                RoomTypeMappings roomTypemapping = roomTypeMappingsRepository.findOneByClientIdAndClientRoomType(staahPopulationDto.getClient().getId(), Long.valueOf(room.getRoom_id()));
                if (roomTypemapping ==null){
                    roomTypemapping= new RoomTypeMappings();
                    roomTypemapping.setClientId(staahPopulationDto.getClient().getId());
                    roomTypemapping.setStatus(ConstantUtil.ACTIVE);
                    roomTypemapping.setType("staah");
                    roomTypemapping.setClientRoomType(staahRoomTypes.getId().longValue());
                    RoomTypes roomTypes = roomTypesRepository.findByName("Uncategorized");
                    roomTypemapping.setRoomTypeId(roomTypes.getId());
                    roomTypemapping.setCapacity(1);

                    roomTypeMappingsRepository.save(roomTypemapping);
                }

                bookings.setRateId(Long.valueOf(room.getPrice().get(0).getRate_id()));
                bookings.setRoomType(roomTypemapping.getRoomTypeId());
                bookings.setRatePlan(Long.valueOf(room.getPrice().get(0).getRate_id()));
                bookings.setCurrency(reservation.getCurrencycode());
//                bookings.setRateValue(room.getPrice().getValue());
//                bookings.setPriceDate(room.getPrice().getDate().toGregorianCalendar().getTime());
                StaahRates staahRates = null;
                if (staahRates!=null) {
                    bookings.setPrice(staahRates.getRate());
                }
                bookings.setCommission(Double.valueOf(reservation.getCommissionamount()));
            	bookings.setTotalAmount(Double.valueOf(room.getAmountaftertax()));
            	bookings.setNetAmount(Double.valueOf(room.getAmountaftertax()));
                for (BookingRequest.Reservations.Reservation.Room.GuestCount room1 : room.getGuestCount()){
                    bookings.setTotalGuests(checkInteger(bookings.getTotalGuests()) + checkInteger(room1.getCount()));
                }
//                bookings.setTotalAdults(room.getNumberofadult());
//                bookings.setTotalChildren(room.getNumberofchild());
                
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
    private OtaMappings getOtas(BookingRequest.Reservations.Reservation reservation, Clients clients) {
        if (reservation.getPayment_type() == null || reservation.getPayment_type().equals("-1") || reservation.getPayment_type().trim().equals("")) {
            reservation.setPayment_type("WEBDIRECT");
        }
        OtaMappings otaMappings = otaMappingsRepository.findFirstByClientIdAndTypeAndClientOta(clients.getId(),ConstantUtil.CHANNELMAN, reservation.getPayment_type().toLowerCase());
        if (otaMappings == null) {
            otaMappings = new OtaMappings();
            otaMappings.setClientOta(reservation.getPayment_type());
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
                Parameter parameter = parameterRepository.findByClientIdAndParamName(bookings.getClientId(), ConstantUtil.ISTOTALVALUE).orElse(null);
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

    private List<BookingRequest.Reservations.Reservation> getMergedStaahBookingDtos(List<BookingRequest.Reservations.Reservation> staahBookingDtos) {
        List<BookingRequest.Reservations.Reservation> bookingsMergeDtoList = new ArrayList<>();
        Map<String, List<BookingRequest.Reservations.Reservation>> staahBookingGrouped = staahBookingDtos.stream().collect(Collectors.groupingBy(p -> p.getReservation_id()));
        staahBookingGrouped.forEach((k, v) -> {
            if (v.size() > 1) {
                BookingRequest.Reservations.Reservation mergedDto = new BookingRequest.Reservations.Reservation();
                v.stream().forEach(staahBookingDto -> {
                    if (mergedDto.getReservation_id() == null) {
                        BeanUtils.copyProperties(staahBookingDto, mergedDto);
                    } else {
                        getRoom(mergedDto, staahBookingDto);
                    }
                });
                bookingsMergeDtoList.add(mergedDto);
            } else {
                if (v.get(0) != null) {
                    getRoom(v.get(0), v.get(0));
                    bookingsMergeDtoList.add(v.get(0));
                }
            }
        });
        return bookingsMergeDtoList;
    }

    private void getRoom(BookingRequest.Reservations.Reservation mergedDto, BookingRequest.Reservations.Reservation staahBookingDto) {
        BookingRequest.Reservations.Reservation.Room rooms = mergedDto.getRoom().get(0);
        rooms.setRemarks(String.valueOf(1));
        List<BookingRequest.Reservations.Reservation.Room> roomDto = staahBookingDto.getRoom();
        if(mergedDto.getReservation_id().equalsIgnoreCase(staahBookingDto.getReservation_id())){
            if (roomDto.size() > 1){
                for(int i = 1 ; roomDto.size()-1 >= i; i++){
                    rooms.setAmountaftertax(rooms.getAmountaftertax() + roomDto.get(i).getAmountaftertax());
                    rooms.setRemarks(String.valueOf(convertToDouble(rooms.getRemarks()) + 1));
                    rooms.setDeparture_date(roomDto.get(i).getDeparture_date());
                }
            }
        }else{
            if (roomDto.size() > 1){
                roomDto.forEach(room->{
                    rooms.setAmountaftertax(rooms.getAmountaftertax() + room.getAmountaftertax());
                    rooms.setRemarks(String.valueOf(convertToDouble(rooms.getRemarks()) + 1));
                    rooms.setDeparture_date(room.getDeparture_date());
                });
            }

        }
        //roomList.add(rooms);
        mergedDto.getRoom().clear();
        mergedDto.getRoom().add(rooms);
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
