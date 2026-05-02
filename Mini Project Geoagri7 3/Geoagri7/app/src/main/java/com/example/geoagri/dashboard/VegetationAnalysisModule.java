package com.example.geoagri.dashboard;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Production vegetation analysis module for farm polygons.
 *
 * Flow:
 * 1) Extract pixels inside polygon.
 * 2) Detect vegetation using NDVI > 0.2.
 * 3) If vegetation ratio < 10%, return "No Vegetation Detected" and stop.
 * 4) Otherwise compute means and classify crop health from mean NDVI.
 */
public final class VegetationAnalysisModule {

    private static final double EPSILON = 1e-9;
    private static final double VEGETATION_NDVI_THRESHOLD = 0.2;
    private static final double MIN_VEGETATION_PERCENTAGE = 0.10;

    private VegetationAnalysisModule() {
    }

    @NonNull
    public static List<SatellitePixel> extractPixelsInsidePolygon(
            @Nullable List<SatellitePixel> allPixels,
            @Nullable List<GeoPoint> polygon
    ) {
        if (allPixels == null || allPixels.isEmpty() || polygon == null || polygon.size() < 3) {
            return Collections.emptyList();
        }

        List<SatellitePixel> inside = new ArrayList<>(allPixels.size());
        for (SatellitePixel pixel : allPixels) {
            if (pixel == null || !pixel.isValid()) {
                continue;
            }
            if (isPointInsidePolygon(pixel.latitude, pixel.longitude, polygon)) {
                inside.add(pixel);
            }
        }
        return inside;
    }

    @NonNull
    public static AnalysisResult analyzeFarmPolygonPixels(@Nullable List<SatellitePixel> polygonPixels) {
        if (polygonPixels == null || polygonPixels.isEmpty()) {
            return AnalysisResult.noVegetation(0.0);
        }

        int totalPixels = 0;
        int vegetationPixels = 0;

        double sumNdvi = 0.0;
        double sumEvi = 0.0;
        double sumMsavi = 0.0;

        // Single pass: detect vegetation and aggregate metrics only for vegetation pixels.
        for (SatellitePixel pixel : polygonPixels) {
            if (pixel == null || !pixel.isValid()) {
                continue;
            }
            totalPixels++;

            double ndvi = calculateNDVI(pixel.nir, pixel.red);
            if (!isVegetationPixel(ndvi)) {
                continue;
            }

            vegetationPixels++;
            sumNdvi += ndvi;
            sumEvi += calculateEVI(pixel.nir, pixel.red, pixel.blue);
            sumMsavi += calculateMSAVI(pixel.nir, pixel.red);
        }

        if (totalPixels == 0) {
            return AnalysisResult.noVegetation(0.0);
        }

        double vegetationPercentage = (double) vegetationPixels / (double) totalPixels;
        if (vegetationPercentage < MIN_VEGETATION_PERCENTAGE) {
            return AnalysisResult.noVegetation(vegetationPercentage);
        }

        // Vegetation threshold passed; safe to compute crop-health metrics.
        double meanNdvi = safeAverage(sumNdvi, vegetationPixels);
        double meanEvi = safeAverage(sumEvi, vegetationPixels);
        double meanMsavi = safeAverage(sumMsavi, vegetationPixels);
        CropHealth cropHealth = classifyCropHealth(meanNdvi);

        return AnalysisResult.vegetationDetected(
                vegetationPercentage,
                meanNdvi,
                meanEvi,
                meanMsavi,
                cropHealth
        );
    }

    public static double calculateNDVI(double nir, double red) {
        double denominator = nir + red;
        if (Math.abs(denominator) < EPSILON) {
            return 0.0;
        }
        return (nir - red) / denominator;
    }

    public static double calculateEVI(double nir, double red, double blue) {
        double denominator = nir + (6.0 * red) - (7.5 * blue) + 1.0;
        if (Math.abs(denominator) < EPSILON) {
            return 0.0;
        }
        return 2.5 * ((nir - red) / denominator);
    }

    public static double calculateNRDVI(double nir, double red) {
        double denominator = nir + red;
        if (denominator <= EPSILON) {
            return 0.0;
        }
        return (nir - red) / Math.sqrt(denominator);
    }

