package com.revnomix.revseed.integration.service;

import com.revnomix.revseed.model.ClientDemandProcessingStatus;
import com.revnomix.revseed.model.ProcessingStatus;
import com.revnomix.revseed.repository.ClientOnDemandRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ClientOnDemandService {

    @Autowired
    private ClientOnDemandRepository clientOnDemandRepository;

    public void changeStatus(Integer clientId, ProcessingStatus processingStatus){
        clientOnDemandRepository.findByClientId(clientId).ifPresentOrElse(client->{
            if (processingStatus.equals(ProcessingStatus.IN_PROGRESS)){
                client.setProcessingStatus(ProcessingStatus.IN_PROGRESS);
                client.setMessage("IN_PROGRESS");
            }else if (processingStatus.equals(ProcessingStatus.COMPLETED)){
                client.setProcessingStatus(ProcessingStatus.COMPLETED);
                client.setMessage("COMPLETED");
            }else if (processingStatus.equals(ProcessingStatus.FAILED)){
                client.setProcessingStatus(ProcessingStatus.FAILED);
                client.setMessage("FAILED");
            }
            clientOnDemandRepository.save(client);
        },()->{
            ClientDemandProcessingStatus client = new ClientDemandProcessingStatus();
            client.setClientId(clientId);
            client.setProcessingStatus(ProcessingStatus.PENDING);
            client.setMessage("PENDING");
            clientOnDemandRepository.save(client);
        });
    }
}
