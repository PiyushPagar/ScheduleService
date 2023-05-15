package com.revnomix.revseed.integration.staah.gateway;

import static com.revnomix.revseed.integration.configuration.StaahFileDownloadConfig.STAAH_FILE_CM_DOWNLOAD_CHANNEL;
import static com.revnomix.revseed.integration.configuration.StaahFileDownloadConfig.STAAH_FILE_OTA_DOWNLOAD_CHANNEL;

import org.springframework.integration.annotation.Gateway;
import org.springframework.integration.annotation.MessagingGateway;

import com.revnomix.revseed.integration.staah.DownloadRequest;

@MessagingGateway
public interface StaahDownloadGateway {
    @Gateway(requestChannel = STAAH_FILE_CM_DOWNLOAD_CHANNEL)
    void downloadCMData(DownloadRequest downloadRequest);

    @Gateway(requestChannel = STAAH_FILE_OTA_DOWNLOAD_CHANNEL)
    void downloadOtaData(DownloadRequest downloadRequest);
}
