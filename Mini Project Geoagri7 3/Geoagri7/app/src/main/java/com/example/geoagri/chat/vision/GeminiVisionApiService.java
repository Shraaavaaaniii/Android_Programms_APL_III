package com.example.geoagri.chat.vision;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import android.util.Log;

import com.example.geoagri.BuildConfig;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public final class GeminiVisionApiService {
    private static final String TAG = "GeminiVisionApiService";

    public interface VisionCallback {
        void onSuccess(@NonNull String responseText);

        void onError(@NonNull String errorMessage);
    }

    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private static final List<String> ENDPOINTS = Arrays.asList(
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=%s",
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash-lite:generateContent?key=%s",
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=%s"
    );

    private static final String SYSTEM_PROMPT = "You are an expert agricultural scientist. "
            + "If user provides a plant image: "
            + "1. Identify crop type. "
            + "2. Detect disease or deficiency if visible. "
            + "3. Explain problem simply. "
            + "4. Suggest practical solution for farmers. "
            + "5. Avoid technical jargon.";

    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(45, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build();

    public void analyzeImage(
            @NonNull String base64Image,
            @NonNull String mimeType,
            @Nullable String farmerQuestion,
            @NonNull VisionCallback callback
    ) {
        String apiKey = BuildConfig.GEMINI_API_KEY;
        if (apiKey == null || apiKey.trim().isEmpty()) {
            callback.onError("Gemini API key is missing. Add GEMINI_API_KEY to local.properties and rebuild the app.");
            return;
        }

        JSONObject payload;
        try {
            payload = buildPayload(base64Image, mimeType, farmerQuestion);
        } catch (JSONException e) {
            Log.e(TAG, "Failed to build Gemini vision payload.", e);
            callback.onError("Failed to build vision payload.");
            return;
        }

        requestWithFallback(0, apiKey.trim(), payload, callback);
    }

    private void requestWithFallback(
            int index,
            @NonNull String apiKey,
            @NonNull JSONObject payload,
            @NonNull VisionCallback callback
    ) {
        if (index >= ENDPOINTS.size()) {
            callback.onError("Gemini vision request failed for all configured model endpoints.");
            return;
        }

        String url = String.format(ENDPOINTS.get(index), apiKey);
        Request request = new Request.Builder()
                .url(url)
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create(payload.toString(), JSON))
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                callback.onError(buildNetworkErrorMessage(e));
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String body = response.body() != null ? response.body().string() : "";

                if (!response.isSuccessful()) {
                    String error = extractError(body);
                    String lower = error.toLowerCase();
                    if ((response.code() == 404 || lower.contains("model") || lower.contains("not found"))
                            && index + 1 < ENDPOINTS.size()) {
                        requestWithFallback(index + 1, apiKey, payload, callback);
                        return;
                    }
                    callback.onError(buildApiErrorMessage(response.code(), error));
                    return;
                }

                try {
                    callback.onSuccess(parseText(body));
                } catch (JSONException e) {
                    Log.e(TAG, "Gemini vision response parsing failed.", e);
                    callback.onError("Gemini returned an unreadable image-analysis response. Please try again.");
                }
            }
        });
    }

    @NonNull
    private String buildApiErrorMessage(int statusCode, @NonNull String detail) {
        String lower = detail.toLowerCase();
        String prefix;
        if (lower.contains("expired")) {
            prefix = "Gemini API key expired. Replace GEMINI_API_KEY in local.properties and rebuild the app.";
        } else if (statusCode == 400 || statusCode == 401 || statusCode == 403
                || lower.contains("api key not valid")
                || lower.contains("invalid")
                || lower.contains("permission")) {
            prefix = "Gemini API key or API permission issue detected. Verify the key, API restrictions, billing, and enabled Generative Language API.";
        } else {
            prefix = "Gemini vision API request failed.";
        }
        Log.e(TAG, "Gemini vision API error (" + statusCode + "): " + detail);
        return prefix + "\n\nDetails: API error (" + statusCode + "): " + detail;
    }

    @NonNull
    private String buildNetworkErrorMessage(@NonNull IOException error) {
        if (error instanceof UnknownHostException) {
            Log.e(TAG, "Cannot reach Gemini vision API.", error);
            return "Network error: cannot reach Gemini API. Check internet connectivity or emulator DNS settings.";
        }
        if (error instanceof SocketTimeoutException) {
            Log.e(TAG, "Gemini vision API request timed out.", error);
            return "Network error: Gemini API request timed out. Please retry on a stable connection.";
        }
        Log.e(TAG, "Network error while calling Gemini vision API.", error);
        return "Network error while calling Gemini API: " + error.getMessage();
    }

    @NonNull
    private String buildOfflineVisionReply(@Nullable String farmerQuestion) {
        String question = farmerQuestion == null ? "" : farmerQuestion.trim();
        StringBuilder reply = new StringBuilder();
        reply.append("Image analysis is running in demo mode right now.\n\n");
        reply.append("Possible observations:\n");
        reply.append("1. Crop may show stress due to water shortage, nutrient deficiency, or pest attack.\n");
        reply.append("2. Farmer should inspect leaf color, wilting, spots, and insect activity.\n");
        reply.append("3. Recommended next step is field inspection plus soil and irrigation check.\n");
        if (!question.isEmpty()) {
            reply.append("\nQuestion considered: ").append(question).append("\n");
        }
        reply.append("\nFor presentation: this simulates plant-image AI guidance when cloud analysis is unavailable.");
        return reply.toString();
    }

    @NonNull
    private JSONObject buildPayload(
            @NonNull String base64Image,
            @NonNull String mimeType,
            @Nullable String farmerQuestion
    ) throws JSONException {
        String question = (farmerQuestion == null || farmerQuestion.trim().isEmpty())
                ? "Analyze this plant image and identify crop type and health issues."
                : farmerQuestion.trim();

        String prompt = "Analyze this plant image and identify crop type and health issues.";
        if (!question.isEmpty()) {
            prompt += "\\n\\nFarmer Question: " + question
                    + "\\nIf both image and question are provided, first analyze image then answer question.";
        }

        JSONObject root = new JSONObject();

        JSONObject content = new JSONObject();
        content.put("role", "user");
        JSONArray parts = new JSONArray();
        parts.put(new JSONObject().put("text", SYSTEM_PROMPT + "\n\n" + prompt));

        JSONObject inline = new JSONObject();
        inline.put("mime_type", mimeType);
        inline.put("data", base64Image);
        parts.put(new JSONObject().put("inline_data", inline));

        content.put("parts", parts);
        root.put("contents", new JSONArray().put(content));
        return root;
    }

    @NonNull
    private String parseText(@NonNull String json) throws JSONException {
        JSONObject root = new JSONObject(json);
        JSONArray candidates = root.optJSONArray("candidates");
        if (candidates == null || candidates.length() == 0) {
            return "No analysis returned.";
        }

        JSONObject c0 = candidates.optJSONObject(0);
        if (c0 == null) {
            return "No analysis returned.";
        }

        JSONObject content = c0.optJSONObject("content");
        if (content == null) {
            return "No analysis returned.";
        }

        JSONArray parts = content.optJSONArray("parts");
        if (parts == null || parts.length() == 0) {
            return "No analysis returned.";
        }

        StringBuilder out = new StringBuilder();
        for (int i = 0; i < parts.length(); i++) {
            JSONObject p = parts.optJSONObject(i);
            if (p == null) {
                continue;
            }
            String t = p.optString("text", "");
            if (!t.isEmpty()) {
                if (out.length() > 0) {
                    out.append("\n");
                }
                out.append(t);
            }
        }

        return out.length() == 0 ? "No analysis returned." : out.toString();
    }

    @NonNull
    private String extractError(@NonNull String body) {
        try {
            JSONObject root = new JSONObject(body);
            JSONObject error = root.optJSONObject("error");
            if (error == null) {
                return "Unexpected API response.";
            }
            return error.optString("message", "Unexpected API response.");
        } catch (JSONException e) {
            return "Unexpected API response.";
        }
    }
}
