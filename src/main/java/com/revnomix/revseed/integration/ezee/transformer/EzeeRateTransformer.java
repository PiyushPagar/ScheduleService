package com.revnomix.revseed.integration.ezee.transformer;



import java.util.ArrayList;

import java.util.Date;
import java.util.List;

import javax.transaction.Transactional;

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
import com.revnomix.revseed.integration.ezee.EzeePopulationDto;
import com.revnomix.revseed.integration.integration.IntegrationType;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class EzeeRateTransformer implements GenericTransformer<EzeePopulationDto, List<StaahRates>> {

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

    @Transactional
    @Override
    public List<StaahRates> transform(EzeePopulationDto ezeePopulationDto) {

        final List<StaahRates> staahRatesList = new ArrayList<>();
        Date today = new Date();
        if(ezeePopulationDto.getResResponse()!=null && ezeePopulationDto.getResResponse().getRoomInfo()!=null) {
	        ezeePopulationDto.getResResponse().getRoomInfo().getSource().forEach(rooms -> {
	            rooms.getRoomTypes().getRateType().forEach(room -> {
	                long noOfDays = DateUtil.daysBetween(room.getFromDate().toGregorianCalendar().getTime(),room.getToDate().toGregorianCalendar().getTime());
                    Long roomId = room.getRoomTypeID();
                    Long rateId = room.getRateTypeID();
                    StaahRoomTypes staahRoomTypes = staahRoomTypesRepository.findByStaahIdAndClientId(roomId, ezeePopulationDto.getClients().getId());
                    if (staahRoomTypes == null) {
                        staahRoomTypes = new StaahRoomTypes();
                        staahRoomTypes.setRegDate(today);
                        staahRoomTypes.setName(rooms.getName());
                        staahRoomTypes.setStaahId(roomId);
                        staahRoomTypes.setClientId(ezeePopulationDto.getClients().getId());
                        staahRoomTypes = staahRoomTypesRepository.save(staahRoomTypes);
                    }
                    RoomTypeMappings roomTypeMappings = roomTypeMappingsRepository.findByClientRoomTypeAndClientId(roomId, ezeePopulationDto.getClients().getId());
                    if (roomTypeMappings == null) {
                        roomTypeMappings = new RoomTypeMappings();
                        roomTypeMappings.setClientId(ezeePopulationDto.getClients().getId());
                        roomTypeMappings.setClientRoomType(staahRoomTypes.getStaahId());
                        roomTypeMappings.setRoomTypeId(1);
                        roomTypeMappings.setType(IntegrationType.EZEE.name());
                        roomTypeMappingsRepository.save(roomTypeMappings);
                    }
	                for (int i = 0; i <= noOfDays;i++) {
	                    StaahRateTypes staahRateTypes = staahRateTypesRepository.findByClientIdAndStaahRateIdAndStaahRoomId(ezeePopulationDto.getClients().getId(), rateId, roomId);
	                    if (staahRateTypes != null) {
	                        StaahRates staahRates = staahRatesRepository.findByClientIdAndRateDateAndRoomTypeIdAndRateTypeId(ezeePopulationDto.getClients().getId(),DateUtil.addDays(room.getFromDate().toGregorianCalendar().getTime(), i), staahRoomTypes.getId(), staahRateTypes.getId());
	                        if (staahRates == null) {
	                            staahRates = new StaahRates();
	                        }
	                        staahRates.setClientId(ezeePopulationDto.getClients().getId());
	                        staahRates.setRegDate(today);
	                        staahRates.setRoomTypeId(staahRoomTypes.getId());
	                        staahRates.setRateTypeId(staahRateTypes.getId());
	                        staahRates.setRate(room.getRoomRate().getBase().doubleValue());
	                        staahRates.setRateDate(DateUtil.addDays(room.getFromDate().toGregorianCalendar().getTime(), i));
	                        staahRatesRepository.save(staahRates);
	                        staahRatesList.add(staahRates);
	                    }
	                }
	            });
	        });
        }
        Clients client = ezeePopulationDto.getClients();
        client.setSystemToday(new Date());
        clientsRepository.save(client);
        return staahRatesList;
    }
}
