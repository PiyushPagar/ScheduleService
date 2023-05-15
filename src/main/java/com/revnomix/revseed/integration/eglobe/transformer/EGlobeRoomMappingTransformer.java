package com.revnomix.revseed.integration.eglobe.transformer;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.transformer.GenericTransformer;
import org.springframework.stereotype.Component;

import com.revnomix.revseed.integration.eglobe.dto.EGlobePopulationDto;
import com.revnomix.revseed.integration.eglobe.dto.EglobeRoomMapping;
//import com.revnomix.revseed.integration.integration.eglobe.dto.EGlobePopulationDto;
//import com.revnomix.revseed.integration.integration.eglobe.dto.EglobeRoomMapping;
import com.revnomix.revseed.model.StaahRateTypes;
import com.revnomix.revseed.model.StaahRoomTypes;
import com.revnomix.revseed.repository.StaahRateTypesRepository;
import com.revnomix.revseed.repository.StaahRoomTypesRepository;

@Component
public class EGlobeRoomMappingTransformer implements GenericTransformer<EGlobePopulationDto, List<StaahRateTypes>> {

    @Autowired
    private StaahRoomTypesRepository eglobeRoomTypesRepository;

    @Autowired
    private StaahRateTypesRepository eglobeRateTypesRepository;
    
    @Override
    public List<StaahRateTypes> transform(EGlobePopulationDto eglobePopulationDto) {
        List<StaahRateTypes> eglobeRateTypesList = new ArrayList<>();

        Date today = new Date();
        List<EglobeRoomMapping> eglobeList = eglobePopulationDto.getEglobeRoomMappingList();
        eglobeList.forEach(room -> {
        	StaahRoomTypes eglobeRoomTypes = eglobeRoomTypesRepository.findByStaahIdAndClientId(Long.parseLong(room.getRoomCode()), eglobePopulationDto.getClient().getId());
            if (eglobeRoomTypes == null) {
                eglobeRoomTypes = new StaahRoomTypes();
            }
            eglobeRoomTypes.setClientId(eglobePopulationDto.getClient().getId());
            eglobeRoomTypes.setStaahId(Long.parseLong(room.getRoomCode()));
            eglobeRoomTypes.setName(room.getRoomName());
            eglobeRoomTypes.setRegDate(today);
            room.getRatePlans().forEach(rate -> {
                StaahRateTypes eglobeRateTypes = eglobeRateTypesRepository.findByClientIdAndStaahRateIdAndStaahRoomId(
                        eglobePopulationDto.getClient().getId(),Long.parseLong(rate.getRatePlanCode()), Long.parseLong(room.getRoomCode()));
                if (eglobeRateTypes == null) {
                    eglobeRateTypes = new StaahRateTypes();
                }
                
                eglobeRateTypes.setClientId(eglobePopulationDto.getClient().getId());
                eglobeRateTypes.setStaahRateId(Long.parseLong(rate.getRatePlanCode()));
                eglobeRateTypes.setStaahRoomId(Long.parseLong(room.getRoomCode()));
                String eglobeRoomName = "";
                String eglobeRateName = "";
                eglobeRoomName = room.getRoomName();
                eglobeRateName = rate.getRatePlanName();
                eglobeRateTypes.setName(eglobeRoomName + " " + eglobeRateName );
                eglobeRateTypes.setRegDate(today);
                eglobeRateTypesRepository.save(eglobeRateTypes);
                eglobeRateTypesList.add(eglobeRateTypes);
            });
            eglobeRoomTypesRepository.save(eglobeRoomTypes);
        });
        return eglobeRateTypesList;
    }
}
