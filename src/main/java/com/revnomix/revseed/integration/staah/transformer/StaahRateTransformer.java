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
import com.revnomix.revseed.schema.staah.RateDate;
import com.revnomix.revseed.Util.DateUtil;
import com.revnomix.revseed.integration.integration.IntegrationType;

@Component
public class StaahRateTransformer implements GenericTransformer<StaahPopulationDto, List<StaahRates>> {

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
    public List<StaahRates> transform(StaahPopulationDto staahPopulationDto) {
        final List<StaahRates> staahRatesList = new ArrayList<>();
        Date today = new Date();
        staahPopulationDto.getRateRequestSegmentsResponse().getRooms().stream().forEach(rooms -> {
            rooms.getRoom().stream().forEach(room -> {
                StaahRoomTypes staahRoomTypes = staahRoomTypesRepository.findByStaahIdAndClientId(room.getRoomTypeCode().longValue(),staahPopulationDto.getClient().getId());
                if (staahRoomTypes == null) {
                    staahRoomTypes = new StaahRoomTypes();
                    staahRoomTypes.setRegDate(today);
                    staahRoomTypes.setName(room.getRoomTypeName());
                    staahRoomTypes.setStaahId(room.getRoomTypeCode().longValue());
                    staahRoomTypes.setClientId(staahPopulationDto.getClient().getId());
                    staahRoomTypes = staahRoomTypesRepository.save(staahRoomTypes);
                }
                RoomTypeMappings roomTypeMappings = roomTypeMappingsRepository.findByClientRoomTypeAndClientId(room.getRoomTypeCode().longValue(),staahPopulationDto.getClient().getId());
                if (roomTypeMappings == null) {
                    roomTypeMappings = new RoomTypeMappings();
                    roomTypeMappings.setClientId(staahPopulationDto.getClient().getId());
                    roomTypeMappings.setClientRoomType(staahRoomTypes.getStaahId());
                    roomTypeMappings.setRoomTypeId(1);
                    roomTypeMappings.setType(IntegrationType.STAAH.name());
                    roomTypeMappingsRepository.save(roomTypeMappings);
                }
                StaahRateTypes staahRateTypes = staahRateTypesRepository.findByClientIdAndStaahRateIdAndStaahRoomId(staahPopulationDto.getClient().getId(), room.getRatePlanCode().longValue(), room.getRoomTypeCode().longValue());
                if (staahRateTypes == null) {
                    staahRateTypes = new StaahRateTypes();
                    staahRateTypes.setClientId(staahPopulationDto.getClient().getId());
                    staahRateTypes.setName(room.getRatePlanName());
                    staahRateTypes.setStaahRateId(room.getRatePlanCode().longValue());
                    staahRateTypes.setStaahRoomId(room.getRoomTypeCode().longValue());
                    staahRateTypes.setRegDate(new Date());
                    staahRateTypes = staahRateTypesRepository.save(staahRateTypes);
                }
                	for(RateDate date :room.getDates().getDate()) {
	                    StaahRates staahRates = staahRatesRepository.findOneByClientIdAndRateDateAndRoomTypeIdAndRateTypeId(staahPopulationDto.getClient().getId(), date.getEffectiveDate().toGregorianCalendar().getTime(), staahRoomTypes.getId(), staahRateTypes.getId());
	                    if (staahRates == null) {
	                        staahRates = new StaahRates();
	                    }
	                    staahRates.setClientId(staahPopulationDto.getClient().getId());
	                    staahRates.setRegDate(today);
	                    staahRates.setRoomTypeId(staahRoomTypes.getId());
	                    staahRates.setRateTypeId(staahRateTypes.getId());
	                    staahRates.setRate(date.getRate());
	                    staahRates.setRateDate(date.getEffectiveDate().toGregorianCalendar().getTime());
	                    staahRatesRepository.save(staahRates);
	                    staahRatesList.add(staahRates);
                	}
            });
        });
        Clients client = staahPopulationDto.getClient();
        client.setSystemToday(DateUtil.setTimeToZero(new Date()));
        clientsRepository.save(client);
       /* if (!staahRatesList.isEmpty()) {
            staahRatesRepository.saveAll(staahRatesList);
        }*/
        logger.info("data save successfully : ");
        return staahRatesList;
    }
}
