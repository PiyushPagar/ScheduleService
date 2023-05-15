package com.revnomix.revseed.integration.staahMax.transformer;

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
import com.revnomix.revseed.integration.staahMax.dto.RoomMapping;
import com.revnomix.revseed.integration.staahMax.dto.RoomMapping.RatePlans;
import com.revnomix.revseed.integration.staahMax.dto.RoomMapping.RoomType;
import com.revnomix.revseed.integration.staahMax.dto.StaahMaxPopulationDto;

@Component
public class StaahMaxRoomMappingTransformer implements GenericTransformer<StaahMaxPopulationDto, List<StaahRateTypes>> {

    @Autowired
    private StaahRoomTypesRepository staahRoomTypesRepository;

    @Autowired
    private StaahRateTypesRepository staahRateTypesRepository;
    
    @Autowired
    private ParameterService parameterService;

    @Override
    public List<StaahRateTypes> transform(StaahMaxPopulationDto staahPopulationDto) {
        List<StaahRateTypes> staahRateTypesList = new ArrayList<>();

        Date today = new Date();
        RoomMapping roomMapping = staahPopulationDto.getRoomMapping();
        if(roomMapping!=null) {
	        roomMapping.getRoomtypes().forEach(room -> {
	            StaahRoomTypes staahRoomTypes = staahRoomTypesRepository.findByStaahIdAndClientId(room.getRoom_id().longValue(), staahPopulationDto.getClient().getId());
	            if (staahRoomTypes == null) {
	                staahRoomTypes = new StaahRoomTypes();
	            }
	            staahRoomTypes.setClientId(staahPopulationDto.getClient().getId());
	            staahRoomTypes.setStaahId(room.getRoom_id().longValue());
	            staahRoomTypes.setName(room.getRoomname());
	            staahRoomTypes.setRegDate(today);
	            staahRoomTypesRepository.save(staahRoomTypes);
	        });
	
	        roomMapping.getRoom_rate_mapping().forEach(rate -> {
	            StaahRateTypes staahRateTypes = staahRateTypesRepository.findByClientIdAndStaahRateIdAndStaahRoomId(
	                    staahPopulationDto.getClient().getId(), rate.getRate_id().longValue(), rate.getRoom_id().longValue());
	            if (staahRateTypes == null) {
	                staahRateTypes = new StaahRateTypes();
	            }
	            
	            staahRateTypes.setClientId(staahPopulationDto.getClient().getId());
	            staahRateTypes.setStaahRateId(rate.getRate_id().longValue());
	            staahRateTypes.setStaahRoomId(rate.getRoom_id().longValue());
	            String staahRoomName = "";
	            String staahRateName = "";
	            for(RoomType roomType : roomMapping.getRoomtypes()) {
	            	if(roomType.getMaster_rate_id()!=null) {
	            		parameterService.createMasterRateIdParametersForClients(staahPopulationDto.getClient().getId(),roomType.getMaster_rate_id(),ConstantUtil.MASTERRATEID);
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
	            staahRateTypes.setName(staahRoomName + " " + staahRateName );
	            staahRateTypes.setRegDate(today);
	            staahRateTypesRepository.save(staahRateTypes);
	            staahRateTypesList.add(staahRateTypes);
	        });
        }
        return staahRateTypesList;
    }
}
