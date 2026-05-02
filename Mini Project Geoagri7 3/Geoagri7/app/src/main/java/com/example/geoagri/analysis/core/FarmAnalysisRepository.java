package com.example.geoagri.analysis.core;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.geoagri.analysis.model.FarmAnalysisResult;

import java.util.concurrent.atomic.AtomicReference;

public final class FarmAnalysisRepository {
    private final AtomicReference<FarmAnalysisResult> latest = new AtomicReference<>();

    public void save(@NonNull FarmAnalysisResult result) {
        latest.set(result);
    }

    @Nullable
    public FarmAnalysisResult getLatest() {
        return latest.get();
    }

    public void clear() {
        latest.set(null);
    }
}
