package com.daacs.model.event

/**
 * Created by adistasio on 5/5/17.
 */
class ErrorEvent {

    String userAgent;

    String currentURL;

    String resolution;

    String viewPort;

    String ipAddress;

    String userId;

    String error;

    ErrorEvent() {}

    ErrorEvent(String error) {
        this.error = error
    }

    @Override
    public String toString() {
        return "ErrorEvent{" +
                "userAgent='" + userAgent + '\'' +
                ", currentURL='" + currentURL + '\'' +
                ", resolution='" + resolution + '\'' +
                ", viewPort='" + viewPort + '\'' +
                ", ipAddress='" + ipAddress + '\'' +
                ", userId='" + userId + '\'' +
                ", error=" + error +
                '}';
    }
}
