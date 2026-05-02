package com.example.geoagri.analysis.model;

import androidx.annotation.NonNull;

public final class FarmAnalysisResult {
    @NonNull public final String cropName;
    public final double confidence;
    @NonNull public final String locationName;
    public final double ndviAvg;

    public FarmAnalysisResult(
            @NonNull String cropName,
            double confidence,
            @NonNull String locationName,
            double ndviAvg
    ) {
        this.cropName = cropName;
        this.confidence = confidence;
        this.locationName = locationName;
        this.ndviAvg = ndviAvg;
    }
}
