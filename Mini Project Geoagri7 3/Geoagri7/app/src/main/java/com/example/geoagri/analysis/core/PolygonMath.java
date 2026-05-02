package com.example.geoagri.analysis.core;

import androidx.annotation.NonNull;

import com.example.geoagri.analysis.model.GeoPoint;

import java.util.List;

public final class PolygonMath {

    private PolygonMath() {
    }

    public static void validatePolygon(@NonNull List<GeoPoint> polygon) throws FarmAnalysisException {
        if (polygon.size() < 3) {
            throw new FarmAnalysisException("Farm polygon must contain at least 3 points.");
        }
    }

    @NonNull
    public static GeoPoint centroid(@NonNull List<GeoPoint> polygon) throws FarmAnalysisException {
        validatePolygon(polygon);

        double signedArea = 0.0;
        double cx = 0.0;
        double cy = 0.0;

        for (int i = 0; i < polygon.size(); i++) {
            GeoPoint p0 = polygon.get(i);
            GeoPoint p1 = polygon.get((i + 1) % polygon.size());
            double a = (p0.longitude * p1.latitude) - (p1.longitude * p0.latitude);
            signedArea += a;
            cx += (p0.longitude + p1.longitude) * a;
            cy += (p0.latitude + p1.latitude) * a;
        }

        signedArea *= 0.5;
        if (Math.abs(signedArea) < 1e-9) {
            // Degenerate polygon fallback to arithmetic mean.
            double lat = 0.0;
            double lon = 0.0;
            for (GeoPoint p : polygon) {
                lat += p.latitude;
                lon += p.longitude;
            }
            return new GeoPoint(lat / polygon.size(), lon / polygon.size());
        }

        cx /= (6.0 * signedArea);
        cy /= (6.0 * signedArea);
        return new GeoPoint(cy, cx);
    }

    @NonNull
    public static String encodePolygon(@NonNull List<GeoPoint> polygon) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < polygon.size(); i++) {
            if (i > 0) {
                builder.append(';');
            }
            GeoPoint point = polygon.get(i);
            builder.append(point.latitude).append(',').append(point.longitude);
        }
        return builder.toString();
    }
}
