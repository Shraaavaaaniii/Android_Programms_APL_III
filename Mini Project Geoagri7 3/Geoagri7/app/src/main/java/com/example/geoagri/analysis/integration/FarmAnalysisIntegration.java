package com.example.geoagri.analysis.integration;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.geoagri.analysis.chat.ChatContextFormatter;
import com.example.geoagri.analysis.core.CropTypeDetector;
import com.example.geoagri.analysis.core.FarmAnalysisEngine;
import com.example.geoagri.analysis.core.FarmAnalysisException;
import com.example.geoagri.analysis.core.FarmAnalysisRepository;
import com.example.geoagri.analysis.model.FarmAnalysisResult;
import com.example.geoagri.analysis.model.GeoPoint;
import com.example.geoagri.analysis.net.ReverseGeocodingService;
import com.example.geoagri.analysis.net.SatelliteSpectralService;
import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.List;

import okhttp3.OkHttpClient;

/**
 * Non-UI integration facade for existing app flow.
 *
 * Usage:
 * 1) Build once in app layer with real endpoint URLs/keys.
 * 2) Call analyzeFarmPolygon(...) after map polygon is pinned.
 * 3) Call enrichQuestionForChatbot(...) before Gemini request.
 */
public final class FarmAnalysisIntegration {

    private final FarmAnalysisEngine engine;
    private final FarmAnalysisRepository repository;

    public FarmAnalysisIntegration(
            @NonNull String reverseGeocodeEndpointTemplate,
            @Nullable String reverseGeocodeApiKey,
            @NonNull String satelliteEndpointUrl,
            @Nullable String satelliteApiKey
    ) {
        OkHttpClient client = new OkHttpClient.Builder().build();
        this.repository = new FarmAnalysisRepository();

        ReverseGeocodingService geocodingService =
                new ReverseGeocodingService(client, reverseGeocodeEndpointTemplate, reverseGeocodeApiKey);
        SatelliteSpectralService spectralService =
                new SatelliteSpectralService(client, satelliteEndpointUrl, satelliteApiKey);

        this.engine = new FarmAnalysisEngine(
                geocodingService,
                spectralService,
                new CropTypeDetector(),
                repository
        );
    }

    public void analyzeFarmPolygon(
            @NonNull List<LatLng> polygon,
            @NonNull FarmAnalysisEngine.Callback callback
    ) {
        List<GeoPoint> geoPoints = new ArrayList<>(polygon.size());
        for (LatLng latLng : polygon) {
            if (latLng == null) {
                continue;
            }
            geoPoints.add(new GeoPoint(latLng.latitude, latLng.longitude));
        }
        engine.analyzeAsync(geoPoints, callback);
    }

    @Nullable
    public FarmAnalysisResult latestResult() {
        return repository.getLatest();
    }

    @NonNull
    public String enrichQuestionForChatbot(@NonNull String userQuestion) {
        return ChatContextFormatter.appendToQuestion(userQuestion, repository.getLatest());
    }

    @NonNull
    public String cropAwareSystemInstruction() {
        return ChatContextFormatter.cropAwareSystemInstruction(repository.getLatest());
    }

    public void clearAnalysis() {
        repository.clear();
    }

    public void shutdown() {
        engine.shutdown();
    }
}
