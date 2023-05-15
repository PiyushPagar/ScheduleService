package com.revnomix.revseed.integration.staah.transformer;

import java.time.LocalDateTime;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.transformer.GenericTransformer;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.revnomix.revseed.model.BookingPaceOccupancyByDate;
import com.revnomix.revseed.model.Bookings;
import com.revnomix.revseed.model.Clients;
import com.revnomix.revseed.model.OtaMappings;
import com.revnomix.revseed.model.Parameter;
import com.revnomix.revseed.model.RoomTypeMappings;
import com.revnomix.revseed.model.RoomTypes;
import com.revnomix.revseed.model.StaahRoomTypes;
import com.revnomix.revseed.repository.BookingPaceOccupancyByDateRepository;
import com.revnomix.revseed.repository.BookingsRepository;
import com.revnomix.revseed.repository.ClientsRepository;
import com.revnomix.revseed.repository.OtaMappingsRepository;
import com.revnomix.revseed.repository.ParameterRepository;
import com.revnomix.revseed.repository.RoomTypeMappingsRepository;
import com.revnomix.revseed.repository.RoomTypesRepository;
import com.revnomix.revseed.repository.StaahRoomTypesRepository;
import com.revnomix.revseed.schema.staah.Reservations;
import com.revnomix.revseed.schema.staah.Room;
import com.revnomix.revseed.Util.ConstantUtil;
import com.revnomix.revseed.Util.DateUtil;
import com.revnomix.revseed.integration.service.OccupancyByDatePopulationService;

@Component
public class ReservationTransformer implements GenericTransformer<StaahPopulationDto, List<Bookings>> {

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
    
    @Autowired
    private ParameterRepository parameterRepository;

	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	private static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");

