package com.example.geoagri.analysis.model;

import androidx.annotation.NonNull;

public final class GeoPoint {
    public final double latitude;
    public final double longitude;

    public GeoPoint(double latitude, double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }

    @NonNull
    @Override
    public String toString() {
        return latitude + "," + longitude;
    }
}
