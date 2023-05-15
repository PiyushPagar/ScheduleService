package com.revnomix.revseed.integration.configuration;

import static com.revnomix.revseed.integration.configuration.PropertiesConfig.COMPLETED_MGS_CHANNEL;


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

import com.revnomix.revseed.integration.staahMax.transformer.StaahMaxBookingTransformer;
import com.revnomix.revseed.integration.staahMax.transformer.StaahMaxPushBookingTransformer;
import com.revnomix.revseed.integration.staahMax.transformer.StaahMaxRateInventoryTransformer;
import com.revnomix.revseed.integration.staahMax.transformer.StaahMaxReservationTransformer;
import com.revnomix.revseed.integration.staahMax.transformer.StaahMaxRoomMappingTransformer;

@ComponentScan
@Configuration
public class PopulateStaahMaxConfig {

    public static final String STAAH_MAX_RESERVATION_DATA_CHANNEL = "staahMaxReservationDataChannel";
    public static final String STAAH_MAX_BOOKING_DATA_CHANNEL = "staahMaxBookingDataChannel";
    public static final String STAAH_MAX_PUSH_BOOKING_DATA_CHANNEL = "staahMaxPushBookingDataChannel";
    public static final String STAAH_MAX_GET_ROOM_MAPPING_DATA_CHANNEL = "staahMaxGetRoomMappingDataChannel";
    public static final String STAAH_MAX_GET_RATE_INVENTORY_DATA_CHANNEL = "staahMaxGetRateInventoryDataChannel";

    @Autowired
    private StaahMaxRoomMappingTransformer staahMaxRoomMappingTransformer;

    @Autowired
    private StaahMaxRateInventoryTransformer staahMaxRateInventoryTransformer;

    @Autowired
    private StaahMaxReservationTransformer staahMaxReservationTransformer;

    @Autowired
    private StaahMaxBookingTransformer staahMaxBookingTransformer;
    
    @Autowired
    private StaahMaxPushBookingTransformer staahMaxPushBookingTransformer;

    @Bean(name = STAAH_MAX_BOOKING_DATA_CHANNEL)
    public MessageChannel staahMaxBookingDataChannel() {
        return new DirectChannel();
    }
    
    @Bean(name = STAAH_MAX_PUSH_BOOKING_DATA_CHANNEL)
    public MessageChannel staahMaxPushBookingDataChannel() {
        return new DirectChannel();
    }

    @Bean(name = STAAH_MAX_RESERVATION_DATA_CHANNEL)
    public MessageChannel staahMaxReservationDataChannel() {
        return new DirectChannel();
    }

    @Bean(name = STAAH_MAX_GET_ROOM_MAPPING_DATA_CHANNEL)
    public MessageChannel staahMaxGetRoomMappingDataChannel() {
        return new DirectChannel();
    }

    @Bean(name = STAAH_MAX_GET_RATE_INVENTORY_DATA_CHANNEL)
    public MessageChannel staahMaxGetRateInventoryDataChannel() {
        return new DirectChannel();
    }

    public PollerSpec getFtpIntervalSpec() {
        PollerSpec pollerSpec = Pollers.fixedRate(10, TimeUnit.SECONDS);
        pollerSpec.maxMessagesPerPoll(10);
        return pollerSpec;
    }

    @Bean
    public IntegrationFlow populateStaahMaxDataFlow() {
        return IntegrationFlows.from(STAAH_MAX_GET_ROOM_MAPPING_DATA_CHANNEL)
                .transform(staahMaxRoomMappingTransformer)
                .channel(COMPLETED_MGS_CHANNEL)
                .get();
    }

    @Bean
    public IntegrationFlow populateStaahMaxRateDataFlow() {
        return IntegrationFlows.from(STAAH_MAX_GET_RATE_INVENTORY_DATA_CHANNEL)
                .transform(staahMaxRateInventoryTransformer)
                .channel(COMPLETED_MGS_CHANNEL)
                .get();
    }

    /*@Bean
    public IntegrationFlow populateStaahBooking() {
        return IntegrationFlows.from(Files.inboundAdapter(new File(config.getStaahIncomingDirectoryPath()))
                        .autoCreateDirectory(true)
                        .patternFilter("staah_booking_*.csv"),
                p -> p.poller(getFtpIntervalSpec()))
                .<File>handle((p, h) -> fileService.moveFileToDirectory(p, config.getArchiveDirectory()))
                .<File, List<StaahBookingDto>>transform((s) -> new DelimitedFileIterator<>(s, RevseedFileType.STAAH_BOOKING, StaahBookingDto.class).all())
                .transform(staahBookingTransformer)
                .<List<Bookings>>handle((p, h)-> bookingsRepository.saveAll(p))
                .channel(COMPLETED_MGS_CHANNEL)
                .get();
    }*/

    @Bean
    public IntegrationFlow populateStaahMaxReservationDataFlow() {
        return IntegrationFlows.from(STAAH_MAX_RESERVATION_DATA_CHANNEL)
                .transform(staahMaxReservationTransformer)
                .channel(COMPLETED_MGS_CHANNEL)
                .get();
    }

    @Bean
    public IntegrationFlow populateStaahMaxBookingDataFlow() {
        return IntegrationFlows.from(STAAH_MAX_BOOKING_DATA_CHANNEL)
                .transform(staahMaxBookingTransformer)
                .channel(COMPLETED_MGS_CHANNEL)
                .get();
    }
    
    @Bean
    public IntegrationFlow populateStaahMaxPushBookingDataFlow() {
        return IntegrationFlows.from(STAAH_MAX_PUSH_BOOKING_DATA_CHANNEL)
                .transform(staahMaxPushBookingTransformer)
                .channel(COMPLETED_MGS_CHANNEL)
                .get();
    }

}