    @Override
    public List<Bookings> transform(StaahPopulationDto staahPopulationDto) {
        List<Bookings> bookingsList = new ArrayList<>();
        Date today = new Date();
        List<Reservations.Reservation> reservations = staahPopulationDto.getReservations().getReservation();
        List<Reservations.Reservation> bookingsMergeDtoList = getMergedStaahBookingDtos(reservations);
        for (Reservations.Reservation staahBookingDto : bookingsMergeDtoList) {
            staahBookingDto.setBookingId(subString(staahBookingDto.getBookingId()));
            List<Bookings> bookings = bookingsRepository.findAllByBookingNoAndClientId(staahBookingDto.getBookingId(),staahPopulationDto.getClient().getId());
            if (bookings != null && !bookings.isEmpty()) {
                bookings.forEach(b->{
                    bookingPaceOccupancyByDateRepository.deleteByBookingIdAndClientId(b.getId(),b.getClientId());                    
                });
                bookings.forEach(b->{
  				  bookingsRepository.deleteById(b.getId());
  			  	});
            }


        }
        for (Reservations.Reservation reservation : bookingsMergeDtoList) {
            reservation.getRoom().stream().forEach(room -> {
                Bookings bookings = bookingsRepository.findByBookingNoAndClientId(reservation.getBookingId(),staahPopulationDto.getClient().getId());
                if (bookings == null) {
                    bookings = new Bookings();
                }
                bookings.setClientId(staahPopulationDto.getClient().getId());
                bookings.setHotelId(staahPopulationDto.getClient().getHotelId());
                bookings.setCmId(staahPopulationDto.getClient().getCmHotel());
                bookings.setChannelManager("staah");
                bookings.setBookingDateTime(reservation.getBookingDate().toGregorianCalendar().getTime());
                bookings.setModDateTime(today);
                bookings.setBookingNo(reservation.getBookingId());
                bookings.setChannelString(reservation.getCompany());
                bookings.setChannelRef(reservation.getChannelRef());
                if(reservation.getCompany()!=null && !reservation.getCompany().trim().equals("")) {
	                bookings.setChannelString(reservation.getCompany());
                }else {
                	 bookings.setChannelString(ConstantUtil.WEBDIRECT);
                }               
                if(reservation.getChannelRef()!=null && !reservation.getChannelRef().trim().equals("")) {
	                bookings.setChannelRef(reservation.getChannelRef());
                }else {
 	                bookings.setChannelRef(reservation.getBookingId());
                }
                bookings.setCmStatus(reservation.getBookingStatus());
                OtaMappings otas = getOtas(reservation, staahPopulationDto.getClient());
                bookings.setOtaId(otas.getId());
                bookings.setCheckinDate(reservation.getRoom().get(0).getArrivalDate().toGregorianCalendar().getTime());
                bookings.setCheckoutDate(reservation.getRoom().get(0).getDepartureDate().toGregorianCalendar().getTime());
                bookings.setLos(getLos(bookings.getCheckoutDate(),bookings.getCheckinDate()));
                bookings.setNoOfRooms((int)Double.parseDouble(room.getRemarks())); //by default 1 and sum of multiple booking
                bookings.setCmRoomId(reservation.getRoom().get(0).getRoomId().longValue()); //by default 1 and sum of multiple booking
                bookings.setCmRoomTypeString(reservation.getRoom().get(0).getRoomName()); //by default 1 and sum of multiple booking

                StaahRoomTypes staahRoomTypes = staahRoomTypesRepository.findByStaahIdAndClientId(room.getRoomId().longValue(),staahPopulationDto.getClient().getId());
                if (staahRoomTypes == null){
                    staahRoomTypes = new StaahRoomTypes();
                    staahRoomTypes.setRegDate(today);
                    staahRoomTypes.setName(room.getRoomName());
                    staahRoomTypes.setStaahId(room.getRoomId().longValue());
                    staahRoomTypes.setClientId(staahPopulationDto.getClient().getId());
                    staahRoomTypes=staahRoomTypesRepository.save(staahRoomTypes);
                }
                RoomTypeMappings roomTypemapping = roomTypeMappingsRepository.findOneByClientIdAndClientRoomType(staahPopulationDto.getClient().getId(), room.getRoomId().longValue());
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
                if(room.getPrice().getRateId()!=null) {
                	bookings.setRateId(room.getPrice().getRateId().longValue());
                }
                bookings.setRoomType(roomTypemapping.getRoomTypeId());
                bookings.setRatePlan(room.getPrice().getRateId().longValue());
                bookings.setCurrency(room.getCurrencycode());
                bookings.setRateValue(room.getPrice().getValue());
                bookings.setPriceDate(room.getPrice().getDate().toGregorianCalendar().getTime());
//                StaahRates staahRates = null;//staahRatesRepository.findByStaahRoomId(room.getPrice().getRateId(), room.getArrivalDate().toGregorianCalendar().getTime(), room.getRoomId());
//                if (staahRates!=null) {
//                    bookings.setPrice(staahRates.getRate());
//                }
                Double totalAmount = room.getTotalprice();
                Double netAmount = room.getTotalprice();
                if(staahPopulationDto.getClient().getCurrency()!=null && room.getCurrencycode()!=null && !staahPopulationDto.getClient().getCurrency().equals(room.getCurrencycode())) {
        	        Parameter isApplyCurrencyConv = parameterRepository.findByClientIdAndParamName(staahPopulationDto.getClient().getId(), ConstantUtil.APPLY_CURRENCY_CONVERSION).orElse(null);
        	        if(isApplyCurrencyConv!=null && isApplyCurrencyConv.getParamValue().equals(ConstantUtil.YES)) {
        	        	Parameter isApplyFixedConv = parameterRepository.findByClientIdAndParamName(staahPopulationDto.getClient().getId(), ConstantUtil.APPLY_FIXED_CONVERSION).orElse(null);
        	        	if(isApplyFixedConv!=null && isApplyFixedConv.getParamValue().equals(ConstantUtil.YES)) {
        	        		Parameter currencyConvFact = parameterRepository.findByClientIdAndParamName(staahPopulationDto.getClient().getId(), ConstantUtil.CURRENCY_CONVERSION_FACTOR).orElse(null);
        	            	if(currencyConvFact!=null) {
        	            		Integer convFact= Integer.parseInt(currencyConvFact.getParamValue());
        	            		totalAmount = totalAmount * convFact;
        	            		netAmount = netAmount * convFact;
        	            	}           		
        	        	}
        	        }
            	}
                bookings.setCommission(reservation.getCommissionamount());
                bookings.setTotalAmount(totalAmount);
                bookings.setNetAmount(netAmount);
                bookings.setTotalGuests(room.getNumberofguests());
                bookings.setTotalAdults(room.getNumberofadult());
                bookings.setTotalChildren(room.getNumberofchild());
                
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
    private OtaMappings getOtas(Reservations.Reservation reservation, Clients clients) {
        if (reservation.getCompany() == null || reservation.getCompany().equals("-1") || reservation.getCompany().trim().equals("")) {
            reservation.setCompany("WEBDIRECT");
        }
        OtaMappings otaMappings = otaMappingsRepository.findFirstByClientIdAndTypeAndClientOta(clients.getId(),ConstantUtil.CHANNELMAN, reservation.getCompany().toLowerCase());
        if (otaMappings == null) {
            otaMappings = new OtaMappings();
            otaMappings.setClientOta(reservation.getCompany());
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

    private List<Reservations.Reservation> getMergedStaahBookingDtos(List<Reservations.Reservation> staahBookingDtos) {
        List<Reservations.Reservation> bookingsMergeDtoList = new ArrayList<>();
        Map<String, List<Reservations.Reservation>> staahBookingGrouped = staahBookingDtos.stream().collect(Collectors.groupingBy(p -> p.getBookingId()));
        staahBookingGrouped.forEach((k, v) -> {
            if (v.size() > 1) {
                Reservations.Reservation mergedDto = new Reservations.Reservation();
                v.stream().forEach(staahBookingDto -> {
                    if (mergedDto.getBookingId() == null) {
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

    private void getRoom(Reservations.Reservation mergedDto, Reservations.Reservation staahBookingDto) {
        List<Room> roomList = new ArrayList<>();
        Room rooms = mergedDto.getRoom().get(0);
        rooms.setRemarks(String.valueOf(1));
        List<Room> roomDto = staahBookingDto.getRoom();
        if(mergedDto.getBookingId().equalsIgnoreCase(staahBookingDto.getBookingId())){
            if (roomDto.size() > 1){
                for(int i = 1 ; roomDto.size()-1 >= i; i++){
                    rooms.setTotalprice(rooms.getTotalprice() + roomDto.get(i).getTotalprice());
                    rooms.setRemarks(String.valueOf(convertToDouble(rooms.getRemarks()) + 1));
                    rooms.setDepartureDate(roomDto.get(i).getDepartureDate());
                }
            }
        }else{
            if (roomDto.size() > 1){
                roomDto.forEach(room->{
                    rooms.setTotalprice(rooms.getTotalprice() + room.getTotalprice());
                    rooms.setRemarks(String.valueOf(convertToDouble(rooms.getRemarks()) + 1));
                    rooms.setDepartureDate(room.getDepartureDate());
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

    private String subString(String bookingNo){
        bookingNo = bookingNo.replace("'", "");
        return bookingNo;
    }
    private int compareTo(Date dateOne, Date dateTwo) {
        return DateUtil.setTimeToZero(dateOne).compareTo(dateTwo);
    }
    
    @Scheduled(cron = "0 15 21 * * ?")
    public void scheduleDemoHotelData() {
    	Clients upperDeckClient = clientsRepository.findOneByPropertyName("Upper Deck Resort");
    	Clients demoHotelClient = clientsRepository.findOneByPropertyName("Revnomix Demo Hotel");
    	Date lastDate = bookingsRepository.findByClientIdInventory(demoHotelClient.getId());   
        logger.info("Add booking and booking pace date for demo hotel start :", dateTimeFormatter.format(LocalDateTime.now()));
        List<Bookings> upperdeckBookingList = null;
		List<Bookings> bookings = bookingsRepository.findAllByClientId(demoHotelClient.getId());
		List<BookingPaceOccupancyByDate> bookingsOccupancyByDate = bookingPaceOccupancyByDateRepository.findByClientId(demoHotelClient.getId());
	    try {
	    	bookingsOccupancyByDate.forEach(bobd->{
			      bookingPaceOccupancyByDateRepository.deleteById(bobd.getId());                    
			});
	   	 	bookings.forEach(b->{
				  bookingsRepository.deleteById(b.getId());
			});
	    }catch(Exception ce) {
	    	logger.error("delete booking and booking pace date for demo hotel start error :", dateTimeFormatter.format(LocalDateTime.now()));
	    }

        upperdeckBookingList = bookingsRepository.findAllByClientId(upperDeckClient.getId());

        for (Bookings bk : upperdeckBookingList) {
        	Bookings demoBookings = new Bookings();
        	demoBookings.setClientId(demoHotelClient.getId());
        	demoBookings.setHotelId(demoHotelClient.getHotelId());
        	demoBookings.setCmId(bk.getCmId());
        	demoBookings.setChannelManager(bk.getChannelManager());
        	demoBookings.setBookingDateTime(bk.getBookingDateTime());
        	demoBookings.setModDateTime(bk.getModDateTime());
        	demoBookings.setBookingNo(bk.getBookingNo());
        	demoBookings.setChannelString(bk.getChannelString());
        	demoBookings.setChannelRef(bk.getChannelRef());
            demoBookings.setCmStatus(bk.getCmStatus());
            demoBookings.setOtaId(bk.getOtaId());
            demoBookings.setCheckinDate(bk.getCheckinDate());
            demoBookings.setCheckoutDate(bk.getCheckoutDate());
            demoBookings.setLos(bk.getLos());
            demoBookings.setNoOfRooms(bk.getNoOfRooms()); //by default 1 and sum of multiple booking
            demoBookings.setCmRoomId(bk.getCmRoomId()); //by default 1 and sum of multiple booking
            demoBookings.setCmRoomTypeString(bk.getCmRoomTypeString()); //by default 1 and sum of multiple booking
            demoBookings.setRateId(bk.getRateId());
            demoBookings.setRoomType(bk.getRoomType());
            demoBookings.setRatePlan(bk.getRatePlan());
            demoBookings.setCurrency(bk.getCurrency());
            demoBookings.setRateValue(bk.getRateValue());
            demoBookings.setPriceDate(bk.getPriceDate());
            demoBookings.setPrice(bk.getPrice());            
            demoBookings.setCommission(bk.getCommission());
            demoBookings.setTotalAmount(bk.getTotalAmount());
            demoBookings.setTotalGuests(bk.getTotalGuests());
            demoBookings.setTotalAdults(bk.getTotalAdults());
            demoBookings.setTotalChildren(bk.getTotalChildren());
            demoBookings.setNetAmount(bk.getNetAmount());
            demoBookings.setRegdateRate(bk.getRegdateRate());
            demoBookings.setRegDate(bk.getRegDate());
            demoBookings.setStatus(bk.getStatus());
            Bookings save = bookingsRepository.save(demoBookings);
            bookingPaceOccupancyByDateRepository.findByBookingIdAndClientId(bk.getId(), bk.getClientId()).forEach(p -> {
            	BookingPaceOccupancyByDate bpobd = new BookingPaceOccupancyByDate();
            	bpobd.setArrival(p.getArrival());
            	bpobd.setBookingId(save.getId());
            	bpobd.setClientId(demoHotelClient.getId());
            	bpobd.setDeparture(p.getDeparture());
            	bpobd.setNoOfRooms(p.getNoOfRooms());
            	bpobd.setOccupancyDate(p.getOccupancyDate());
            	bpobd.setOtaId(p.getOtaId());
            	bpobd.setPace(p.getPace());
            	bpobd.setRpd(p.getRpd());
            	bpobd.setStatus(p.getStatus());
            	bookingPaceOccupancyByDateRepository.save(bpobd);
            });
            //saveBookingPaceRecord(save);
        }
    }
    
}
