package com.example.geoagri.analysis.core;

import androidx.annotation.NonNull;

import com.example.geoagri.analysis.model.SatelliteLandData;
import com.example.geoagri.analysis.model.SpectralPixel;

import java.util.ArrayList;
import java.util.List;

public final class SpectralMath {

    private SpectralMath() {
    }

    public static double ndvi(double nir, double red) {
        double d = nir + red;
        return Math.abs(d) < 1e-9 ? 0.0 : (nir - red) / d;
    }

    public static double evi(double nir, double red, double blueProxy) {
        double d = nir + 6.0 * red - 7.5 * blueProxy + 1.0;
        return Math.abs(d) < 1e-9 ? 0.0 : 2.5 * (nir - red) / d;
    }

    public static double msavi(double nir, double red) {
        double t = 2.0 * nir + 1.0;
        double inside = (t * t) - 8.0 * (nir - red);
        if (inside < 0.0) {
            inside = 0.0;
        }
        return (t - Math.sqrt(inside)) / 2.0;
    }

    @NonNull
    public static SatelliteLandData summarize(@NonNull List<SpectralPixel> rawPixels) throws FarmAnalysisException {
        if (rawPixels.isEmpty()) {
            throw new FarmAnalysisException("No satellite pixels returned for polygon.");
        }

        List<SpectralPixel> pixels = new ArrayList<>(rawPixels.size());
        double red = 0.0;
        double nir = 0.0;
        double swir = 0.0;
        double ndvi = 0.0;
        double evi = 0.0;
        double msavi = 0.0;

        for (SpectralPixel p : rawPixels) {
            if (p == null || !p.isValid()) {
                continue;
            }
            pixels.add(p);
            red += p.red;
            nir += p.nir;
            swir += p.swir;

            ndvi += ndvi(p.nir, p.red);
            // SWIR is used as conservative blue proxy when blue is not available.
            evi += evi(p.nir, p.red, p.swir);
            msavi += msavi(p.nir, p.red);
        }

        if (pixels.isEmpty()) {
            throw new FarmAnalysisException("Satellite response contains no valid spectral pixels.");
        }

        int n = pixels.size();
        return new SatelliteLandData(
                pixels,
                red / n,
                nir / n,
                swir / n,
                ndvi / n,
                evi / n,
                msavi / n
        );
    }
}
