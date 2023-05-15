package com.revnomix.revseed.integration.file;


public interface PositionAware {

    void setPosition(long position);
    long getPosition();

}