    public static double calculateMSAVI(double nir, double red) {
        double term = (2.0 * nir) + 1.0;
        double radicand = (term * term) - (8.0 * (nir - red));
        if (radicand < 0.0) {
            radicand = 0.0;
        }
        return (term - Math.sqrt(radicand)) / 2.0;
    }

    public static boolean isVegetationPixel(double ndvi) {
        return ndvi > VEGETATION_NDVI_THRESHOLD;
    }

    private static CropHealth classifyCropHealth(double meanNdvi) {
        if (meanNdvi >= 0.6) {
            return CropHealth.High;
        }
        if (meanNdvi >= 0.3) {
            return CropHealth.Moderate;
        }
        return CropHealth.Poor;
    }

    private static double safeAverage(double sum, int count) {
        return count <= 0 ? 0.0 : (sum / (double) count);
    }

    /**
     * Ray-casting point in polygon test.
     * Uses longitude as x and latitude as y.
     */
    private static boolean isPointInsidePolygon(
            double latitude,
            double longitude,
            @NonNull List<GeoPoint> polygon
    ) {
        boolean inside = false;
        int n = polygon.size();
        for (int i = 0, j = n - 1; i < n; j = i++) {
            GeoPoint pi = polygon.get(i);
            GeoPoint pj = polygon.get(j);
            boolean intersects = ((pi.latitude > latitude) != (pj.latitude > latitude))
                    && (longitude < ((pj.longitude - pi.longitude) * (latitude - pi.latitude)
                    / ((pj.latitude - pi.latitude) + EPSILON) + pi.longitude));
            if (intersects) {
                inside = !inside;
            }
        }
        return inside;
    }

    public enum CropHealth {
        High,
        Moderate,
        Poor
    }

    public static final class GeoPoint {
        public final double latitude;
        public final double longitude;

        public GeoPoint(double latitude, double longitude) {
            this.latitude = latitude;
            this.longitude = longitude;
        }
    }

    public static final class SatellitePixel {
        public final double latitude;
        public final double longitude;
        public final double nir;
        public final double red;
        public final double blue;

        public SatellitePixel(double latitude, double longitude, double nir, double red, double blue) {
            this.latitude = latitude;
            this.longitude = longitude;
            this.nir = nir;
            this.red = red;
            this.blue = blue;
        }

        public boolean isValid() {
            return !Double.isNaN(latitude)
                    && !Double.isNaN(longitude)
                    && !Double.isNaN(nir)
                    && !Double.isNaN(red)
                    && !Double.isNaN(blue)
                    && !Double.isInfinite(nir)
                    && !Double.isInfinite(red)
                    && !Double.isInfinite(blue);
        }
    }

    public static final class AnalysisResult {
        public final double vegetation_percentage;
        @Nullable
        public final Double mean_ndvi;
        @Nullable
        public final Double mean_evi;
        @Nullable
        public final Double mean_msavi;
        @Nullable
        public final String crop_health;
        public final String status;
        public final String message;

        private AnalysisResult(
                double vegetationPercentage,
                @Nullable Double meanNdvi,
                @Nullable Double meanEvi,
                @Nullable Double meanMsavi,
                @Nullable String cropHealth,
                @NonNull String status,
                @NonNull String message
        ) {
            this.vegetation_percentage = vegetationPercentage;
            this.mean_ndvi = meanNdvi;
            this.mean_evi = meanEvi;
            this.mean_msavi = meanMsavi;
            this.crop_health = cropHealth;
            this.status = status;
            this.message = message;
        }

        @NonNull
        private static AnalysisResult noVegetation(double vegetationPercentage) {
            return new AnalysisResult(
                    vegetationPercentage,
                    null,
                    null,
                    null,
                    null,
                    "No Vegetation Detected",
                    "Selected area does not contain crops."
            );
        }

        @NonNull
        private static AnalysisResult vegetationDetected(
                double vegetationPercentage,
                double meanNdvi,
                double meanEvi,
                double meanMsavi,
                @NonNull CropHealth cropHealth
        ) {
            return new AnalysisResult(
                    vegetationPercentage,
                    meanNdvi,
                    meanEvi,
                    meanMsavi,
                    cropHealth.name(),
                    "Vegetation Detected",
                    String.format(
                            Locale.US,
                            "Vegetation detected in %.2f%% of selected area.",
                            vegetationPercentage * 100.0
                    )
            );
        }
    }
}
