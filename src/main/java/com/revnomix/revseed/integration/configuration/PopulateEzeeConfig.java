package com.revnomix.revseed.integration.configuration;


import static com.revnomix.revseed.integration.configuration.PropertiesConfig.COMPLETED_MGS_CHANNEL;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.messaging.MessageChannel;

import com.revnomix.revseed.integration.ezee.transformer.EzeeBookingsTransformer;
import com.revnomix.revseed.integration.ezee.transformer.EzeeRateTransformer;
import com.revnomix.revseed.integration.ezee.transformer.EzeeRateUpdateTransformer;
import com.revnomix.revseed.integration.ezee.transformer.EzeeRoomInfoMappingTransformer;
import com.revnomix.revseed.integration.ezee.transformer.EzeeRoomInventoryMappingTransformer;
import com.revnomix.revseed.integration.ezee.transformer.EzeeRoomInvetoryUpdateTransformer;
import com.revnomix.revseed.integration.ezee.transformer.EzeeRoomMappingTransformer;


@Configuration
public class PopulateEzeeConfig {

    public static final String EZEE_GET_ROOM_MAPPING_DATA_CHANNEL = "ezeeGetRoomMappingDataChannel";
    public static final String EZEE_GET_ROOM_INFO_CHANNEL = "ezeeGetRoomMappingDataChannel";
    public static final String EZEE_GET_INVENTORY_CHANNEL = "ezeeUpdateRoomInventoryDataChannel";
    public static final String EZEE_GET_BOOKINGS_CHANNEL = "ezeeGetBookingsDataChannel";
    public static final String EZEE_GET_RATE_CHANNEL = "ezeeGetRateDataChannel";
    public static final String EZEE_GET_RATE_UPDATE_CHANNEL = "ezeeGetRateUpdateDataChannel";
    public static final String EZEE_GET_INVENTORY_UPDATE_CHANNEL = "ezeeGetInventoryUpdateDataChannel";

    @Autowired
    private PropertiesConfig config;

    @Autowired
    private EzeeRoomMappingTransformer ezeeRoomMappingTransformer;

    @Autowired
    private EzeeBookingsTransformer ezeeBookingsTransformer;

    @Autowired
    private EzeeRateTransformer ezeeRateTransformer;

    @Autowired
    private EzeeRateUpdateTransformer ezeeRateUpdateTransformer;

    @Autowired
    private EzeeRoomInfoMappingTransformer ezeeRoomInfoMappingTransformer;

    @Autowired
    private EzeeRoomInventoryMappingTransformer ezeeRoomInvetoryMappingTransformer;

    @Autowired
    private EzeeRoomInvetoryUpdateTransformer ezeeRoomInvetoryUpdateTransformer;

    @Bean(name = EZEE_GET_ROOM_MAPPING_DATA_CHANNEL)
    public MessageChannel ezeeGetRoomMappingDataChannel() {
        return new DirectChannel();
    }

    @Bean(name = EZEE_GET_ROOM_INFO_CHANNEL)
    public MessageChannel ezeeGetRoomInfoChannel() {
        return new DirectChannel();
    }

    @Bean(name = EZEE_GET_INVENTORY_CHANNEL)
    public MessageChannel ezeeGetInventoryChannel() {
        return new DirectChannel();
    }

    @Bean(name = EZEE_GET_INVENTORY_UPDATE_CHANNEL)
    public MessageChannel ezeeGetInventoryUpdateChannel() {
        return new DirectChannel();
    }

    @Bean(name = EZEE_GET_BOOKINGS_CHANNEL)
    public MessageChannel ezeeGetBookingsChannel() {
        return new DirectChannel();
    }

    @Bean(name = EZEE_GET_RATE_CHANNEL)
    public MessageChannel ezeeGetRateChannel() {
        return new DirectChannel();
    }

    @Bean(name = EZEE_GET_RATE_UPDATE_CHANNEL)
    public MessageChannel ezeeGetRateUpdateChannel() {
        return new DirectChannel();
    }

    @Bean
    public IntegrationFlow populateEzeeRoomMappingDataFlow() {
        return IntegrationFlows.from(EZEE_GET_ROOM_MAPPING_DATA_CHANNEL)
                .transform(ezeeRoomMappingTransformer)
                .channel(COMPLETED_MGS_CHANNEL)
                .get();
    }

    @Bean
    public IntegrationFlow populateEzeeRoomInfoDataFlow() {
        return IntegrationFlows.from(EZEE_GET_ROOM_INFO_CHANNEL)
                .transform(ezeeRoomInfoMappingTransformer)
                .channel(COMPLETED_MGS_CHANNEL)
                .get();
    }

    @Bean
    public IntegrationFlow updateRoomInventoryChannelDataFlow() {
        return IntegrationFlows.from(EZEE_GET_INVENTORY_CHANNEL)
                .transform(ezeeRoomInvetoryMappingTransformer)
                .channel(COMPLETED_MGS_CHANNEL)
                .get();
    }

    @Bean
    public IntegrationFlow updateRoomInventoryUpdateChannelDataFlow() {
        return IntegrationFlows.from(EZEE_GET_INVENTORY_UPDATE_CHANNEL)
                .transform(ezeeRoomInvetoryUpdateTransformer)
                .channel(COMPLETED_MGS_CHANNEL)
                .get();
    }

    @Bean
    public IntegrationFlow populateEzeeBookingsDataFlow() {
        return IntegrationFlows.from(EZEE_GET_BOOKINGS_CHANNEL)
                .transform(ezeeBookingsTransformer)
                .channel(COMPLETED_MGS_CHANNEL)
                .get();
    }

    @Bean
    public IntegrationFlow populateEzeeRateDataFlow() {
        return IntegrationFlows.from(EZEE_GET_RATE_CHANNEL)
                .transform(ezeeRateTransformer)
                .channel(COMPLETED_MGS_CHANNEL)
                .get();
    }

    @Bean
    public IntegrationFlow populateEzeeRateUpdateDataFlow() {
        return IntegrationFlows.from(EZEE_GET_RATE_UPDATE_CHANNEL)
                .transform(ezeeRateUpdateTransformer)
                .channel(COMPLETED_MGS_CHANNEL)
                .get();
    }
}



