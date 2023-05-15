package com.revnomix.revseed.integration.ezee.transformer;

import java.util.ArrayList;

import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.transformer.GenericTransformer;
import org.springframework.stereotype.Component;

import com.revnomix.revseed.model.Clients;
import com.revnomix.revseed.model.StaahRateTypes;
import com.revnomix.revseed.model.StaahRates;
import com.revnomix.revseed.model.StaahRoomTypes;
import com.revnomix.revseed.repository.StaahRateTypesRepository;
import com.revnomix.revseed.repository.StaahRatesRepository;
import com.revnomix.revseed.repository.StaahRoomTypesRepository;
import com.revnomix.revseed.schema.ezee.RESRequest;
import com.revnomix.revseed.Service.AlertService;
import com.revnomix.revseed.Util.DateUtil;
import com.revnomix.revseed.integration.ezee.EzeePopulationDto;
import com.revnomix.revseed.integration.service.EzeeInboundService;
import com.revnomix.revseed.integration.service.EzeeRequestType;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class EzeeRateUpdateTransformer implements GenericTransformer<EzeePopulationDto, List<StaahRates>> {

    @Autowired
    private StaahRatesRepository staahRatesRepository;

    @Autowired
    private StaahRateTypesRepository staahRateTypesRepository;

    @Autowired
    private StaahRoomTypesRepository staahRoomTypesRepository;

    @Autowired
    private EzeeInboundService ezeeInboundService;
    
	@Autowired
	private AlertService alertService;

    @Override
    public List<StaahRates> transform(EzeePopulationDto ezeePopulationDto) {
        final List<StaahRates> staahRatesList = new ArrayList<>();
        Date today = new Date();

        Long roomId = ezeePopulationDto.getResRequest().getRateType().getRoomTypeID();
        Long rateId = ezeePopulationDto.getResRequest().getRateType().getRateTypeID();
        long noOfDays = DateUtil.daysBetween(ezeePopulationDto.getResRequest().getRateType().getFromDate().toGregorianCalendar().getTime(),
                ezeePopulationDto.getResRequest().getRateType().getToDate().toGregorianCalendar().getTime());
        for (int i = 0; i <= noOfDays;i++) {
            StaahRoomTypes staahRoomTypes = staahRoomTypesRepository.findByStaahIdAndClientId(roomId, ezeePopulationDto.getClients().getId());
            if (staahRoomTypes == null) {
            	try {
            		ezeeInboundService.getRoomInfo(getRoomRequest(ezeePopulationDto.getClients()));
            	}catch(Exception ce) {
            		ce.printStackTrace();
            	}
            }
            staahRoomTypes = staahRoomTypesRepository.findByStaahIdAndClientId(roomId, ezeePopulationDto.getClients().getId());
            if (staahRoomTypes == null) {
                staahRoomTypes = new StaahRoomTypes();
                staahRoomTypes.setRegDate(today);
                staahRoomTypes.setName("UNMATCHED");
                staahRoomTypes.setStaahId(roomId);
                staahRoomTypes.setClientId(ezeePopulationDto.getClients().getId());
                staahRoomTypesRepository.save(staahRoomTypes);
            }
            StaahRateTypes staahRateTypes = staahRateTypesRepository.findByClientIdAndStaahRoomIdAndRateId(ezeePopulationDto.getClients().getId(), roomId, rateId);
            if (staahRateTypes == null) {
            	try {
            		ezeeInboundService.getRoomInfo(getRoomRequest(ezeePopulationDto.getClients()));
	            }catch(Exception ce) {
	        		ce.printStackTrace();
	        	}
            }
            staahRateTypes = staahRateTypesRepository.findByClientIdAndStaahRoomIdAndRateId(ezeePopulationDto.getClients().getId(), roomId, rateId);
            if (staahRateTypes == null) {
                staahRateTypes = new StaahRateTypes();
                staahRateTypes.setClientId(ezeePopulationDto.getClients().getId());
                staahRateTypes.setName("UNMATCHED");
                staahRateTypes.setRateId(rateId);
                staahRateTypes.setStaahRoomId(roomId);
                staahRateTypes.setRegDate(new Date());
                staahRateTypesRepository.save(staahRateTypes);
            }
            StaahRates staahRates = staahRatesRepository.findByClientIdAndRateDateAndRoomTypeIdAndRateTypeId(ezeePopulationDto.getClients().getId(),
                    DateUtil.addDays(ezeePopulationDto.getResRequest().getRateType().getFromDate().toGregorianCalendar().getTime(), i), staahRoomTypes.getId(), staahRateTypes.getId());
            if (staahRates == null) {
                staahRates = new StaahRates();
            }
            staahRates.setClientId(ezeePopulationDto.getClients().getId());
            staahRates.setRegDate(today);
            staahRates.setRoomTypeId(staahRoomTypes.getId());
            staahRates.setRateTypeId(staahRateTypes.getId());
            Double rate = (double)ezeePopulationDto.getResRequest().getRateType().getRoomRate().getBase();
            staahRates.setRateDate(DateUtil.addDays(ezeePopulationDto.getResRequest().getRateType().getFromDate().toGregorianCalendar().getTime(), i));
            staahRatesRepository.save(staahRates);
            staahRates.setRate(rate);
            staahRatesRepository.save(staahRates);
            staahRatesList.add(staahRates);
        }
        return staahRatesList;
    }

    private RESRequest getRoomRequest(Clients clients) {
        RESRequest request = new RESRequest();
        RESRequest.Authentication roomRequest = new RESRequest.Authentication();
        roomRequest.setHotelCode(clients.getCmHotel());
        roomRequest.setAuthCode(clients.getCmPassword());
        request.setRequestType(EzeeRequestType.ROOM_INFO);
        request.setAuthentication(roomRequest);
        return request;
    }
}
