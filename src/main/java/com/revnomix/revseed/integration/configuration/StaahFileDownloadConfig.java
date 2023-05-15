package com.revnomix.revseed.integration.configuration;
import static com.revnomix.revseed.integration.configuration.PropertiesConfig.COMPLETED_MGS_CHANNEL;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.messaging.MessageChannel;

//import com.revnomix.revseed.controller.staah.StaahDataDownloaderController;
import com.revnomix.revseed.integration.staah.DownloadRequest;

@Configuration
public class StaahFileDownloadConfig {

    public static final String STAAH_FILE_CM_DOWNLOAD_CHANNEL = "staahCmDownloadChannel";
    public static final String STAAH_FILE_OTA_DOWNLOAD_CHANNEL = "staahOtaDownloadChannel";

//    @Autowired
//    StaahDataDownloaderController staahDataDownloaderController;

    @Bean(name = STAAH_FILE_CM_DOWNLOAD_CHANNEL)
    public MessageChannel staahCmDownloadChannel() {
        return new DirectChannel();
    }

    @Bean(name = STAAH_FILE_OTA_DOWNLOAD_CHANNEL)
    public MessageChannel staahOtaDownloadChannel() {
        return new DirectChannel();
    }

//    @Bean
//    public IntegrationFlow staahCmDownloadChannelFlow() {
//        return IntegrationFlows.from(STAAH_FILE_CM_DOWNLOAD_CHANNEL)
//                .<DownloadRequest>handle((p,h)->staahDataDownloaderController.getRatesFromStaah(p.getStaahId(),p.getPropertyName(),p.getSessionId()))
//                .channel(COMPLETED_MGS_CHANNEL)
//                .get();
//    }

//    @Bean
//    public IntegrationFlow staahOtaDownloadChannelFlow() {
//        return IntegrationFlows.from(STAAH_FILE_OTA_DOWNLOAD_CHANNEL)
//                .<DownloadRequest>handle((p,h)->staahDataDownloaderController.getBookingsFromStaah(p.getStaahId(),p.getPropertyName()))
//                .channel(COMPLETED_MGS_CHANNEL)
//                .get();
//    }
}
