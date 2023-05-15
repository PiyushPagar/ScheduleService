package com.revnomix.revseed.integration.staah;

public class DownloadRequest {

    Integer staahId;

    String propertyName;
    String sessionId;

    public DownloadRequest(Integer staahId,String propertyName) {
        this.staahId = staahId;
        this.propertyName = propertyName;
    }

    public DownloadRequest(Integer staahId, String propertyName, String sessionId) {
        this.staahId = staahId;
        this.propertyName = propertyName;
        this.sessionId = sessionId;
    }

    public Integer getStaahId() {
        return staahId;
    }

    public String getPropertyName() {
        return propertyName;
    }

    public String getSessionId() {
        return sessionId;
    }
}
