package com.example.geoagri.dashboard;

import com.google.android.gms.maps.model.LatLng;

import java.util.List;

public class IsroDataRepository {

    public BandData fetchBandDataForArea(List<LatLng> boundaryPoints) {
        // Placeholder for real ISRO integration (e.g., Bhuvan/NRSC pipeline).
        // For now, deterministic mock values based on centroid and polygon size.
        LatLng centroid = centroid(boundaryPoints);
        double areaWeight = Math.min(1.0, Math.max(0.1, approximateAreaWeight(boundaryPoints)));

        double nir = clamp(0.35 + Math.abs(Math.sin(centroid.latitude)) * 0.35 + (0.08 * areaWeight), 0.2, 0.92);
        double red = clamp(0.12 + Math.abs(Math.cos(centroid.longitude)) * 0.28 - (0.04 * areaWeight), 0.05, 0.55);
        double blue = clamp(0.06 + Math.abs(Math.sin(centroid.latitude * centroid.longitude)) * 0.12, 0.03, 0.32);

        return new BandData(nir, red, blue);
    }

    private LatLng centroid(List<LatLng> points) {
        double lat = 0.0;
        double lng = 0.0;
        for (LatLng point : points) {
            lat += point.latitude;
            lng += point.longitude;
        }
        int count = Math.max(points.size(), 1);
        return new LatLng(lat / count, lng / count);
    }

    private double approximateAreaWeight(List<LatLng> points) {
        if (points.size() < 3) {
            return 0.1;
        }

        double sum = 0.0;
        for (int i = 0; i < points.size(); i++) {
            LatLng a = points.get(i);
            LatLng b = points.get((i + 1) % points.size());
            sum += (a.longitude * b.latitude) - (b.longitude * a.latitude);
        }
        double areaDeg = Math.abs(sum) / 2.0;
        return Math.min(areaDeg * 1000.0, 1.0);
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    public static class BandData {
        public final double nir;
        public final double red;
        public final double blue;

        public BandData(double nir, double red, double blue) {
            this.nir = nir;
            this.red = red;
            this.blue = blue;
        }
    }
}
