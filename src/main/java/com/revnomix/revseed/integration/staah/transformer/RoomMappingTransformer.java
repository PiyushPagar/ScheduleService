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

@Component
public class RoomMappingTransformer implements GenericTransformer<StaahPopulationDto, List<StaahRateTypes>> {

    @Autowired
    private StaahRoomTypesRepository staahRoomTypesRepository;

    @Autowired
    private StaahRateTypesRepository staahRateTypesRepository;

    @Override
    public List<StaahRateTypes> transform(StaahPopulationDto staahPopulationDto) {
        List<StaahRateTypes> staahRateTypesList = new ArrayList<>();

        Date today = new Date();
        staahPopulationDto.getRoomresponse().getRooms().getRoom().stream().forEach(r -> {
            r.getRates().getRate().stream().forEach(rate -> {
                StaahRoomTypes staahRoomTypes = staahRoomTypesRepository.findByStaahIdAndClientId(r.getRoomId().longValue(),staahPopulationDto.getClient().getId());
                if (staahRoomTypes == null) {
                    staahRoomTypes = new StaahRoomTypes();
                }
                staahRoomTypes.setClientId(staahPopulationDto.getClient().getId());
                staahRoomTypes.setStaahId(r.getRoomId().longValue());
                staahRoomTypes.setName(r.getRoomName());
                staahRoomTypes.setRegDate(today);
                staahRoomTypes = staahRoomTypesRepository.save(staahRoomTypes);
                StaahRateTypes staahRateTypes = staahRateTypesRepository.findByClientIdAndStaahRateIdAndStaahRoomId(
                        staahPopulationDto.getClient().getId(), (long)rate.getRateId(), (long)r.getRoomId()
                );
                if (staahRateTypes == null) {
                    staahRateTypes = new StaahRateTypes();
                }
                staahRateTypes.setClientId(staahPopulationDto.getClient().getId());
                staahRateTypes.setStaahRateId((long)rate.getRateId());
                staahRateTypes.setStaahRoomId(r.getRoomId().longValue());
                staahRateTypes.setName(rate.getRateName());
                staahRateTypes.setRegDate(today);
                staahRateTypesRepository.save(staahRateTypes);
                staahRateTypesList.add(staahRateTypes);
            });
        });
        return staahRateTypesList;
    }
}
