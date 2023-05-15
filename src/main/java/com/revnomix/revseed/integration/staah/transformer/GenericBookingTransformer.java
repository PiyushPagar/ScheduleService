package com.revnomix.revseed.integration.staah.transformer;

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
import com.revnomix.revseed.schema.ezee.RESResponse;
import com.revnomix.revseed.schema.ezee.RESResponse.Reservations.CancelReservation;
import com.revnomix.revseed.Util.ConstantUtil;
import com.revnomix.revseed.Util.DateUtil;
import com.revnomix.revseed.integration.eglobe.dto.EGlobeBookingResponse;
import com.revnomix.revseed.integration.eglobe.dto.EGlobeRoomDto;
import com.revnomix.revseed.integration.integration.IntegrationType;
import com.revnomix.revseed.integration.service.OccupancyByDatePopulationService;
import com.revnomix.revseed.integration.staah.StaahBookingDto;
import com.revnomix.revseed.integration.staahMax.dto.DailyBookingResponse;
import com.revnomix.revseed.integration.staahMax.dto.GenericIntegrationDto;

@Component
public class GenericBookingTransformer implements GenericTransformer<GenericIntegrationDto, List<Bookings>> {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
	
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

	@Override
	public List<Bookings> transform(GenericIntegrationDto source) {
		List<Bookings> bookingsList = new ArrayList<>();
        Date today = new Date();
        try {
        if(source.getIntegerationType().equals(IntegrationType.EZEE.toString())) {
	        for(CancelReservation cr :source.getResResponse().getReservations().getCancelReservation()) {
	        	List<Bookings> cancelBookingList = bookingsRepository.findAllByChannelRefAndChannelManager(cr.getVoucherNo(),"ezee");
	        	for(Bookings bookings :cancelBookingList) {
	        		bookings.setCmStatus("Cancel");
	        		bookings.setRegdateRate(today);
	                bookings.setRegDate(today);
	        		bookingsRepository.save(bookings);
	        	}
	        }
	        
	        for( RESResponse.Reservations.Reservation reserve : source.getResResponse().getReservations().getReservation()) {
	            RESResponse.Reservations.Reservation.BookByInfo reservation = reserve.getBookByInfo();
	            List<RESResponse.Reservations.Reservation.BookByInfo.BookingTran> reservationsezee = reserve.getBookByInfo().getBookingTran();
	            for (RESResponse.Reservations.Reservation.BookByInfo.BookingTran rev : reservationsezee){	            	
	                List<Bookings> bookings = bookingsRepository.findAllByBookingNoAndClientId(String.valueOf(rev.getTransactionId()),source.getClient().getId());
	                if (bookings != null && !bookings.isEmpty()) {
	                    bookings.forEach(b->{
	                        bookingPaceOccupancyByDateRepository.deleteByBookingIdAndClientId(b.getId(),b.getClientId());
	                    });

	                    bookingsRepository.deleteByBookingNo(String.valueOf(rev.getTransactionId()));
	                }
	            	Date bookingDate =null;	            	
	            	Integer noOfRooms = 1;
	            	Integer totalChildren = 0;
	            	Integer totalAdults = 0;
	            	Double netAmount = 0d; 
	                Date checkinDate = rev.getStart().toGregorianCalendar().getTime();
	                Date checkOutDate = rev.getEnd().toGregorianCalendar().getTime();
	                Date priceDate = null;
	                Date modDateTime = null;
	                Long roomId = rev.getRoomTypeCode().longValue();
	                if (rev.getCreatedatetime() != null) {
	            		bookingDate = DateUtil.toDate(rev.getCreatedatetime(), "yyyy-MM-dd");	            	
		                if (rev.getModifydatetime() != null) {
		                	modDateTime = DateUtil.toDate(rev.getModifydatetime(), "yyyy-MM-dd");
	                    } else {
	                    	modDateTime = DateUtil.toDate(rev.getCreatedatetime(), "yyyy-MM-dd");
	                    }
	                } else if (rev.getCreatedatetime()!= null) {
	                	modDateTime =today;
	                } else {
	                	bookingDate =today;
	                    modDateTime =today;
	                }
	                
	                StaahRoomTypes staahRoomTypes = staahRoomTypesRepository.findByStaahIdAndClientId(roomId, source.getClient().getId());
	                if (staahRoomTypes == null) {
	                    staahRoomTypes = new StaahRoomTypes();
	                    staahRoomTypes.setRegDate(today);
	                    staahRoomTypes.setName(rev.getRoomTypeName());
	                    staahRoomTypes.setStaahId(roomId);
	                    staahRoomTypes.setClientId(source.getClient().getId());
	                    staahRoomTypes = staahRoomTypesRepository.save(staahRoomTypes);
	                }
	                RoomTypeMappings roomTypemapping = roomTypeMappingsRepository.findByClientRoomTypeAndClientId(roomId, source.getClient().getId());
	                if (roomTypemapping == null) {
	                    roomTypemapping = new RoomTypeMappings();
	                    roomTypemapping.setClientId(source.getClient().getId());
	                    roomTypemapping.setStatus("active");
	                    roomTypemapping.setType("ezee");
	                    roomTypemapping.setClientRoomType((long) staahRoomTypes.getId());
	                    RoomTypes roomTypes = roomTypesRepository.findByName("Uncategorized");
	                    roomTypemapping.setRoomTypeId(roomTypes.getId());
	                    roomTypemapping.setCapacity(1);
	                    roomTypeMappingsRepository.save(roomTypemapping);
	                }
	                
	                for (RESResponse.Reservations.Reservation.BookByInfo.BookingTran.RentalInfo bookByInfo : rev.getRentalInfo()) {
	                	totalAdults= checkInteger(totalAdults) + bookByInfo.getAdult();
	                	totalChildren= checkInteger(totalChildren) + bookByInfo.getChild();
	                	netAmount = bookByInfo.getRentBeforeTax().doubleValue() + netAmount;
	                }
	                String channelManager = "ezee";
	                try {
		            	setBookings(source.getClient().getId(), source.getClient().getHotelId(), source.getClient().getCmHotel(), String.valueOf(rev.getTransactionId()), bookingDate,
		                		checkinDate, checkOutDate, channelManager,rev.getStatus(),reservation.getBookedBy(), rev.getVoucherNo(),
		                		noOfRooms, roomId,rev.getRoomTypeName(), null, rev.getRateplanCode().longValue(), rev.getCurrencyCode(), rev.getTotalRate().floatValue(),
		                		priceDate, rev.getTotalRate().doubleValue(), netAmount, totalAdults, totalChildren,modDateTime,roomTypemapping.getRoomTypeId(),rev.getTACommision().doubleValue(),null,source.getClient().getCurrency());
	                }catch(Exception ce) {
	                  	ce.printStackTrace();
	                  }
	            }
	        }
        }
        
        if(source.getIntegerationType().equals(IntegrationType.STAAHMAX.toString())) {
            List<DailyBookingResponse.Reservations> reservations = source.getDailyBookingResponse().getReservations();
            for (DailyBookingResponse.Reservations staahBookingDto : reservations) {
                staahBookingDto.setReservation_code(subString(staahBookingDto.getReservation_code()));
                List<Bookings> bookings = bookingsRepository.findAllByBookingNoAndClientId(staahBookingDto.getReservation_code(),source.getClient().getId());
                if (bookings != null && !bookings.isEmpty()) {
                    bookings.forEach(b->{
                        bookingPaceOccupancyByDateRepository.deleteByBookingIdAndClientId(b.getId(),b.getClientId());
                    });

                    bookingsRepository.deleteByBookingNo(staahBookingDto.getReservation_code());
                }
            }       
	        for (DailyBookingResponse.Reservations reservation : reservations) {
	            reservation.getRoom_info().forEach(room -> {
	                Date bookingDate = DateUtil.toDateByAllFormat(reservation.getDate(), "yyyy-MM-dd");
	                Date checkinDate = DateUtil.toDateByAllFormat(reservation.getCheckin_at(), "yyyy-MM-dd");
	                Date checkOutDate = DateUtil.toDateByAllFormat(reservation.getCheckout_at(), "yyyy-MM-dd");
	                Date priceDate = DateUtil.toDateByAllFormat(reservation.getDate(), "yyyy-MM-dd");
	                Date modDateTime = null;
	                Long rateId = 0L;
	                Float rateValue = room.getPrice().get(0).getAmount();
	                if(room.getPrice().get(0).getRate_code_group()!=null) {
	                	rateId= room.getPrice().get(0).getRate_code_group().longValue();
	                }
	                Integer totalAdults = 0;
	                Integer totalChildren = 0;
	                for (DailyBookingResponse.Reservations.RoomInfo room1 : reservation.getRoom_info()){
	                	totalAdults= checkInteger(totalAdults) + checkInteger(room1.getNumber_adults());
	                	totalChildren= checkInteger(totalChildren) + checkInteger(room1.getNumber_children());
	                }               
	                String channelManager = "staah All";
	                Date bookingDatePlusOne = DateUtil.addDays(bookingDate,(1));
	                if(bookingDatePlusOne.before(today) && (bookingDatePlusOne.before(checkinDate)||bookingDatePlusOne.equals(checkinDate)) 
	                		&& (ConstantUtil.CANCELLED_STATUS.contains(reservation.getStatus()) || ConstantUtil.MODIFIED_STATUS.contains(reservation.getStatus()))) {
	                	modDateTime = bookingDatePlusOne;
	                }else {
	                	modDateTime = bookingDate;
	                }
	                try {
	                setBookings(source.getClient().getId(), source.getClient().getHotelId(), source.getClient().getCmHotel(), reservation.getReservation_code(), bookingDate,
	                		checkinDate, checkOutDate, channelManager, reservation.getStatus(), reservation.getTravel_agent(), reservation.getReservation_code(),
	                		reservation.getRoom_count(), Long.valueOf(room.getRoom_number()),
	                		room.getRoom_type(), rateId, rateId, reservation.getCurrency_code(), rateValue, priceDate, reservation.getTotalprice(), room.getTotalprice(), totalAdults,
	                		totalChildren,modDateTime,null,null,null,source.getClient().getCurrency());
	                }catch(Exception ce) {
	                  	ce.printStackTrace();
	                  }
	            });
	        }
        }
        if(source.getIntegerationType().equals(IntegrationType.STAAH.toString())) {
          List<StaahBookingDto> bookingsMergeDtoList = getMergedStaahBookingDtos(source.getStaahBookingDtos());   		
	  		  for (StaahBookingDto staahBookingDto : bookingsMergeDtoList) {
		  		  staahBookingDto.setBookingNo(subString(staahBookingDto.getBookingNo()));
		  		  List<Bookings> bookings =bookingsRepository.findAllByBookingNoAndClientId(staahBookingDto.getBookingNo(),source.getClient().getId());
		  		  if (bookings != null && !bookings.isEmpty()) 
		  		  {
		  			  bookings.forEach(b->{
		  				  bookingPaceOccupancyByDateRepository.deleteByBookingIdAndClientId(b.getId(),b.getClientId()); 				  
		  			  });		  
		  			  bookings.forEach(b->{
		  				  bookingsRepository.deleteById(b.getId());
		  			  });
		  		  		
		  		  }else {
		  			  
		  			Date bookingDate = DateUtil.toHourDateByAllFormat(staahBookingDto.getTimeBooked(), "yyyy-MM-dd HH:mm:ss");;
	                Date checkinDate = DateUtil.toDateByAllFormat(staahBookingDto.getCheckinDate(), "yyyy-MM-dd");
	                Date checkOutDate = DateUtil.toDateByAllFormat(staahBookingDto.getCheckoutDate(), "yyyy-MM-dd");
	                Date modDateTime = DateUtil.toHourDateByAllFormat(staahBookingDto.getTimeModified(), "yyyy-MM-dd HH:mm:ss");
	                String channelManager = "staah";
	                StaahRoomTypes roomTypes = getStaahRoomTypes(source.getClient(), staahBookingDto);
					try {	 
						setBookings(source.getClient().getId(), source.getClient().getHotelId(),
						  source.getClient().getCmHotel(), staahBookingDto.getBookingNo(),
						  bookingDate, checkinDate, checkOutDate, channelManager,
						  staahBookingDto.getStatus(), staahBookingDto.getChannel(),
						  staahBookingDto.getChannelRef(), convertStringValueToInt(staahBookingDto.getNoOfRooms()),
						  convertStringValueToLong(staahBookingDto.getRoomId()), roomTypes.getId().toString(),
						  convertStringValueToLong(staahBookingDto.getRateId()),
						  convertStringValueToLong(staahBookingDto.getRatePlan()),
						  staahBookingDto.getCurrency(), null, null,
						  convertToDouble(staahBookingDto.getTotalAmount()), convertToDouble(staahBookingDto.getNetAmount())
						  , convertStringValueToInt(staahBookingDto.getNoOfExtraAdult()),
						  convertStringValueToInt(staahBookingDto.getNoOfChild()),modDateTime,
						  roomTypes.getId(),convertToDouble(staahBookingDto.getCommision()),staahBookingDto.getAddon(),source.getClient().getCurrency());
                  }catch(Exception ce) {
                  	ce.printStackTrace();
                  }
						 
		  		  }
	  		 }
        }
        
        if(source.getIntegerationType().equals(IntegrationType.EGLOBE.toString())) {
        	List<EGlobeBookingResponse> eglobeBookingReponseList = source.getEGlobeBookingResponseList(); 
        	for(EGlobeBookingResponse eGlobeBookingResponse : eglobeBookingReponseList) {
            	List<Bookings> bookings = bookingsRepository.findAllByBookingNoAndClientId(eGlobeBookingResponse.getBookingCode(),source.getClient().getId());
                if (bookings != null && !bookings.isEmpty()) {
                    bookings.forEach(b->{
                        bookingPaceOccupancyByDateRepository.deleteByBookingIdAndClientId(b.getId(),b.getClientId());
                    });
                    bookingsRepository.deleteByBookingNo(eGlobeBookingResponse.getBookingCode());
                }
            }      
            for (EGlobeBookingResponse reservation : eglobeBookingReponseList) {
            	if(reservation.getRooms()!=null) {
            		String channelManager = IntegrationType.EGLOBE.name();
            		Date bookingDate = DateUtil.toDateByAllFormat(reservation.getDateBooked(), ConstantUtil.SPACEDDMMMYYYYHHMMA);
                    Date bookingDatePlusOne = DateUtil.addDays(bookingDate,(1));
                    Date checkinDate = DateUtil.toDateByAllFormat(reservation.getCheckIn(), ConstantUtil.SPACEDDMMMYYYY);
                    Date checkOutDate =DateUtil.toDateByAllFormat(reservation.getCheckOut(),ConstantUtil.SPACEDDMMMYYYY);
                    EGlobeRoomDto room = reservation.getRooms().get(0);
                    //Parameter paramOptCurrency =parameterRepository.findByClientIdAndParamName(source.getClient().getId(), ConstantUtil.CURRENCY).orElse(null);
                    String currency = source.getClient().getCurrency(); //paramOptCurrency.getParamValue();
                    Double billedAmount = reservation.getBilledAmount();
	                Date modDateTime = null;
	                Long ratePlanCode = null;
	                try {
	                	ratePlanCode = Long.parseLong(room.getRoomRates().get(0).getRatePlanCode());
	                }catch(Exception ce) {
	                	ce.printStackTrace();
	                }
	                Integer numofRoom = reservation.getRooms().size();
	                if(bookingDatePlusOne.before(today) && (bookingDatePlusOne.before(checkinDate)||bookingDatePlusOne.equals(checkinDate)) 
	                		&& (ConstantUtil.CANCELLED_STATUS.contains(reservation.getBookingStatus()) || ConstantUtil.MODIFIED_STATUS.contains(reservation.getBookingStatus()))) {
	                	modDateTime = bookingDatePlusOne;
	                }else {
	                	modDateTime = bookingDate;
	                }
                    try {
					  setBookings(source.getClient().getId(), source.getClient().getHotelId(),
							  source.getClient().getCmHotel(), reservation.getBookingCode(),
							  bookingDate, checkinDate, checkOutDate, channelManager,
							  reservation.getBookingStatus(), reservation.getChannelName(),
							  reservation.getBookingCode(),numofRoom ,
							  Long.valueOf(room.getRoomTypeCode()),null,
							  ratePlanCode,
							  null,currency, new Double(billedAmount).floatValue(), bookingDate,
							  billedAmount, billedAmount,reservation.getTotalAdults(),reservation.getTotalChildren(),modDateTime,
							  null,null,null,source.getClient().getCurrency());
                    }catch(Exception ce) {
                    	ce.printStackTrace();
                    }
            	}
            }
        }
        }catch(Exception ce) {
        	ce.printStackTrace();
        }
        Clients client = source.getClient();
        client.setSystemToday(DateUtil.setTimeToZero(new Date()));
        clientsRepository.save(client);
        return bookingsList;
	}
	
