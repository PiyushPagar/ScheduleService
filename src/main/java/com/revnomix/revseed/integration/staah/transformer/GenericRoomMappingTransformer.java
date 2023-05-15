package com.revnomix.revseed.integration.staah.transformer;

import java.util.ArrayList;

import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.transformer.GenericTransformer;
import org.springframework.stereotype.Component;

import com.revnomix.revseed.model.StaahRateTypes;
import com.revnomix.revseed.model.StaahRoomTypes;
import com.revnomix.revseed.repository.StaahRateTypesRepository;
import com.revnomix.revseed.repository.StaahRoomTypesRepository;
import com.revnomix.revseed.Service.ParameterService;
import com.revnomix.revseed.Util.ConstantUtil;
import com.revnomix.revseed.integration.eglobe.dto.EglobeRoomMapping;
import com.revnomix.revseed.integration.integration.IntegrationType;
import com.revnomix.revseed.integration.staahMax.dto.GenericIntegrationDto;
import com.revnomix.revseed.integration.staahMax.dto.RoomMapping;
import com.revnomix.revseed.integration.staahMax.dto.RoomMapping.RatePlans;
import com.revnomix.revseed.integration.staahMax.dto.RoomMapping.RoomType;

@Component
public class GenericRoomMappingTransformer  implements GenericTransformer<GenericIntegrationDto, List<StaahRateTypes>> {

    @Autowired
    private StaahRoomTypesRepository staahRoomTypesRepository;

    @Autowired
    private StaahRateTypesRepository staahRateTypesRepository;
    
    @Autowired
    private ParameterService parameterService;
    
	@Override
	public List<StaahRateTypes> transform(GenericIntegrationDto source) {
        Date today = new Date();
        List<StaahRateTypes> staahRateTypesList = new ArrayList<>();
		if(source.getIntegerationType().equals(IntegrationType.STAAH.toString())) {
	        source.getStaahPopulationDto().getRoomresponse().getRooms().getRoom().stream().forEach(r -> {
	            r.getRates().getRate().stream().forEach(rate -> {
	            	saveStaahRoomTypes(r.getRoomId().longValue(), source.getStaahPopulationDto().getClient().getId(),r.getRoomName(),today);
	            	StaahRateTypes staahRateTypes = saveStaahRateTypes(source.getStaahPopulationDto().getClient().getId(),(long)rate.getRateId(),r.getRoomId().longValue(),rate.getRateName(),today);
	            	staahRateTypesList.add(staahRateTypes);
	            });
	        });
		}
		
		if(source.getIntegerationType().equals(IntegrationType.STAAHMAX.toString())) {
			 RoomMapping roomMapping = source.getRoomMapping();

			roomMapping.getRoomtypes().forEach(room -> {
				saveStaahRoomTypes(room.getRoom_id().longValue(), source.getStaahPopulationDto().getClient().getId(),room.getRoomname(),today);
	        });
			roomMapping.getRoom_rate_mapping().forEach(rate -> {
				String staahRoomName = "";
				String staahRateName = "";
	            for(RoomType roomType : roomMapping.getRoomtypes()) {
	            	if(roomType.getMaster_rate_id()!=null) {
	            		parameterService.createMasterRateIdParametersForClients(source.getStaahPopulationDto().getClient().getId(),roomType.getMaster_rate_id(),ConstantUtil.MASTERRATEID);
	            	}
	            	if(roomType.getRoom_id().equals(rate.getRoom_id()))
	            	{
	            		staahRoomName = roomType.getRoomname();
	            	}
	            }
	            for(RatePlans ratePlan : roomMapping.getRateplans()) {
	            	
	            	if(ratePlan.getRate_id().equals(rate.getRate_id()))
	            	{
	            		staahRateName = ratePlan.getRatename();
	            	}
	            }
				StaahRateTypes staahRateTypes = saveStaahRateTypes(source.getStaahPopulationDto().getClient().getId(),(long)rate.getRate_id(),rate.getRoom_id().longValue(),staahRoomName + " " + staahRateName,today);
            	staahRateTypesList.add(staahRateTypes);
			});
		}
		
		if(source.getIntegerationType().equals(IntegrationType.EZEE.toString())) {
			source.getResResponse().getRoomInfo().getRatePlans().getRatePlan().forEach(rate -> {
				saveStaahRoomTypes(rate.getRoomTypeID(), source.getStaahPopulationDto().getClient().getId(),rate.getRoomType(),today);
            	StaahRateTypes staahRateTypes = saveStaahRateTypes(source.getStaahPopulationDto().getClient().getId(),rate.getRatePlanID(),rate.getRoomTypeID(),rate.getRateType()+" ("+rate.getRoomType()+")",today);
            	staahRateTypesList.add(staahRateTypes);
			});
		}
		
		if(source.getIntegerationType().equals(IntegrationType.EGLOBE.toString())) {
	        List<EglobeRoomMapping> eglobeList = source.getEglobeRoomMappingList();
	        eglobeList.forEach(room -> {
	        	saveStaahRoomTypes(Long.parseLong(room.getRoomCode()), source.getStaahPopulationDto().getClient().getId(),room.getRoomName(),today);
	        	room.getRatePlans().forEach(rate -> {
	        		String eglobeRoomName = "";
	                String eglobeRateName = "";
	                eglobeRoomName = room.getRoomName();
	                eglobeRateName = rate.getRatePlanName();
	        		StaahRateTypes staahRateTypes = saveStaahRateTypes(source.getStaahPopulationDto().getClient().getId(),Long.parseLong(rate.getRatePlanCode()),Long.parseLong(room.getRoomCode()),eglobeRoomName + " " + eglobeRateName,today);
	            	staahRateTypesList.add(staahRateTypes);
	        	});
	        });
		}
		return staahRateTypesList;
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

}
