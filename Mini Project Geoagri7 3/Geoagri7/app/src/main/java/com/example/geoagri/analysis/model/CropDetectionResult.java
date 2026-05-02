package com.example.geoagri.analysis.model;

import androidx.annotation.NonNull;

public final class CropDetectionResult {
    @NonNull public final String cropName;
    public final double confidence;

    public CropDetectionResult(@NonNull String cropName, double confidence) {
        this.cropName = cropName;
        this.confidence = confidence;
    }
}
