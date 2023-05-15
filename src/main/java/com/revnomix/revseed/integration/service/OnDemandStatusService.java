package com.revnomix.revseed.integration.service;

import com.revnomix.revseed.model.OnDemandStatus;
import com.revnomix.revseed.repository.OnDemandStatusRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class OnDemandStatusService {

    @Autowired
    private OnDemandStatusRepository onDemandStatusRepository;

    public void changeStatus(Integer clientId, String serviceType){
        onDemandStatusRepository.findByClientIdAndServiceType(clientId, serviceType).ifPresentOrElse(client->{
            client.setAllowRun(1);
            onDemandStatusRepository.save(client);
        },()->{
            OnDemandStatus client = new OnDemandStatus();
            client.setClientId(clientId);
            client.setServiceType(serviceType);
            client.setAllowRun(1);
            onDemandStatusRepository.save(client);
        });
    }
}
