package com.revnomix.revseed.integration.staah.transformer;

import java.util.ArrayList;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.beanutils.converters.DateConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.transformer.GenericTransformer;
import org.springframework.stereotype.Component;

import com.revnomix.revseed.model.BookingPaceOccupancyByDate;
import com.revnomix.revseed.model.BookingStatusMapping;
import com.revnomix.revseed.model.Bookings;
import com.revnomix.revseed.model.Clients;
import com.revnomix.revseed.model.OtaMappings;
import com.revnomix.revseed.model.Parameter;
import com.revnomix.revseed.model.StaahRoomTypes;
import com.revnomix.revseed.repository.BookingPaceOccupancyByDateRepository;
import com.revnomix.revseed.repository.BookingStatusMappingRepository;
import com.revnomix.revseed.repository.BookingsRepository;
import com.revnomix.revseed.repository.ClientsRepository;
import com.revnomix.revseed.repository.OtaMappingsRepository;
import com.revnomix.revseed.repository.ParameterRepository;
import com.revnomix.revseed.repository.RoomTypesRepository;
import com.revnomix.revseed.repository.StaahRoomTypesRepository;
import com.revnomix.revseed.Util.ConstantUtil;
import com.revnomix.revseed.Util.DateUtil;
import com.revnomix.revseed.integration.integration.IntegrationType;
import com.revnomix.revseed.integration.staah.StaahBookingDto;

@Component
public class StaahBookingTransformer implements GenericTransformer<List<StaahBookingDto>, List<Bookings>> {
    public static final String STATUS_ACTIVE = "Active";
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final DateConverter dateConverter = new DateConverter(null);
    @Autowired
    private RoomTypesRepository roomTypesRepository;

    @Autowired
    private ClientsRepository clientsRepository;

    @Autowired
    private StaahRoomTypesRepository staahRoomTypesRepository;

    @Autowired
    private OtaMappingsRepository otaMappingsRepository;

    @Autowired
    private ParameterRepository parameterRepository;
    
    @Autowired
    private BookingsRepository bookingsRepository;
    @Autowired
    private BookingPaceOccupancyByDateRepository bookingPaceOccupancyByDateRepository;

    @Autowired
    private BookingStatusMappingRepository bookingStatusMappingRepository;

