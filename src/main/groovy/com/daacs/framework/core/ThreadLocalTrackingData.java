package com.daacs.framework.core;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by chostetter on 12/15/16.
 */
public class ThreadLocalTrackingData {
    private static final ThreadLocal<Map<String, Object>> threadLocalTrackingData = new ThreadLocal<>();

    public static Map<String, Object> getTrackingData() {
        return threadLocalTrackingData.get();
    }

    public static void setTrackingData(Map<String, Object> trackingData) {
        threadLocalTrackingData.set(trackingData);
    }

    public static void addTrackingData(String key, Object value){
        Map<String, Object> trackingData = threadLocalTrackingData.get();
        if(trackingData == null){
            trackingData = new HashMap<>();
        }

        trackingData.put(key, value);
        ThreadLocalTrackingData.setTrackingData(trackingData);
    }
}
