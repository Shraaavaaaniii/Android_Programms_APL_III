package com.example.geoagri.analysis.model;

import androidx.annotation.NonNull;

import java.util.Collections;
import java.util.List;

public final class SatelliteLandData {
    public final List<SpectralPixel> pixels;
    public final double avgRed;
    public final double avgNir;
    public final double avgSwir;
    public final double avgNdvi;
    public final double avgEvi;
    public final double avgMsavi;

    public SatelliteLandData(
            @NonNull List<SpectralPixel> pixels,
            double avgRed,
            double avgNir,
            double avgSwir,
            double avgNdvi,
            double avgEvi,
            double avgMsavi
    ) {
        this.pixels = Collections.unmodifiableList(pixels);
        this.avgRed = avgRed;
        this.avgNir = avgNir;
        this.avgSwir = avgSwir;
        this.avgNdvi = avgNdvi;
        this.avgEvi = avgEvi;
        this.avgMsavi = avgMsavi;
    }
}