    @Override
    public List<Bookings> transform(List<StaahBookingDto> staahBookingDtos) {
        List<Bookings> bookingsList = new ArrayList<>();
        Date today = new Date();
        Clients clients = null;
        try {
	        if (staahBookingDtos.size() > 0) {
	            clients = clientsRepository.findOneByCmHotelAndStatus(convertStringValueToInt(staahBookingDtos.get(0).getPropertyId()), STATUS_ACTIVE);
	            if (clients == null) {
	                logger.error("While population channel manager Id not found in client table or deactivated");
	                return bookingsList;
	            }
	        }
	        List<StaahBookingDto> bookingsMergeDtoList = getMergedStaahBookingDtos(staahBookingDtos);
			
			  for (StaahBookingDto staahBookingDto : bookingsMergeDtoList) {
			  staahBookingDto.setBookingNo(subString(staahBookingDto.getBookingNo()));
			  List<Bookings> bookings =bookingsRepository.findAllByBookingNoAndClientId(staahBookingDto.getBookingNo(),clients.getId());
			  if (bookings != null && !bookings.isEmpty()) 
			  {
				  bookings.forEach(b->{
					  bookingPaceOccupancyByDateRepository.deleteByBookingIdAndClientId(b.getId(),b.getClientId()); 				  
				  });		  
				  bookings.forEach(b->{
					  bookingsRepository.deleteById(b.getId());
				  });
			  		
			  } 
			 }
			 
	        for (StaahBookingDto staahBookingDto : bookingsMergeDtoList) {
	
	            boolean flagAlreadyExist = true;
	            Double diffNetAmount = 0d;
	            Integer diffNoOfRooms = 0;
	            Bookings bookings = bookingsRepository.findByBookingNoAndClientId(staahBookingDto.getBookingNo(),clients.getId());
	            if (bookings == null || !staahBookingDto.getStatus().equalsIgnoreCase(bookings.getCmStatus()) || staahBookingDto.getStatus().equalsIgnoreCase("M")) {
	                if (bookings == null) {
	                    bookings = new Bookings();
	                    flagAlreadyExist = false;
	                } else {
	                    diffNetAmount = bookings.getNetAmount() - convertToDouble(staahBookingDto.getNetAmount());
	                    diffNoOfRooms = bookings.getNoOfRooms() - convertStringValueToInt(staahBookingDto.getNoOfRooms());
	                }
	
	                bookings.setClientId(clients.getId());
	                bookings.setHotelId(clients.getHotelId());
	                StaahRoomTypes roomTypes = getStaahRoomTypes(clients, staahBookingDto);
	                //bookings.setCmId(convertStringValueToInt(staahBookingDto.getPropertyId()));
	                bookings.setCmStatus(staahBookingDto.getStatus());
	                List<BookingStatusMapping> bookingStatusMappingList = bookingStatusMappingRepository.findByBookingStatusAndChannelManager(staahBookingDto.getStatus(),IntegrationType.STAAH.toString());
	                if(bookingStatusMappingList!=null && bookingStatusMappingList.size()<1) {
	                	BookingStatusMapping bookingStatusMapping = new BookingStatusMapping();
	                	bookingStatusMapping.setBookingStatus(staahBookingDto.getStatus());
	                	bookingStatusMapping.setChannelManager(IntegrationType.STAAH.toString());
	                	bookingStatusMapping.setStatus(ConstantUtil.ACTIVE);
	                	bookingStatusMappingRepository.save(bookingStatusMapping);
	                }
	                bookings.setRoomType(roomTypes.getId());
	                OtaMappings otas = getOtas(staahBookingDto, clients);
	                bookings.setOtaId(otas.getId());
	                bookings.setNoOfRooms(convertStringValueToInt(staahBookingDto.getNoOfRooms()));
	                bookings.setChannelRef(staahBookingDto.getChannelRef());
	                bookings.setChannelString(staahBookingDto.getChannel());
	                if(staahBookingDto.getChannel()!=null && !staahBookingDto.getChannel().trim().equals("")) {
		                bookings.setChannelString(staahBookingDto.getChannel());
	                }else {
	                	 bookings.setChannelString(ConstantUtil.WEBDIRECT);
	                }               
	                if(staahBookingDto.getChannelRef()!=null && !staahBookingDto.getChannelRef().trim().equals("")) {
		                bookings.setChannelRef(staahBookingDto.getChannelRef());
	                }else {
	 	                bookings.setChannelRef(staahBookingDto.getBookingNo());
	                }
	                /*dateConverter.setPattern(RevseedFileType.STAAH_BOOKING.getDatePattern());
	                dateConverter.setPattern("yyyy-MM-dd");*/
	                bookings.setCheckinDate(DateUtil.toDateByAllFormat(staahBookingDto.getCheckinDate(), "yyyy-MM-dd"));
	                bookings.setCheckoutDate(DateUtil.toDateByAllFormat(staahBookingDto.getCheckoutDate(), "yyyy-MM-dd"));
	                bookings.setLos(getLos(bookings.getCheckoutDate(), bookings.getCheckinDate()));
	                bookings.setAddon(staahBookingDto.getAddon());
	                //dateConverter.setPattern("yyyy-MM-dd HH:mm:ss");
	                Date bookingDateTime = DateUtil.toHourDateByAllFormat(staahBookingDto.getTimeBooked(), "yyyy-MM-dd HH:mm:ss");
	                Date modifiedDateTime = DateUtil.toHourDateByAllFormat(staahBookingDto.getTimeModified(), "yyyy-MM-dd HH:mm:ss");
	
	                if (staahBookingDto.getTimeBooked() == null || getChar(bookingDateTime) == '0') {
	                    bookings.setBookingDateTime(bookings.getCheckinDate());
	                }else {
	                    bookings.setBookingDateTime(bookingDateTime);
	                }
	                if (modifiedDateTime == null || getChar(modifiedDateTime) == '0') {
	                    bookings.setModDateTime(bookings.getBookingDateTime());
	                }else{
	                    bookings.setModDateTime(modifiedDateTime);
	                }
	                bookings.setBookingNo(staahBookingDto.getBookingNo());
	                
	                bookings.setRateId(convertStringValueToLong(staahBookingDto.getRateId()));
	                bookings.setRatePlan(convertStringValueToLong(staahBookingDto.getRatePlan()));
	                bookings.setCurrency(staahBookingDto.getCurrency());
	                //staahBookingDto.getRateValue(staahRateTypesRepository.).
	                //bookings.setRateValue();
	                Double totalAmount = null;
	                Double netAmount = null;
	                if (staahBookingDto.getNetAmount() != null) {
	                	totalAmount = convertToDouble(staahBookingDto.getTotalAmount());
	                }
	                if (staahBookingDto.getNetAmount() != null) {
	                	netAmount = convertToDouble(staahBookingDto.getNetAmount());
	                }
	                if(clients.getCurrency()!=null && staahBookingDto.getCurrency()!=null && !clients.getCurrency().equals(staahBookingDto.getCurrency())) {
	        	        Parameter isApplyCurrencyConv = parameterRepository.findByClientIdAndParamName(clients.getId(), ConstantUtil.APPLY_CURRENCY_CONVERSION).orElse(null);
	        	        if(isApplyCurrencyConv!=null && isApplyCurrencyConv.getParamValue().equals(ConstantUtil.YES)) {
	        	        	Parameter isApplyFixedConv = parameterRepository.findByClientIdAndParamName(clients.getId(), ConstantUtil.APPLY_FIXED_CONVERSION).orElse(null);
	        	        	if(isApplyFixedConv!=null && isApplyFixedConv.getParamValue().equals(ConstantUtil.YES)) {
	        	        		Parameter currencyConvFact = parameterRepository.findByClientIdAndParamName(clients.getId(), ConstantUtil.CURRENCY_CONVERSION_FACTOR).orElse(null);
	        	            	if(currencyConvFact!=null) {
	        	            		Integer convFact= Integer.parseInt(currencyConvFact.getParamValue());
	        	            		totalAmount = totalAmount * convFact;
	        	            		netAmount = netAmount * convFact;
	        	            	}           		
	        	        	}
	        	        }
	            	}
	                
	                bookings.setCommission(convertToDouble(staahBookingDto.getCommision()));
	                bookings.setTaxValue(convertToDouble(staahBookingDto.getTaxValue()));
	                bookings.setTotalAmount(totalAmount);
	                bookings.setNetAmount(netAmount);
	                bookings.setNoOfExtraAdults(convertStringValueToInt(staahBookingDto.getNoOfExtraAdult()));
	                bookings.setExtraAdultRate(convertToDouble(staahBookingDto.getExtraAdultRate()));
	                bookings.setNoOfExtraChildren(convertStringValueToInt(staahBookingDto.getNoOfChild()));
	                bookings.setExtraChildrenRate(convertToDouble(staahBookingDto.getExtraChildRate()));
	                bookings.setRatePlan(convertStringValueToLong(staahBookingDto.getRatePlan()));
	                bookings.setAddonRate(convertToDouble(staahBookingDto.getAddonRate()));
	                bookings.setStatus(ConstantUtil.ACTIVE);
	                bookings.setChannelManager("staah");
	                bookings.setCmRoomTypeString(staahBookingDto.getRoomType());
	                if (staahBookingDto.getRoomId() != null) {
	                    bookings.setCmRoomId(convertStringValueToLong(staahBookingDto.getRoomId()));
	                }
	                bookings.setRegDate(today);
	                if (bookings.getChannelString() == null || bookings.getChannelString().isEmpty() || bookings.getChannelString().equals("-1")) {
	                    bookings.setChannelString("WEB_DIRECT");
	                }
	                Bookings save = bookingsRepository.save(bookings);
	                saveBookingPaceRecord(save);
	            }
	        }
        }catch(Exception ce) {
        	ce.printStackTrace();
        }
        logger.info("Population of bookings file has been completed");
        clients.setSystemToday(DateUtil.setTimeToZero(new Date()));
        clientsRepository.save(clients);
        return bookingsList;
    }

