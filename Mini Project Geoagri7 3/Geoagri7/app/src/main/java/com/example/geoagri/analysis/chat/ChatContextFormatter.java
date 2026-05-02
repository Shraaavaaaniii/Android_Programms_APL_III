package com.example.geoagri.analysis.chat;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.geoagri.analysis.model.FarmAnalysisResult;

import java.util.Locale;

public final class ChatContextFormatter {

    private ChatContextFormatter() {
    }

    @Nullable
    public static String format(@Nullable FarmAnalysisResult result) {
        if (result == null) {
            return null;
        }
        return "Detected Farm Crop: " + result.cropName + "\n"
                + "Location: " + result.locationName + "\n"
                + String.format(Locale.US, "NDVI Average: %.2f", result.ndviAvg);
    }

    @NonNull
    public static String appendToQuestion(@NonNull String userQuestion, @Nullable FarmAnalysisResult result) {
        String context = format(result);
        if (context == null || context.trim().isEmpty()) {
            return userQuestion;
        }
        return userQuestion.trim() + "\n\nFarm Context:\n" + context;
    }

    @NonNull
    public static String cropAwareSystemInstruction(@Nullable FarmAnalysisResult result) {
        String base = "You are an agricultural expert AI assistant helping farmers understand crop health. "
                + "Use detected crop context when available. Explain simply and provide practical actions.";

        String context = format(result);
        if (context == null) {
            return base;
        }
        return base + "\n\nCurrent Farm Detection:\n" + context;
    }
}
