package com.example.geoagri.analysis.model;

public final class SpectralPixel {
    public final double red;
    public final double nir;
    public final double swir;

    public SpectralPixel(double red, double nir, double swir) {
        this.red = red;
        this.nir = nir;
        this.swir = swir;
    }

    public boolean isValid() {
        return !Double.isNaN(red) && !Double.isNaN(nir) && !Double.isNaN(swir)
                && !Double.isInfinite(red) && !Double.isInfinite(nir) && !Double.isInfinite(swir);
    }
}