    private int compareTo(Date dateOne, Date dateTwo) {
        return DateUtil.setTimeToZero(dateOne).compareTo(dateTwo);
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
            paceOccupancyByDate.setPace(0L);
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

                            //This code is commented because the amount is adding while uploading the excel workbench
//                            mergedDto.setTotalAmount(String.valueOf(convertToDouble(mergedDto.getTotalAmount()) + convertToDouble(staahBookingDto.getTotalAmount())));
//                            mergedDto.setNetAmount(String.valueOf(convertToDouble(mergedDto.getNetAmount()) + convertToDouble(staahBookingDto.getNetAmount())));
//                            mergedDto.setNoOfRooms(String.valueOf(convertToDouble(mergedDto.getNoOfRooms()) + convertToDouble(staahBookingDto.getNoOfRooms())));
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

    private OtaMappings getOtas(StaahBookingDto staahBookingDto, Clients clients) {
        if (staahBookingDto.getChannel() == null || staahBookingDto.getChannel().equals("-1") || staahBookingDto.getChannel().trim().equals("")) {
            staahBookingDto.setChannel("WEBDIRECT");
        }
        OtaMappings otaMappings = otaMappingsRepository.findFirstByClientIdAndTypeAndClientOta(clients.getId(),ConstantUtil.CHANNELMAN, staahBookingDto.getChannel().toLowerCase());
        if (otaMappings == null) {
            otaMappings = new OtaMappings();
            otaMappings.setClientOta(staahBookingDto.getChannel());
            otaMappings.setClientId(clients.getId());
            otaMappings.setOtaId(1);
            otaMappings.setRegdate(new Date());
            otaMappings.setType(ConstantUtil.CHANNELMAN);
            otaMappings = otaMappingsRepository.save(otaMappings);
        }
        return otaMappings;
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
    	try {
    		return value == null ? 0d : Double.valueOf(value);
    	}catch(Exception ce) {
    		ce.printStackTrace();
    		return 0d;
    	}
    }

    private Integer convertStringValueToInt(String value) {
    	try {
        return value == null ? 0 : Double.valueOf(value).intValue();
    	}catch(Exception ce) {
    		ce.printStackTrace();
    		return 0;
    	}
    }

    private Long convertStringValueToLong(String value) {
    	try {
        return value == null ? 0 : Double.valueOf(value).longValue();
    	}catch(Exception ce) {
    		ce.printStackTrace();
    		return 0l;
    	}
    }

    private Float convertStringValueTotFloat(String value) {
    	try {
        return value == null ? 0f : convertToDouble(value).floatValue();
    	}catch(Exception ce) {
    		ce.printStackTrace();
    		return 0f;
    	}
    }

    private Integer getLos(Date checkoutDate, Date checkInDate) {
    	try {
	        long difference = checkoutDate.getTime() - checkInDate.getTime();
	        return Math.toIntExact((difference / (1000 * 60 * 60 * 24)));
    	}catch(Exception ce) {
    		ce.printStackTrace();
    		return 0;
    	}
    }

    private String subString(String bookingNo){
    	try {
        bookingNo = bookingNo.replace("'", "");
        return bookingNo;
    	}catch(Exception ce) {
    		ce.printStackTrace();
    		return bookingNo;
    	}
    }

    private char getChar(Date date){
	        String bookingDate = DateUtil.formatDate(date, "yyyy-MM-dd HH:mm:ss");
	        return bookingDate.charAt(0);
    }
}
