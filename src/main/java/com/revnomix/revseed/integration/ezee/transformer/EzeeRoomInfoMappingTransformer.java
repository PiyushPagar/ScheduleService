package com.revnomix.revseed.integration.ezee.transformer;

import java.util.ArrayList;

import java.util.Date;
import java.util.List;

import javax.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.transformer.GenericTransformer;
import org.springframework.stereotype.Component;

import com.revnomix.revseed.model.StaahRateTypes;
import com.revnomix.revseed.model.StaahRoomTypes;
import com.revnomix.revseed.repository.StaahRateTypesRepository;
import com.revnomix.revseed.repository.StaahRoomTypesRepository;
import com.revnomix.revseed.integration.ezee.EzeePopulationDto;

@Component
public class EzeeRoomInfoMappingTransformer implements GenericTransformer<EzeePopulationDto, List<StaahRateTypes>> {

    @Autowired
    private StaahRoomTypesRepository staahRoomTypesRepository;

    @Autowired
    private StaahRateTypesRepository staahRateTypesRepository;

    @Transactional
    @Override
    public List<StaahRateTypes> transform(EzeePopulationDto ezeePopulationDto) {
        List<StaahRateTypes> staahRateTypesList = new ArrayList<>();

        Date today = new Date();
            ezeePopulationDto.getResResponse().getRoomInfo().getRatePlans().getRatePlan().forEach(rate -> {
                Long roomId = rate.getRoomTypeID();
                Long ratePlanId = rate.getRatePlanID();
                Long rateId = rate.getRateTypeID();
                StaahRoomTypes staahRoomTypes = staahRoomTypesRepository.findByStaahIdAndClientId(roomId,ezeePopulationDto.getClients().getId());
                if (staahRoomTypes == null) {
                    staahRoomTypes = new StaahRoomTypes();
                }
                staahRoomTypes.setClientId(ezeePopulationDto.getClients().getId());
                staahRoomTypes.setStaahId(roomId);
                staahRoomTypes.setName(rate.getRoomType());
                staahRoomTypes.setRegDate(today);
                staahRoomTypes = staahRoomTypesRepository.save(staahRoomTypes);
                StaahRateTypes staahRateTypes = staahRateTypesRepository.findByClientIdAndStaahRateIdAndStaahRoomId(
                        ezeePopulationDto.getClients().getId(), ratePlanId, roomId
                );
                if (staahRateTypes == null) {
                    staahRateTypes = new StaahRateTypes();
                }
                staahRateTypes.setClientId(ezeePopulationDto.getClients().getId());
                staahRateTypes.setStaahRateId(ratePlanId);
                staahRateTypes.setStaahRoomId(roomId);
                staahRateTypes.setRateId(rateId);
                staahRateTypes.setName(rate.getRateType()+" ("+rate.getRoomType()+")");
                staahRateTypes.setRegDate(today);
                staahRateTypesRepository.save(staahRateTypes);
                staahRateTypesList.add(staahRateTypes);
            });
        return staahRateTypesList;

    }
}
