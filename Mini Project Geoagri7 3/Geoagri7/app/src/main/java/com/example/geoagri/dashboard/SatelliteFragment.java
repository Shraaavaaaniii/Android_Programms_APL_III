package com.example.geoagri.dashboard;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.geoagri.R;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public class SatelliteFragment extends Fragment implements OnMapReadyCallback, GoogleMap.OnMapClickListener {
    private static final LatLng MAHARASHTRA_CENTER = new LatLng(19.7515, 75.7139);
    private static final float MAHARASHTRA_DEFAULT_ZOOM = 7f;

    private MaterialCardView analysisCard;
    private TextView selectedLocationText;
    private TextView dataSourceText;
    private TextView healthLabelText;
    private View healthIndicator;
    private TextView ndviText;
    private TextView eviText;
    private TextView nrdviText;
    private TextView msaviText;
    private ProgressBar ndviProgress;
    private ProgressBar eviProgress;
    private ProgressBar nrdviProgress;
    private ProgressBar msaviProgress;
    private ProgressBar loadingBar;

    private GoogleMap googleMap;
    private Polygon farmPolygon;
    private final List<LatLng> boundaryPoints = new ArrayList<>();
    private final List<Marker> boundaryMarkers = new ArrayList<>();

    private boolean mapTilesLoaded;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Random random = new Random();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_satellite, container, false);

        analysisCard = root.findViewById(R.id.analysisCard);
        selectedLocationText = root.findViewById(R.id.selectedLocationText);
        dataSourceText = root.findViewById(R.id.dataSourceText);
        healthLabelText = root.findViewById(R.id.healthLabelText);
        healthIndicator = root.findViewById(R.id.healthIndicator);
        ndviText = root.findViewById(R.id.ndviText);
        eviText = root.findViewById(R.id.eviText);
        nrdviText = root.findViewById(R.id.nrdviText);
        msaviText = root.findViewById(R.id.msaviText);
        ndviProgress = root.findViewById(R.id.ndviProgress);
        eviProgress = root.findViewById(R.id.eviProgress);
        nrdviProgress = root.findViewById(R.id.nrdviProgress);
        msaviProgress = root.findViewById(R.id.msaviProgress);
        loadingBar = root.findViewById(R.id.loadingBar);

        Button analyzeBtn = root.findViewById(R.id.analyzeBtn);
        Button clearAreaBtn = root.findViewById(R.id.clearAreaBtn);

        setupMap();

        analyzeBtn.setOnClickListener(v -> {
            if (!hasValidFarmPolygon()) {
                showToast("Please draw farm boundary first");
                return;
            }
            analyzeFarmArea(new ArrayList<>(boundaryPoints));
        });

        clearAreaBtn.setOnClickListener(v -> clearSelectedArea());

        return root;
    }

    private void setupMap() {
        try {
            SupportMapFragment mapFragment = SupportMapFragment.newInstance();
            getChildFragmentManager()
                    .beginTransaction()
                    .replace(R.id.mapContainer, mapFragment)
                    .commitNowAllowingStateLoss();
            mapFragment.getMapAsync(this);
        } catch (Exception e) {
            showToast("Unable to load Google Map.");
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        googleMap = map;
        mapTilesLoaded = false;

        googleMap.getUiSettings().setZoomControlsEnabled(true);
        googleMap.getUiSettings().setMapToolbarEnabled(false);
        googleMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
        googleMap.setOnMapClickListener(this);
        googleMap.setOnMapLoadedCallback(() -> mapTilesLoaded = true);

        // Maharashtra, India (state-level default focus).
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(MAHARASHTRA_CENTER, MAHARASHTRA_DEFAULT_ZOOM));

        mainHandler.postDelayed(() -> {
            if (!isAdded()) {
                return;
            }
            if (!mapTilesLoaded) {
                // Retry once from Maharashtra center if first tile load stalls.
                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                        MAHARASHTRA_CENTER,
                        MAHARASHTRA_DEFAULT_ZOOM
                ));
                showToast("Map tiles not loading. Check internet, Maps SDK billing, and API key restrictions.");
            }
        }, 8000L);
    }

    @Override
    public void onMapClick(@NonNull LatLng latLng) {
        if (googleMap == null) {
            return;
        }

        boundaryPoints.add(latLng);
        // Keep one marker per boundary point so area can be cleared fully.
        Marker marker = googleMap.addMarker(
                new MarkerOptions()
                        .position(latLng)
                        .title("Point " + boundaryPoints.size())
        );
        if (marker != null) {
            boundaryMarkers.add(marker);
        }

        redrawPolygon();

        selectedLocationText.setText(String.format(
                Locale.getDefault(),
                "Boundary points: %d (tap more or Analyze)",
                boundaryPoints.size()
        ));

        if (boundaryPoints.size() < 3) {
            showToast("Draw at least 3 points to create farm area");
        }

        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(
                latLng,
                Math.max(12f, googleMap.getCameraPosition().zoom)
        ));
    }

    private void redrawPolygon() {
        if (googleMap == null) {
            return;
        }

        if (farmPolygon != null) {
            farmPolygon.remove();
        }

        if (boundaryPoints.size() >= 3) {
            PolygonOptions polygonOptions = new PolygonOptions()
                    .addAll(boundaryPoints)
                    .strokeColor(Color.parseColor("#11C735"))
                    .strokeWidth(5f)
                    // ARGB color must include '#' prefix; missing it crashes on third tap.
                    .fillColor(Color.parseColor("#5511C735"));
            farmPolygon = googleMap.addPolygon(polygonOptions);
        }
    }

    private void clearSelectedArea() {
        if (googleMap != null) {
            // Clears all map overlays (markers/polygon) in one call.
            googleMap.clear();
        }
        boundaryPoints.clear();
        for (Marker marker : boundaryMarkers) {
            marker.remove();
        }
        boundaryMarkers.clear();
        if (farmPolygon != null) {
            farmPolygon.remove();
            farmPolygon = null;
        }
        selectedLocationText.setText("Draw farm boundary: tap 3+ points on map");
        dataSourceText.setText("Data source: ISRO satellite feed (simulated)");
    }

    private boolean hasValidFarmPolygon() {
        return farmPolygon != null && boundaryPoints.size() >= 3;
    }

    private void analyzeFarmArea(List<LatLng> coordinates) {
        setLoading(true);
        dataSourceText.setText("Analyzing selected farm area...");
        mainHandler.postDelayed(() -> {
            if (!isAdded()) {
                return;
            }
            setLoading(false);
            AnalysisResult result = analyzeFarmArea();
            renderResult(result, coordinates.size());
        }, 900L);
    }

    // Mock satellite API response stub for now.
    private AnalysisResult analyzeFarmArea() {
        double ndvi = 0.35 + (random.nextDouble() * 0.45);
        double evi = 0.20 + (random.nextDouble() * 0.40);
        double ndre = 0.25 + (random.nextDouble() * 0.45);
        double msavi = 0.30 + (random.nextDouble() * 0.40);

        String healthStatus;
        String insights;
        if (ndvi >= 0.60) {
            healthStatus = "Healthy";
            insights = "Crop vigor is strong. Maintain irrigation and nutrient schedule.";
        } else if (ndvi >= 0.40) {
            healthStatus = "Moderate";
            insights = "Patchy canopy detected. Review soil moisture and nitrogen levels.";
        } else {
            healthStatus = "Poor";
            insights = "Vegetation stress likely. Inspect pests, water stress, and nutrient deficiency.";
        }

        return new AnalysisResult(ndvi, evi, ndre, msavi, healthStatus, insights);
    }

    private void renderResult(AnalysisResult result, int pointCount) {
        ndviText.setText(String.format(Locale.getDefault(), "NDVI: %.3f", result.ndvi));
        eviText.setText(String.format(Locale.getDefault(), "EVI: %.3f", result.evi));
        nrdviText.setText(String.format(Locale.getDefault(), "NDRE: %.3f", result.ndre));
        msaviText.setText(String.format(Locale.getDefault(), "MSAVI: %.3f", result.msavi));

        ndviProgress.setProgress(toProgress(result.ndvi));
        eviProgress.setProgress(toProgress(result.evi));
        nrdviProgress.setProgress(toProgress(result.ndre));
        msaviProgress.setProgress(toProgress(result.msavi));

        String healthLabel;
        int accentColor;
        int cardColor;

        if ("Healthy".equals(result.healthStatus)) {
            healthLabel = result.healthStatus;
            accentColor = Color.parseColor("#13A83B");
            cardColor = Color.parseColor("#E5F6E9");
        } else if ("Moderate".equals(result.healthStatus)) {
            healthLabel = result.healthStatus;
            accentColor = Color.parseColor("#E0A100");
            cardColor = Color.parseColor("#FFF8E2");
        } else {
            healthLabel = result.healthStatus;
            accentColor = Color.parseColor("#D72F2F");
            cardColor = Color.parseColor("#FDEAEA");
        }

        healthLabelText.setText(String.format(Locale.getDefault(), "Overall Crop Health: %s", healthLabel));
        selectedLocationText.setText(String.format(
                Locale.getDefault(),
                "Boundary points: %d (analysis complete)",
                pointCount
        ));
        dataSourceText.setText(String.format(
                Locale.getDefault(),
                "Insights: %s",
                result.insights
        ));
        healthLabelText.setTextColor(accentColor);
        if (healthIndicator.getBackground() != null) {
            healthIndicator.getBackground().setTint(accentColor);
        } else {
            healthIndicator.setBackgroundColor(accentColor);
        }
        analysisCard.setCardBackgroundColor(cardColor);
        analysisCard.setStrokeColor(accentColor);
    }

    private void setLoading(boolean loading) {
        loadingBar.setVisibility(loading ? View.VISIBLE : View.GONE);
    }

    private int toProgress(double value) {
        double clamped = clamp(value, -1.0, 1.0);
        return (int) Math.round(((clamped + 1.0) / 2.0) * 100.0);
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private void showToast(String message) {
        if (isAdded()) {
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mainHandler.removeCallbacksAndMessages(null);
    }

    private static class AnalysisResult {
        final double ndvi;
        final double evi;
        final double ndre;
        final double msavi;
        final String healthStatus;
        final String insights;

        AnalysisResult(double ndvi, double evi, double ndre, double msavi, String healthStatus, String insights) {
            this.ndvi = ndvi;
            this.evi = evi;
            this.ndre = ndre;
            this.msavi = msavi;
            this.healthStatus = healthStatus;
            this.insights = insights;
        }
    }
}
