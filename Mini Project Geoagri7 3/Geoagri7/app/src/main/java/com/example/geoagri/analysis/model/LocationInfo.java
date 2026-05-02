package com.example.geoagri.analysis.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public final class LocationInfo {
    @Nullable public final String village;
    @Nullable public final String district;
    @Nullable public final String state;
    @Nullable public final String country;
    public final double latitude;
    public final double longitude;

    public LocationInfo(
            @Nullable String village,
            @Nullable String district,
            @Nullable String state,
            @Nullable String country,
            double latitude,
            double longitude
    ) {
        this.village = village;
        this.district = district;
        this.state = state;
        this.country = country;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    @NonNull
    public String compactName() {
        StringBuilder sb = new StringBuilder();
        appendPart(sb, village);
        appendPart(sb, district);
        appendPart(sb, state);
        appendPart(sb, country);
        return sb.length() == 0 ? latitude + "," + longitude : sb.toString();
    }

    private void appendPart(@NonNull StringBuilder sb, @Nullable String value) {
        if (value == null || value.trim().isEmpty()) {
            return;
        }
        if (sb.length() > 0) {
            sb.append(", ");
        }
        sb.append(value.trim());
    }
}
