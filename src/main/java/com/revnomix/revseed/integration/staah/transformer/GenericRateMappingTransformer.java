package com.revnomix.revseed.integration.staah.transformer;

import java.util.ArrayList;

import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.transformer.GenericTransformer;
import org.springframework.stereotype.Component;

import com.revnomix.revseed.model.Clients;
import com.revnomix.revseed.model.RoomTypeMappings;
import com.revnomix.revseed.model.StaahRateTypes;
import com.revnomix.revseed.model.StaahRates;
import com.revnomix.revseed.model.StaahRoomTypes;
import com.revnomix.revseed.repository.ClientsRepository;
import com.revnomix.revseed.repository.RoomTypeMappingsRepository;
import com.revnomix.revseed.repository.StaahRateTypesRepository;
import com.revnomix.revseed.repository.StaahRatesRepository;
import com.revnomix.revseed.repository.StaahRoomTypesRepository;
import com.revnomix.revseed.Util.DateUtil;
import com.revnomix.revseed.integration.integration.IntegrationType;
import com.revnomix.revseed.integration.staahMax.dto.GenericIntegrationDto;

@Component
public class GenericRateMappingTransformer  implements GenericTransformer<GenericIntegrationDto, List<StaahRates>> {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private StaahRateTypesRepository staahRateTypesRepository;

    @Autowired
    private StaahRoomTypesRepository staahRoomTypesRepository;

    @Autowired
    private RoomTypeMappingsRepository roomTypeMappingsRepository;

    @Autowired
    private StaahRatesRepository staahRatesRepository;
    
    @Autowired
    private ClientsRepository clientsRepository;
    
	@Override
	public List<StaahRates> transform(GenericIntegrationDto source) {
        final List<StaahRates> staahRatesList = new ArrayList<>();
        Date today = new Date();
		if(source.getIntegerationType().equals(IntegrationType.STAAH.toString())) {
			source.getStaahPopulationDto().getRateRequestSegmentsResponse().getRooms().stream().forEach(rooms -> {
	            rooms.getRoom().stream().forEach(room -> {
	            	StaahRoomTypes staahRoomTypes = staahRoomTypesRepository.findByStaahIdAndClientId(room.getRoomTypeCode().longValue(), source.getClient().getId());
                	if(staahRoomTypes!=null) {
		            	saveRoomTypeMappings(source.getClient().getId(),staahRoomTypes.getStaahId(),1,IntegrationType.STAAH.name(),room.getRoomTypeCode().longValue());
	                	StaahRateTypes staahRateTypes = saveStaahRateTypes(source.getClient().getId(),room.getRatePlanCode().longValue(),room.getRoomTypeCode().longValue(),room.getRatePlanName(),today);
		                room.getDates().getDate().stream().forEach(date -> {
		               	saveStaahRates(source.getClient().getId(),staahRoomTypes.getId(),staahRateTypes.getId(),date.getRate(),date.getEffectiveDate().toGregorianCalendar().getTime(), today);		                
		                });
                	}
	            });
	        });
		}
		if(source.getIntegerationType().equals(IntegrationType.EZEE.toString())) {
	        if(source.getResResponse()!=null && source.getResResponse().getRoomInfo()!=null) {
	        	source.getResResponse().getRoomInfo().getSource().forEach(rooms -> {
		            rooms.getRoomTypes().getRateType().forEach(room -> {
		                long noOfDays = DateUtil.daysBetween(room.getFromDate().toGregorianCalendar().getTime(),room.getToDate().toGregorianCalendar().getTime());
		                Long roomId = room.getRoomTypeID();
	                    Long rateId = room.getRateTypeID();
	                    StaahRoomTypes staahRoomTypes = staahRoomTypesRepository.findByStaahIdAndClientId(roomId, source.getClient().getId());
	                    if(staahRoomTypes!=null) {
		                    saveRoomTypeMappings(source.getClient().getId(),staahRoomTypes.getStaahId(),1,IntegrationType.STAAH.name(),roomId);
			                for (int i = 0; i <= noOfDays;i++) {
			                    StaahRateTypes staahRateTypes = staahRateTypesRepository.findByClientIdAndStaahRateIdAndStaahRoomId(source.getClient().getId(), rateId, roomId);
			                    if (staahRateTypes != null) {
			                    	saveStaahRates(source.getClient().getId(),staahRoomTypes.getId(),staahRateTypes.getId(),room.getRoomRate().getBase().doubleValue(),DateUtil.addDays(room.getFromDate().toGregorianCalendar().getTime(), i),today);
			                    }
			                }
	                    }
		            });
		        });
	        }
		}
        Clients client = source.getClient();
        client.setSystemToday(DateUtil.setTimeToZero(new Date()));
        clientsRepository.save(client);
        logger.info("data save successfully : ");
        return staahRatesList;
	}

