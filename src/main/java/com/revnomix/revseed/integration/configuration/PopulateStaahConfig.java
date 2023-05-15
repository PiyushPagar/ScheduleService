package com.revnomix.revseed.integration.configuration;


import static com.revnomix.revseed.integration.configuration.PropertiesConfig.COMPLETED_MGS_CHANNEL;


import java.io.File;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.PollerSpec;
import org.springframework.integration.dsl.Pollers;
import org.springframework.integration.file.dsl.Files;
import org.springframework.messaging.MessageChannel;

import com.revnomix.revseed.model.Bookings;
import com.revnomix.revseed.repository.BookingsRepository;
import com.revnomix.revseed.repository.StaahInventoryRepository;
import com.revnomix.revseed.repository.StaahRatesRepository;
import com.revnomix.revseed.repository.StaahRoomTypesRepository;
import com.revnomix.revseed.integration.file.DelimitedFileIterator;
import com.revnomix.revseed.integration.file.type.RevseedFileType;
import com.revnomix.revseed.integration.staah.StaahBookingDto;
import com.revnomix.revseed.integration.staah.transformer.ReservationTransformer;
import com.revnomix.revseed.integration.staah.transformer.RoomMappingTransformer;
import com.revnomix.revseed.integration.staah.transformer.StaahBookingTransformer;
import com.revnomix.revseed.integration.staah.transformer.StaahRateTransformer;
import com.revnomix.revseed.integration.staah.transformer.ViewInventoryTransformer;
import com.revnomix.revseed.integration.utility.FileService;

@Configuration
public class PopulateStaahConfig {

    public static final String STAAH_RESERVATION_DATA_CHANNEL = "staahReservationDataChannel";
    public static final String STAAH_GET_ROOM_MAPPING_DATA_CHANNEL = "staahGetRoomMappingDataChannel";
    public static final String STAAH_GET_RATE_INVENTORY_DATA_CHANNEL = "staahGetRateInventoryDataChannel";
    public static final String STAAH_GET_RATE_DATA_CHANNEL = "staahGetRate";


    @Autowired
    private ViewInventoryTransformer viewInventoryTransformer;

    @Autowired
    private RoomMappingTransformer roomMappingTransformer;

    @Autowired
    private StaahRoomTypesRepository staahRoomTypesRepository;

    @Autowired
    private StaahInventoryRepository staahInventoryRepository;

    @Autowired
    private ReservationTransformer reservationTransformer;

    @Autowired
    private StaahRatesRepository staahRatesRepository;

    @Autowired
    private BookingsRepository bookingsRepository;

    @Autowired
    private PropertiesConfig config;

    @Autowired
    private FileService fileService;

    @Autowired
    private StaahBookingTransformer staahBookingTransformer;

    @Autowired
    private StaahRateTransformer staahRateTransformer;


    @Bean(name = STAAH_RESERVATION_DATA_CHANNEL)
    public MessageChannel staahReservationDataChannel() {
        return new DirectChannel();
    }

    @Bean(name = STAAH_GET_ROOM_MAPPING_DATA_CHANNEL)
    public MessageChannel staahGetRoomMappingDataChannel() {
        return new DirectChannel();
    }

    @Bean(name = STAAH_GET_RATE_INVENTORY_DATA_CHANNEL)
    public MessageChannel staahGetRoomInventoryDataChannel() {
        return new DirectChannel();
    }

    @Bean(name = STAAH_GET_RATE_DATA_CHANNEL)
    public MessageChannel staahGetRateDataChannel() {
        return new DirectChannel();
    }


    public PollerSpec getFtpIntervalSpec() {
        PollerSpec pollerSpec = Pollers.fixedRate(10, TimeUnit.SECONDS);
        pollerSpec.maxMessagesPerPoll(10);
        return pollerSpec;
    }

    @Bean
    public IntegrationFlow populateStaahDataFlow() {
        return IntegrationFlows.from(STAAH_GET_ROOM_MAPPING_DATA_CHANNEL)
                .transform(roomMappingTransformer)
                .channel(COMPLETED_MGS_CHANNEL)
                .get();
    }

    @Bean
    public IntegrationFlow populateStaahRateDataFlow() {
        return IntegrationFlows.from(STAAH_GET_RATE_DATA_CHANNEL)
                .transform(staahRateTransformer)
                .channel(COMPLETED_MGS_CHANNEL)
                .get();
    }

    @Bean
    public IntegrationFlow populateViewInventoryFlow() {
        return IntegrationFlows.from(STAAH_GET_RATE_INVENTORY_DATA_CHANNEL)
                .transform(viewInventoryTransformer)
                .channel(COMPLETED_MGS_CHANNEL)
                .get();
    }

    @Bean
    public IntegrationFlow populateStaahBooking() {
    	try {
//			IntegrationFlow flow = IntegrationFlows.from(Files.inboundAdapter(new File(config.getStaahIncomingDirectoryPath()))
//	                .autoCreateDirectory(true)
//	                .patternFilter("staah_booking_*.csv"),
//	        p -> p.poller(getFtpIntervalSpec()))
//	        .<File>handle((p, h) -> fileService.moveFileToDirectory(p, config.getArchiveDirectory()))
//	        .<File, List<StaahBookingDto>>transform((s) -> new DelimitedFileIterator<>(s, RevseedFileType.STAAH_BOOKING, StaahBookingDto.class).all())
//	        .transform(staahBookingTransformer)
//	        .<List<Bookings>>handle((p, h)-> bookingsRepository.saveAll(p))
//	        .channel(COMPLETED_MGS_CHANNEL)
//	        .get();
//	    	return flow;
    		return null;
    		
    	}catch(Exception ce) {
    		ce.printStackTrace();
    		return null;
    	}
    }

    @Bean
    public IntegrationFlow populateReservationDataFlow() {
        return IntegrationFlows.from(STAAH_RESERVATION_DATA_CHANNEL)
                .transform(reservationTransformer)
                .<List<Bookings>>handle((p, h) -> bookingsRepository.saveAll(p))
                .channel(COMPLETED_MGS_CHANNEL)
                .get();
    }
}




