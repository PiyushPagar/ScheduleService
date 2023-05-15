package com.revnomix.revseed.integration.configuration;

import static com.revnomix.revseed.integration.configuration.PropertiesConfig.COMPLETED_MGS_CHANNEL;

//import static com.revnomix.revseed.integration.configuration.PropertiesConfig.COMPLETED_MGS_CHANNEL;


import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.PollerSpec;
import org.springframework.integration.dsl.Pollers;
import org.springframework.messaging.MessageChannel;

import com.revnomix.revseed.integration.eglobe.transformer.EGlobeBookingTransformer;
import com.revnomix.revseed.integration.eglobe.transformer.EGlobePushBookingTransformer;
import com.revnomix.revseed.integration.eglobe.transformer.EGlobeRateInventoryTransformer;
import com.revnomix.revseed.integration.eglobe.transformer.EGlobeRoomMappingTransformer;

@ComponentScan
@Configuration
public class PopulateEGlobeConfig {

    public static final String E_GLOBE_BOOKING_DATA_CHANNEL = "eGlobeBookingDataChannel";
    public static final String E_GLOBE_PUSH_BOOKING_DATA_CHANNEL = "eGlobePushBookingDataChannel";
    public static final String E_GLOBE_GET_ROOM_MAPPING_DATA_CHANNEL = "eGlobeGetRoomMappingDataChannel";
    public static final String E_GLOBE_GET_RATE_MAPPING_DATA_CHANNEL = "eGlobeGetRateMappingDataChannel";
    public static final String E_GLOBE_GET_RATE_INVENTORY_DATA_CHANNEL = "eGlobeGetRateInventoryDataChannel";
    
    @Autowired
    private EGlobeRoomMappingTransformer eGlobeRoomMappingTransformer;

    @Autowired
    private EGlobeRateInventoryTransformer eGlobeRateInventoryTransformer;

    @Autowired
    private EGlobeBookingTransformer eGlobeBookingTransformer;
    
    @Autowired
    private EGlobePushBookingTransformer eGlobePushBookingTransformer;
    
    @Bean(name = E_GLOBE_BOOKING_DATA_CHANNEL)
    public MessageChannel eGlobeBookingDataChannel() {
        return new DirectChannel();
    }
    
    @Bean(name = E_GLOBE_PUSH_BOOKING_DATA_CHANNEL)
    public MessageChannel eGlobePushBookingDataChannel() {
        return new DirectChannel();
    }

    @Bean(name = E_GLOBE_GET_RATE_MAPPING_DATA_CHANNEL)
    public MessageChannel eGlobeReservationDataChannel() {
        return new DirectChannel();
    }

    @Bean(name = E_GLOBE_GET_ROOM_MAPPING_DATA_CHANNEL)
    public MessageChannel eGlobeGetRoomMappingDataChannel() {
        return new DirectChannel();
    }

    @Bean(name = E_GLOBE_GET_RATE_INVENTORY_DATA_CHANNEL)
    public MessageChannel eGlobeGetRateInventoryDataChannel() {
        return new DirectChannel();
    }
    
    public PollerSpec getFtpIntervalSpec() {
        PollerSpec pollerSpec = Pollers.fixedRate(10, TimeUnit.SECONDS);
        pollerSpec.maxMessagesPerPoll(10);
        return pollerSpec;
    }

    @Bean
    public IntegrationFlow populateeGlobeDataFlow() {
        return IntegrationFlows.from(E_GLOBE_GET_ROOM_MAPPING_DATA_CHANNEL)
                .transform(eGlobeRoomMappingTransformer)
                .channel(COMPLETED_MGS_CHANNEL)
                .get();
    }

    @Bean
    public IntegrationFlow populateeGlobeRateDataFlow() {
        return IntegrationFlows.from(E_GLOBE_GET_RATE_INVENTORY_DATA_CHANNEL)
                .transform(eGlobeRateInventoryTransformer)
                .channel(COMPLETED_MGS_CHANNEL)
                .get();
    }
    
    @Bean
    public IntegrationFlow populateeGlobeBookingDataFlow() {
        return IntegrationFlows.from(E_GLOBE_BOOKING_DATA_CHANNEL)
                .transform(eGlobeBookingTransformer)
                .channel(COMPLETED_MGS_CHANNEL)
                .get();
    }
    
    @Bean
    public IntegrationFlow populateeGlobePushBookingDataFlow() {
        return IntegrationFlows.from(E_GLOBE_PUSH_BOOKING_DATA_CHANNEL)
                .transform(eGlobePushBookingTransformer)
                .channel(COMPLETED_MGS_CHANNEL)
                .get();
    }
}