	StaahRoomTypes saveStaahRoomTypes(Long roomId, Integer clientId,String roomName,Date regDate) {
        StaahRoomTypes staahRoomTypes = staahRoomTypesRepository.findByStaahIdAndClientId(roomId, clientId);
        if (staahRoomTypes == null) {
            staahRoomTypes = new StaahRoomTypes();
        }
        staahRoomTypes.setClientId(clientId);
        staahRoomTypes.setStaahId(roomId);
        staahRoomTypes.setName(roomName);
        staahRoomTypes.setRegDate(regDate);
        staahRoomTypesRepository.save(staahRoomTypes);
        return staahRoomTypes;
	}
	
	StaahRateTypes saveStaahRateTypes(Integer clientId,Long rateId,Long roomId,String rateName,Date regDate) {
		StaahRateTypes staahRateTypes = staahRateTypesRepository.findByClientIdAndStaahRateIdAndStaahRoomId(clientId,rateId,roomId);
        if (staahRateTypes == null) {
            staahRateTypes = new StaahRateTypes();
        }
        staahRateTypes.setClientId(clientId);
        staahRateTypes.setStaahRateId(rateId);
        staahRateTypes.setStaahRoomId(roomId);
        staahRateTypes.setName(rateName);
        staahRateTypes.setRegDate(regDate);
        staahRateTypesRepository.save(staahRateTypes);
        return staahRateTypes;
	}
	
	RoomTypeMappings saveRoomTypeMappings(Integer clientId,Long roomType,Integer staahId,String integrationType,Long roomTypeCode) {
	    RoomTypeMappings roomTypeMappings = roomTypeMappingsRepository.findByClientRoomTypeAndClientId(roomTypeCode,clientId);
	    if (roomTypeMappings == null) {
	        roomTypeMappings = new RoomTypeMappings();
	    }
        roomTypeMappings.setClientId(clientId);
        roomTypeMappings.setClientRoomType(roomType);
        roomTypeMappings.setRoomTypeId(staahId);
        roomTypeMappings.setType(integrationType);
        roomTypeMappingsRepository.save(roomTypeMappings);
	    return roomTypeMappings;
	}
	
	StaahRates saveStaahRates(Integer clientId,Integer roomTypeId,Integer rateTypeId,Double rate,Date  rateDate, Date regDate) {
	    StaahRates staahRates = staahRatesRepository.findOneByClientIdAndRateDateAndRoomTypeIdAndRateTypeId(clientId, rateDate, roomTypeId, rateTypeId);
	    if (staahRates == null) {
	        staahRates = new StaahRates();
	    }
	    staahRates.setClientId(clientId);
	    staahRates.setRegDate(regDate);
	    staahRates.setRoomTypeId(roomTypeId);
	    staahRates.setRateTypeId(rateTypeId);
	    staahRates.setRate(rate);
	    staahRates.setRateDate(rateDate);
	    staahRatesRepository.save(staahRates);
	    return staahRates;
	}
}
