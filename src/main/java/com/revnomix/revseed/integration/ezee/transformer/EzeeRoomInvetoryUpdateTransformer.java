package com.revnomix.revseed.integration.ezee.transformer;

import java.util.ArrayList;

import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.transformer.GenericTransformer;
import org.springframework.stereotype.Component;

import com.revnomix.revseed.model.Clients;
import com.revnomix.revseed.model.StaahInventory;
import com.revnomix.revseed.model.StaahRoomTypes;
import com.revnomix.revseed.repository.StaahInventoryRepository;
import com.revnomix.revseed.repository.StaahRoomTypesRepository;
import com.revnomix.revseed.schema.ezee.RESRequest;
import com.revnomix.revseed.Service.AlertService;
import com.revnomix.revseed.Util.DateUtil;
import com.revnomix.revseed.integration.ezee.EzeePopulationDto;
import com.revnomix.revseed.integration.service.EzeeInboundService;
import com.revnomix.revseed.integration.service.EzeeRequestType;

@Component
public class EzeeRoomInvetoryUpdateTransformer implements GenericTransformer<EzeePopulationDto, List<StaahInventory>> {

    @Autowired
    private StaahRoomTypesRepository staahRoomTypesRepository;

    @Autowired
    private StaahInventoryRepository staahInventoryRepository;

    @Autowired
    private EzeeInboundService ezeeInboundService;
    
	@Autowired
	private AlertService alertService;

    @Override
    public List<StaahInventory> transform(EzeePopulationDto ezeePopulationDto) {
        List<StaahInventory> staahInventories = new ArrayList<>();
        Date today = new Date();
        long noOfDays = DateUtil.daysBetween(ezeePopulationDto.getResRequest().getRoomType().getFromDate().toGregorianCalendar().getTime(),
                ezeePopulationDto.getResRequest().getRoomType().getToDate().toGregorianCalendar().getTime());
        Long id = ezeePopulationDto.getResRequest().getRoomType().getRoomTypeID();
        StaahRoomTypes staahRoomTypes = staahRoomTypesRepository.findByStaahIdAndClientId(id, ezeePopulationDto.getClients().getId());
        if (staahRoomTypes == null) {
        	try {
        		ezeeInboundService.getRoomInfo(getRoomRequest(ezeePopulationDto.getClients()));
        	}catch(Exception ce) {
        		ce.printStackTrace();
        	}
        }
        staahRoomTypes = staahRoomTypesRepository.findByStaahIdAndClientId(id, ezeePopulationDto.getClients().getId());
        if (staahRoomTypes == null) {
            staahRoomTypes = new StaahRoomTypes();
            staahRoomTypes.setClientId(ezeePopulationDto.getClients().getId());
            staahRoomTypes.setStaahId(id);
            staahRoomTypes.setName("UNMATCHED");
            staahRoomTypes.setRegDate(new Date());
            staahRoomTypes = staahRoomTypesRepository.save(staahRoomTypes);
        }
        for (int i = 0; i <= noOfDays; i++) {
            StaahInventory staahInventory = staahInventoryRepository.findByClientIdAndInvDateAndRoomTypeId(ezeePopulationDto.getClients().getId(), DateUtil.addDays(ezeePopulationDto.getResRequest().getRoomType().getFromDate().toGregorianCalendar().getTime(), i), staahRoomTypes.getId());
            if (staahInventory == null) {
                staahInventory = new StaahInventory();
            }
            staahInventory.setClientId(ezeePopulationDto.getClients().getId());
            staahInventory.setRoomTypeId(staahRoomTypes.getId());
            staahInventory.setAvailability(ezeePopulationDto.getResRequest().getRoomType().getAvailability());
            staahInventory.setInvDate(DateUtil.addDays(ezeePopulationDto.getResRequest().getRoomType().getFromDate().toGregorianCalendar().getTime(), i));
            staahInventory.setRegDate(today);
            staahInventoryRepository.save(staahInventory);
            staahInventories.add(staahInventory);
        }
        return staahInventories;
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