	private Integer getLos(Date checkoutDate, Date checkInDate) {
        long difference = checkoutDate.getTime() - checkInDate.getTime();
        return Math.toIntExact((difference / (1000 * 60 * 60 * 24)));
    }
	
	private synchronized Bookings setBookings (Integer clientId, Integer hotelId, Integer cmHotelId, String bookingNo, Date bookingDate, Date checkinDate, Date checkOutDate,
			String channelManager, String bookingStatus, String channel, String channelRef, Integer noOfRooms,Long roomId, String roomType, Long rateId,Long ratePlan,
			String currency, Float rateValue, Date priceDate, Double totalAmount, Double netAmount, Integer totalAdults, Integer totalChildren,Date modDateTime,Integer roomTypeId,
			Double commission,String addOn,String baseCurrency) {
		Bookings bookings = bookingsRepository.findByBookingNoAndClientId(bookingNo,clientId);
        if (bookings == null || bookings.getStatus().equalsIgnoreCase("M")) {
            bookings = new Bookings();
        }
        Date today = new Date();
        bookings.setClientId(clientId);
        bookings.setHotelId(hotelId);
        bookings.setCmId(cmHotelId);
        bookings.setChannelManager(channelManager);
        bookings.setBookingDateTime(bookingDate);
        bookings.setBookingNo(bookingNo);       
        if(channel!=null && !channel.trim().equals("")) {
            bookings.setChannelString(channel);
        }else {
        	 bookings.setChannelString(ConstantUtil.WEBDIRECT);
        }               
        if(channelRef!=null && !channelRef.trim().equals("")) {
            bookings.setChannelRef(channelRef);
        }else {
             bookings.setChannelRef("");
        }

        bookings.setCmStatus(bookingStatus);
        OtaMappings otas = getOtas(clientId, channel);
        bookings.setOtaId(otas.getId());
        bookings.setCheckinDate(checkinDate);
        bookings.setCheckoutDate(checkOutDate);
        bookings.setLos(getLos(bookings.getCheckoutDate(),bookings.getCheckinDate()));
        bookings.setNoOfRooms(noOfRooms);
        bookings.setCmRoomId(roomId);
        bookings.setCmRoomTypeString(roomType); 
        bookings.setRoomType(roomTypeId);
        if(rateId!=null) {
        	bookings.setRateId(rateId);
        }
        bookings.setRatePlan(ratePlan);
        if(baseCurrency!=null && currency!=null && !baseCurrency.equals(currency)) {
	        Parameter isApplyCurrencyConv = parameterRepository.findByClientIdAndParamName(clientId, ConstantUtil.APPLY_CURRENCY_CONVERSION).orElse(null);
	        if(isApplyCurrencyConv!=null && isApplyCurrencyConv.getParamValue().equals(ConstantUtil.YES)) {
	        	Parameter isApplyFixedConv = parameterRepository.findByClientIdAndParamName(clientId, ConstantUtil.APPLY_FIXED_CONVERSION).orElse(null);
	        	if(isApplyFixedConv!=null && isApplyFixedConv.getParamValue().equals(ConstantUtil.YES)) {
	        		Parameter currencyConvFact = parameterRepository.findByClientIdAndParamName(clientId, ConstantUtil.CURRENCY_CONVERSION_FACTOR).orElse(null);
	            	if(currencyConvFact!=null) {
	            		Integer convFact= Integer.parseInt(currencyConvFact.getParamValue());
	            		totalAmount = totalAmount * convFact;
	            		netAmount = netAmount * convFact;
	            	}           		
	        	}
	        }
    	}
        bookings.setCurrency(currency);
        bookings.setRateValue(rateValue);
        bookings.setPriceDate(priceDate);
        bookings.setTotalAmount(totalAmount);
        bookings.setNetAmount(netAmount);
        bookings.setTotalAdults(totalAdults);
        bookings.setTotalChildren(totalChildren);
        bookings.setRegdateRate(today);
        bookings.setRegDate(today);
        bookings.setModDateTime(modDateTime);
        bookings.setCommission(commission);
        bookings.setAddon(addOn);
        bookings.setStatus(ConstantUtil.ACTIVE);
        //occupancyByDatePopulationService.updateOrInsertOccupancyByDate(bookings, bookings.getNetAmount(), bookings.getNoOfRooms());
        Bookings save = bookingsRepository.save(bookings);
        saveBookingPaceRecord(save);
        return bookings;
	}
    private OtaMappings getOtas(Integer clientId, String channel) {
        if (channel == null || channel.equals("-1") || channel.trim().equals("")) {
        	channel = "WEBDIRECT" ;
        }
        OtaMappings otaMappings = otaMappingsRepository.findFirstByClientIdAndTypeAndClientOta(clientId, ConstantUtil.CHANNELMAN, channel.toLowerCase());
        if (otaMappings == null) {
            otaMappings = new OtaMappings();
            otaMappings.setClientOta(channel);
            otaMappings.setClientId(clientId);
            otaMappings.setOtaId(1);
            otaMappings.setRegdate(new Date());
            otaMappings.setType(ConstantUtil.CHANNELMAN);
            otaMappings = otaMappingsRepository.save(otaMappings);
        }
        return otaMappings;
    }
    
