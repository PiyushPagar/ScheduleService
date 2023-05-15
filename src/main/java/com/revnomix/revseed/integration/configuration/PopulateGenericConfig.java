package com.revnomix.revseed.integration.configuration;

import static com.revnomix.revseed.integration.configuration.PropertiesConfig.COMPLETED_MGS_CHANNEL;

import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.PollerSpec;
import org.springframework.integration.dsl.Pollers;
import org.springframework.messaging.MessageChannel;

import com.revnomix.revseed.integration.staah.transformer.GenericBookingTransformer;
import com.revnomix.revseed.integration.staah.transformer.GenericInventoryMappingTransformer;
import com.revnomix.revseed.integration.staah.transformer.GenericRateMappingTransformer;
import com.revnomix.revseed.integration.staah.transformer.GenericRoomMappingTransformer;

//import com.revnomix.revseed.integration.integration.staah.transformer.GenericBookingTransformer;
//import com.revnomix.revseed.integration.integration.staah.transformer.GenericInventoryMappingTransformer;
//import com.revnomix.revseed.integration.integration.staah.transformer.GenericRateMappingTransformer;
//import com.revnomix.revseed.integration.integration.staah.transformer.GenericRoomMappingTransformer;

@Configuration
public class PopulateGenericConfig {
	
    public static final String GENERIC_BOOKING_DATA_CHANNEL = "genericBookingDataChannel";
    public static final String GENERIC_PUSH_BOOKING_DATA_CHANNEL = "genericPushBookingDataChannel";
    public static final String GENERIC_GET_ROOM_MAPPING_DATA_CHANNEL = "genericGetRoomMappingDataChannel";
    public static final String GENERIC_GET_RATE_MAPPING_DATA_CHANNEL = "genericGetRateMappingDataChannel";
    public static final String GENERIC_GET_RATE_INVENTORY_DATA_CHANNEL = "genericGetRateInventoryDataChannel";
    
    @Autowired
    private GenericBookingTransformer genericBookingTransformer;
    
    @Autowired
    private GenericInventoryMappingTransformer genericInventoryMappingTransformer;
    
    @Autowired
    private GenericRoomMappingTransformer genericRoomMappingTransformer;
    
    @Autowired
    private GenericRateMappingTransformer genericRateMappingTransformer;
    
    @Bean(name = GENERIC_BOOKING_DATA_CHANNEL)
    public MessageChannel genericBookingDataChannel() {
        return new DirectChannel();
    }
    
    @Bean(name = GENERIC_PUSH_BOOKING_DATA_CHANNEL)
    public MessageChannel genericPushBookingDataChannel() {
        return new DirectChannel();
    }

    @Bean(name = GENERIC_GET_RATE_MAPPING_DATA_CHANNEL)
    public MessageChannel genericGetRateMappingDataChannel() {
        return new DirectChannel();
    }

    @Bean(name = GENERIC_GET_ROOM_MAPPING_DATA_CHANNEL)
    public MessageChannel genericGetRoomMappingDataChannel() {
        return new DirectChannel();
    }

    @Bean(name = GENERIC_GET_RATE_INVENTORY_DATA_CHANNEL)
    public MessageChannel genericGetRateInventoryDataChannel() {
        return new DirectChannel();
    }
    
    public PollerSpec getFtpIntervalSpec() {
        PollerSpec pollerSpec = Pollers.fixedRate(10, TimeUnit.SECONDS);
        pollerSpec.maxMessagesPerPoll(10);
        return pollerSpec;
    }

    @Bean
    public IntegrationFlow populateGenericDataFlow() {
        return IntegrationFlows.from(GENERIC_GET_ROOM_MAPPING_DATA_CHANNEL)
                .transform(genericRoomMappingTransformer)
                .channel(COMPLETED_MGS_CHANNEL)
                .get();
    }

    @Bean
    public IntegrationFlow populateGenericRateDataFlow() {
        return IntegrationFlows.from(GENERIC_GET_RATE_MAPPING_DATA_CHANNEL)
                .transform(genericRateMappingTransformer)
                .channel(COMPLETED_MGS_CHANNEL)
                .get();
    }
    
    @Bean
    public IntegrationFlow populateGenericInventoryDataFlow() {
        return IntegrationFlows.from(GENERIC_GET_RATE_INVENTORY_DATA_CHANNEL)
                .transform(genericInventoryMappingTransformer)
                .channel(COMPLETED_MGS_CHANNEL)
                .get();
    }
    
    @Bean
    public IntegrationFlow populateGenericBookingDataFlow() {
        return IntegrationFlows.from(GENERIC_BOOKING_DATA_CHANNEL)
                .transform(genericBookingTransformer)
                .channel(COMPLETED_MGS_CHANNEL)
                .get();
    }
    
    @Bean
    public IntegrationFlow populateGenericPushBookingDataFlow() {
        return IntegrationFlows.from(GENERIC_PUSH_BOOKING_DATA_CHANNEL)
                .transform(genericBookingTransformer)
                .channel(COMPLETED_MGS_CHANNEL)
                .get();
    }
}
