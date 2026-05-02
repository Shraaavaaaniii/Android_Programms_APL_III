package com.example.geoagri.analysis.core;

import androidx.annotation.NonNull;

import com.example.geoagri.analysis.model.CropDetectionResult;
import com.example.geoagri.analysis.model.FarmAnalysisResult;
import com.example.geoagri.analysis.model.GeoPoint;
import com.example.geoagri.analysis.model.LocationInfo;
import com.example.geoagri.analysis.model.SatelliteLandData;
import com.example.geoagri.analysis.model.SpectralPixel;
import com.example.geoagri.analysis.net.ReverseGeocodingService;
import com.example.geoagri.analysis.net.SatelliteSpectralService;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class FarmAnalysisEngine {

    public interface Callback {
        void onSuccess(@NonNull FarmAnalysisResult result);
        void onError(@NonNull FarmAnalysisException error);
    }

    private final ReverseGeocodingService reverseGeocodingService;
    private final SatelliteSpectralService satelliteSpectralService;
    private final CropTypeDetector cropTypeDetector;
    private final FarmAnalysisRepository repository;
    private final ExecutorService executor;

    public FarmAnalysisEngine(
            @NonNull ReverseGeocodingService reverseGeocodingService,
            @NonNull SatelliteSpectralService satelliteSpectralService,
            @NonNull CropTypeDetector cropTypeDetector,
            @NonNull FarmAnalysisRepository repository
    ) {
        this.reverseGeocodingService = reverseGeocodingService;
        this.satelliteSpectralService = satelliteSpectralService;
        this.cropTypeDetector = cropTypeDetector;
        this.repository = repository;
        this.executor = Executors.newSingleThreadExecutor();
    }

    public void analyzeAsync(@NonNull List<GeoPoint> polygon, @NonNull Callback callback) {
        executor.execute(() -> {
            try {
                FarmAnalysisResult result = analyzeSync(polygon);
                callback.onSuccess(result);
            } catch (FarmAnalysisException e) {
                callback.onError(e);
            } catch (Exception e) {
                callback.onError(new FarmAnalysisException("Unexpected farm analysis failure.", e));
            }
        });
    }

    @NonNull
    public FarmAnalysisResult analyzeSync(@NonNull List<GeoPoint> polygon) throws FarmAnalysisException {
        PolygonMath.validatePolygon(polygon);

        GeoPoint centroid = PolygonMath.centroid(polygon);
        LocationInfo locationInfo = reverseGeocodingService.reverseGeocode(centroid.latitude, centroid.longitude);

        List<SpectralPixel> pixels = satelliteSpectralService.fetchSpectralData(polygon);
        SatelliteLandData summary = SpectralMath.summarize(pixels);

        CropDetectionResult cropDetection = cropTypeDetector.detect(summary, locationInfo, LocalDate.now());

        FarmAnalysisResult result = new FarmAnalysisResult(
                cropDetection.cropName,
                cropDetection.confidence,
                locationInfo.compactName(),
                summary.avgNdvi
        );
        repository.save(result);
        return result;
    }

    public void shutdown() {
        executor.shutdownNow();
    }
}
