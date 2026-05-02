package com.example.geoagri.dashboard;

public final class SatelliteIndexCalculator {

    private SatelliteIndexCalculator() {
    }

    public static double calculateNDVI(double nir, double red) {
        double denominator = nir + red;
        if (Math.abs(denominator) < 1e-9) {
            return 0.0;
        }
        return (nir - red) / denominator;
    }

    public static double calculateEVI(double nir, double red, double blue) {
        double denominator = nir + 6.0 * red - 7.5 * blue + 1.0;
        if (Math.abs(denominator) < 1e-9) {
            return 0.0;
        }
        return 2.5 * (nir - red) / denominator;
    }

    public static double calculateNRDVI(double nir, double red) {
        double base = nir + red;
        if (base <= 0.0) {
            return 0.0;
        }
        return (nir - red) / Math.sqrt(base);
    }

    public static double calculateMSAVI(double nir, double red) {
        double term = 2.0 * nir + 1.0;
        double inside = term * term - 8.0 * (nir - red);
        if (inside < 0.0) {
            inside = 0.0;
        }
        return (term - Math.sqrt(inside)) / 2.0;
    }
}
