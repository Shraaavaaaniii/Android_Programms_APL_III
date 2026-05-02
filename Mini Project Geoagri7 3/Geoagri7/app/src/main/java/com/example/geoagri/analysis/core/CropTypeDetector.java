package com.example.geoagri.analysis.core;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.geoagri.analysis.model.CropDetectionResult;
import com.example.geoagri.analysis.model.LocationInfo;
import com.example.geoagri.analysis.model.SatelliteLandData;
import com.example.geoagri.analysis.model.SpectralPixel;

import java.time.LocalDate;
import java.util.Locale;

public final class CropTypeDetector {

    @NonNull
    public CropDetectionResult detect(
            @NonNull SatelliteLandData landData,
            @Nullable LocationInfo location,
            @NonNull LocalDate observationDate
    ) {
        double tomatoScore = scoreTomato(landData, location, observationDate);

        if (tomatoScore >= 0.55) {
            return new CropDetectionResult("Tomato", clamp01(tomatoScore));
        }

        // Generic crop fallback based on vegetation vigor.
        String genericCrop = landData.avgNdvi >= 0.55 ? "Dense Vegetation Crop" : "Mixed Field Crop";
        return new CropDetectionResult(genericCrop, clamp01(0.45 + (landData.avgNdvi * 0.3)));
    }

    private double scoreTomato(
            @NonNull SatelliteLandData landData,
            @Nullable LocationInfo location,
            @NonNull LocalDate observationDate
    ) {
        double score = 0.0;

        // Rule: Maharashtra + Oct-Feb strongly suggests tomato season window.
        if (isMaharashtra(location)) {
            score += 0.25;
            if (isOctToFeb(observationDate)) {
                score += 0.25;
            }
        }

        // Tomato-like spectral behavior (heuristic): moderate-high NDVI, good NIR, controlled SWIR.
        if (landData.avgNdvi >= 0.35 && landData.avgNdvi <= 0.75) {
            score += 0.20;
        }
        if (landData.avgNir > landData.avgRed && landData.avgNir > landData.avgSwir) {
            score += 0.15;
        }
        if (landData.avgSwir < 0.45) {
            score += 0.10;
        }

        // NDVI pattern consistency: lower variance often indicates homogeneous crop canopy.
        double ndviStd = ndviStdDev(landData);
        if (ndviStd <= 0.18) {
            score += 0.10;
        }

        return score;
    }

    private boolean isMaharashtra(@Nullable LocationInfo location) {
        if (location == null || location.state == null) {
            return false;
        }
        return "maharashtra".equals(location.state.trim().toLowerCase(Locale.US));
    }

    private boolean isOctToFeb(@NonNull LocalDate date) {
        int month = date.getMonthValue();
        return month >= 10 || month <= 2;
    }

    private double ndviStdDev(@NonNull SatelliteLandData landData) {
        if (landData.pixels.isEmpty()) {
            return 1.0;
        }
        double mean = landData.avgNdvi;
        double sumSq = 0.0;
        int n = 0;
        for (SpectralPixel pixel : landData.pixels) {
            if (pixel == null || !pixel.isValid()) {
                continue;
            }
            double ndvi = SpectralMath.ndvi(pixel.nir, pixel.red);
            double diff = ndvi - mean;
            sumSq += diff * diff;
            n++;
        }
        if (n == 0) {
            return 1.0;
        }
        return Math.sqrt(sumSq / n);
    }

    private double clamp01(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }
}
