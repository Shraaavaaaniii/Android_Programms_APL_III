package com.example.geoagri.dashboard;

import com.google.firebase.Timestamp;

public class FarmItem {
    private String farmId;
    private String userId;
    private double latitude;
    private double longitude;
    private String farmName;
    private Timestamp timestamp;

    public FarmItem() {
    }

    public String getFarmId() {
        return farmId;
    }

    public String getUserId() {
        return userId;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public String getFarmName() {
        return farmName;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }
}