    private Double currencyConversion (Double totalAmount, Integer clientId,String baseCurrency,String currentCurrency) {   	
        try {
        	if(baseCurrency!=null && currentCurrency!=null && !baseCurrency.equals(currentCurrency)) {
		        Parameter isApplyCurrencyConv = parameterRepository.findByClientIdAndParamName(clientId, ConstantUtil.APPLY_CURRENCY_CONVERSION).orElse(null);
		        if(isApplyCurrencyConv!=null && isApplyCurrencyConv.getParamValue().equals(ConstantUtil.YES)) {
		        	Parameter isApplyFixedConv = parameterRepository.findByClientIdAndParamName(clientId, ConstantUtil.APPLY_FIXED_CONVERSION).orElse(null);
		        	if(isApplyFixedConv!=null && isApplyFixedConv.getParamValue().equals(ConstantUtil.YES)) {
		        		Parameter currencyConvFact = parameterRepository.findByClientIdAndParamName(clientId, ConstantUtil.CURRENCY_CONVERSION_FACTOR).orElse(null);
		            	if(currencyConvFact!=null) {
		            		Integer convFact= Integer.parseInt(currencyConvFact.getParamValue());
		            		totalAmount = totalAmount * convFact;
		            	}           		
		        	}
		        }
        	}
	        return totalAmount;
        }catch(Exception ce) {
        	ce.printStackTrace();
        	return totalAmount;
        }
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
    
    private List<StaahBookingDto> getMergedStaahBookingDtos(List<StaahBookingDto> staahBookingDtos) {
        List<StaahBookingDto> bookingsMergeDtoList = new ArrayList<>();
        Map<String, List<StaahBookingDto>> staahBookingGrouped = staahBookingDtos.stream().collect(Collectors.groupingBy(p -> p.getBookingNo()));
        staahBookingGrouped.forEach((k, v) -> {
            if (v.size() > 1) {
                StaahBookingDto mergedDto = new StaahBookingDto();
                v.stream().forEach(staahBookingDto -> {
                    if (mergedDto.getBookingNo() == null) {
                        BeanUtils.copyProperties(staahBookingDto, mergedDto);
                    } else {
                        if (staahBookingDto.isConfirmed()) {
                            mergedDto.setTotalAmount(String.valueOf(convertToDouble(mergedDto.getTotalAmount())));
                            mergedDto.setNetAmount(String.valueOf(convertToDouble(mergedDto.getNetAmount())));
                            mergedDto.setNoOfRooms(String.valueOf(convertToDouble(mergedDto.getNoOfRooms()) + convertToDouble(staahBookingDto.getNoOfRooms())));
                            mergedDto.setCheckoutDate(staahBookingDto.getCheckoutDate());
                        }
                        if (staahBookingDto.isCanceled()) {
                            mergedDto.setTotalAmount(String.valueOf(convertToDouble(mergedDto.getTotalAmount()) - convertToDouble(staahBookingDto.getTotalAmount())));
                            mergedDto.setNetAmount(String.valueOf(convertToDouble(mergedDto.getNetAmount()) - convertToDouble(staahBookingDto.getNetAmount())));
                            mergedDto.setNoOfRooms(String.valueOf(convertToDouble(staahBookingDto.getNoOfRooms()) - convertToDouble(mergedDto.getNoOfRooms())));
                            mergedDto.setCheckoutDate(staahBookingDto.getCheckoutDate());
                        }
                        if (staahBookingDto.isModified()) {
                            mergedDto.setTotalAmount(String.valueOf(convertToDouble(staahBookingDto.getTotalAmount())));
                            mergedDto.setNetAmount(String.valueOf(convertToDouble(staahBookingDto.getNetAmount())));
                            if (staahBookingDto.getNoOfRooms() != null) {
                                mergedDto.setNoOfRooms(String.valueOf(convertToDouble(staahBookingDto.getNoOfRooms()) + convertToDouble(mergedDto.getNoOfRooms())));
                            }else {
                                mergedDto.setNoOfRooms(String.valueOf(convertToDouble(mergedDto.getNoOfRooms())));
                            }
                            mergedDto.setCheckoutDate(staahBookingDto.getCheckoutDate());
                        }
                    }
                });
                bookingsMergeDtoList.add(mergedDto);
            } else {
                if (v.get(0) != null) {
                    bookingsMergeDtoList.add(v.get(0));
                }
            }
        });
        return bookingsMergeDtoList;
    }
    
    private StaahRoomTypes getStaahRoomTypes(Clients clients, StaahBookingDto staahBookingDto) {
        StaahRoomTypes roomTypes = staahRoomTypesRepository.findByClientIdAndStaahId(clients.getId(), convertStringValueToLong(staahBookingDto.getRoomId()));
        if (roomTypes == null) {
            roomTypes = new StaahRoomTypes();
            roomTypes.setClientId(clients.getId());
            roomTypes.setName(staahBookingDto.getRoomType());
            roomTypes.setStaahId(convertStringValueToLong(staahBookingDto.getRoomId()));
            roomTypes.setRegDate(new Date());
            roomTypes = staahRoomTypesRepository.save(roomTypes);
        }
        return roomTypes;
    }


    private Double convertToDouble(String value) {
        return value == null ? 0d : Double.valueOf(value);
    }

    private Integer checkInteger(Integer value) {
        return value == null ? 0 : value;
    }
    
    private Integer convertStringValueToInt(String value) {
        return value == null ? 0 : Double.valueOf(value).intValue();
    }
    
    private Long convertStringValueToLong(String value) {
        return value == null ? 0 : Double.valueOf(value).longValue();
    }

    private String subString(String bookingNo){
        bookingNo = bookingNo.replace("'", "");
        return bookingNo;
    }
    private int compareTo(Date dateOne, Date dateTwo) {
        return DateUtil.setTimeToZero(dateOne).compareTo(dateTwo);
    }

}
