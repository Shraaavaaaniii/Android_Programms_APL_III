package com.example.geoagri.chatbot;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import android.util.Log;

import com.example.geoagri.BuildConfig;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.dnsoverhttps.DnsOverHttps;

/**
 * Networking layer for Gemini REST API.
 */
public final class GeminiApiService {
    private static final String TAG = "GeminiApiService";

    public interface GeminiCallback {
        void onSuccess(@NonNull String aiResponse);

        void onError(@NonNull String errorMessage);
    }

    private static final String SYSTEM_INSTRUCTION =
            "You are an agricultural expert AI assistant helping farmers understand crop health. "
                    + "Explain problems simply and give practical solutions.";

    private static final String GEMINI_TEXT_ENDPOINT_FORMAT =
            "https://generativelanguage.googleapis.com/%s/models/%s:generateContent";

    private static final String GEMINI_MODELS_ENDPOINT =
            "https://generativelanguage.googleapis.com/v1beta/models";

    private static final List<String> API_VERSIONS = Arrays.asList(
            "v1beta",
            "v1"
    );

    private static final List<String> MODEL_FALLBACKS = Arrays.asList(
            "gemini-2.0-flash",
            "gemini-2.0-flash-lite",
            "gemini-1.5-flash"
    );

    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private final OkHttpClient httpClient;

    public GeminiApiService() {
        // Conservative timeouts for mobile network instability + DoH fallback for emulator DNS issues.
        httpClient = createHttpClient();
    }

    public void askGemini(
            @NonNull String userQuestion,
            @Nullable String farmData,
            @NonNull GeminiCallback callback
    ) {
        String apiKey = BuildConfig.GEMINI_API_KEY;
        if (apiKey == null || apiKey.trim().isEmpty()) {
            callback.onError("Gemini API key is missing. Add GEMINI_API_KEY to local.properties and rebuild the app.");
            return;
        }
        apiKey = apiKey.trim();

        String prompt = buildPrompt(userQuestion, farmData);
        JSONObject payload;
        try {
            payload = buildRequestBody(prompt);
        } catch (JSONException e) {
            Log.e(TAG, "Failed to build Gemini request payload.", e);
            callback.onError("Failed to build request payload.");
            return;
        }
        fetchSupportedModelEndpoints(apiKey, payload, callback);
    }

    @NonNull
    private JSONObject buildRequestBody(@NonNull String combinedPrompt) throws JSONException {
        JSONObject root = new JSONObject();

        JSONArray contents = new JSONArray();
        JSONObject contentItem = new JSONObject();
        contentItem.put("role", "user");
        JSONArray parts = new JSONArray();
        JSONObject textPart = new JSONObject();
        textPart.put("text", SYSTEM_INSTRUCTION + "\n\n" + combinedPrompt);
        parts.put(textPart);
        contentItem.put("parts", parts);
        contents.put(contentItem);

        root.put("contents", contents);
        return root;
    }

    @NonNull
    private String buildPrompt(@NonNull String userQuestion, @Nullable String farmData) {
        StringBuilder builder = new StringBuilder();
        builder.append("User Question: ").append(userQuestion.trim());

        if (farmData != null && !farmData.trim().isEmpty()) {
            builder.append("\n\nFarm Data: ").append(farmData.trim());
        }

        return builder.toString();
    }

    @NonNull
    private String parseAiReply(@NonNull String json) throws JSONException {
        JSONObject root = new JSONObject(json);
        JSONArray candidates = root.optJSONArray("candidates");
        if (candidates == null || candidates.length() == 0) {
            return "I could not generate an answer right now. Please try again.";
        }

        JSONObject firstCandidate = candidates.optJSONObject(0);
        if (firstCandidate == null) {
            return "I could not generate an answer right now. Please try again.";
        }

        JSONObject content = firstCandidate.optJSONObject("content");
        if (content == null) {
            return "I could not generate an answer right now. Please try again.";
        }

        JSONArray parts = content.optJSONArray("parts");
        if (parts == null || parts.length() == 0) {
            return "I could not generate an answer right now. Please try again.";
        }

        StringBuilder reply = new StringBuilder();
        for (int i = 0; i < parts.length(); i++) {
            JSONObject part = parts.optJSONObject(i);
            if (part == null) {
                continue;
            }
            String text = part.optString("text", "");
            if (!text.isEmpty()) {
                if (reply.length() > 0) {
                    reply.append("\n");
                }
                reply.append(text);
            }
        }

        if (reply.length() == 0) {
            return "I could not generate an answer right now. Please try again.";
        }

        return reply.toString();
    }

    @NonNull
    private String extractErrorMessage(@NonNull String responseBody) {
        try {
            JSONObject root = new JSONObject(responseBody);
            JSONObject error = root.optJSONObject("error");
            if (error == null) {
                return "Unexpected API response.";
            }
            String message = error.optString("message", "Unexpected API response.");
            return message.isEmpty() ? "Unexpected API response." : message;
        } catch (JSONException ignored) {
            return "Unexpected API response.";
        }
    }

    @NonNull
    private List<HttpUrl> buildFallbackEndpointUrls(@NonNull String apiKey) {
        java.util.ArrayList<HttpUrl> urls = new java.util.ArrayList<>();
        for (String apiVersion : API_VERSIONS) {
            for (String model : MODEL_FALLBACKS) {
                HttpUrl endpoint = HttpUrl.parse(String.format(GEMINI_TEXT_ENDPOINT_FORMAT, apiVersion, model));
                if (endpoint != null) {
                    urls.add(endpoint.newBuilder()
                            .addQueryParameter("key", apiKey)
                            .build());
                }
            }
        }
        return urls;
    }

    private void fetchSupportedModelEndpoints(
            @NonNull String apiKey,
            @NonNull JSONObject payload,
            @NonNull GeminiCallback callback
    ) {
        HttpUrl endpoint = HttpUrl.parse(GEMINI_MODELS_ENDPOINT);
        if (endpoint == null) {
            askGeminiApi(0, buildFallbackEndpointUrls(apiKey), payload, callback);
            return;
        }

        HttpUrl url = endpoint.newBuilder()
                .addQueryParameter("key", apiKey)
                .build();

        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                callback.onError(buildNetworkErrorMessage(e));
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String responseBody = response.body() != null ? response.body().string() : "";

                if (!response.isSuccessful()) {
                    String error = extractErrorMessage(responseBody);
                    callback.onError(buildApiErrorMessage(response.code(), error));
                    return;
                }

                List<HttpUrl> endpointUrls = parseGenerateContentEndpoints(apiKey, responseBody);
                if (endpointUrls.isEmpty()) {
                    Log.e(TAG, "No generateContent models returned by Gemini models.list; using static fallback endpoints.");
                    endpointUrls = buildFallbackEndpointUrls(apiKey);
                }
                askGeminiApi(0, endpointUrls, payload, callback);
            }
        });
    }

    @NonNull
    private List<HttpUrl> parseGenerateContentEndpoints(@NonNull String apiKey, @NonNull String responseBody) {
        java.util.ArrayList<String> modelNames = new java.util.ArrayList<>();
        try {
            JSONObject root = new JSONObject(responseBody);
            JSONArray models = root.optJSONArray("models");
            if (models == null) {
                return new java.util.ArrayList<>();
            }

            for (int i = 0; i < models.length(); i++) {
                JSONObject model = models.optJSONObject(i);
                if (model == null) {
                    continue;
                }
                JSONArray methods = model.optJSONArray("supportedGenerationMethods");
                if (!supportsGenerateContent(methods)) {
                    continue;
                }
                String name = model.optString("name", "");
                if (name.startsWith("models/")) {
                    modelNames.add(name);
                }
            }
        } catch (JSONException e) {
            Log.e(TAG, "Failed to parse Gemini models.list response.", e);
        }

        return buildPrioritizedModelUrls(apiKey, modelNames);
    }

    private boolean supportsGenerateContent(@Nullable JSONArray methods) {
        if (methods == null) {
            return false;
        }
        for (int i = 0; i < methods.length(); i++) {
            if ("generateContent".equals(methods.optString(i))) {
                return true;
            }
        }
        return false;
    }

    @NonNull
    private List<HttpUrl> buildPrioritizedModelUrls(
            @NonNull String apiKey,
            @NonNull List<String> modelNames
    ) {
        java.util.ArrayList<String> prioritizedNames = new java.util.ArrayList<>();
        addMatchingModels(prioritizedNames, modelNames, "gemini-2.5-flash");
        addMatchingModels(prioritizedNames, modelNames, "gemini-2.0-flash");
        addMatchingModels(prioritizedNames, modelNames, "gemini-1.5-flash");
        addMatchingModels(prioritizedNames, modelNames, "gemini");

        java.util.ArrayList<HttpUrl> urls = new java.util.ArrayList<>();
        for (String modelName : prioritizedNames) {
            HttpUrl endpoint = HttpUrl.parse("https://generativelanguage.googleapis.com/v1beta/"
                    + modelName + ":generateContent");
            if (endpoint != null) {
                urls.add(endpoint.newBuilder()
                        .addQueryParameter("key", apiKey)
                        .build());
            }
        }
        return urls;
    }

    private void addMatchingModels(
            @NonNull List<String> prioritizedNames,
            @NonNull List<String> modelNames,
            @NonNull String namePart
    ) {
        for (String modelName : modelNames) {
            if (modelName.contains(namePart) && !prioritizedNames.contains(modelName)) {
                prioritizedNames.add(modelName);
            }
        }
    }

    private void askGeminiApi(
            int endpointIndex,
            @NonNull List<HttpUrl> endpointUrls,
            @NonNull JSONObject payload,
            @NonNull GeminiCallback callback
    ) {
        if (endpointIndex >= endpointUrls.size()) {
            callback.onError("Gemini API request failed. No compatible Gemini model endpoint was available for this API key/project.");
            return;
        }

        RequestBody body = RequestBody.create(payload.toString(), JSON);
        Request request = new Request.Builder()
                .url(endpointUrls.get(endpointIndex))
                .addHeader("Content-Type", "application/json")
                .post(body)
                .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                callback.onError(buildNetworkErrorMessage(e));
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String responseBody = response.body() != null ? response.body().string() : "";

                if (!response.isSuccessful()) {
                    String error = extractErrorMessage(responseBody);
                    if (shouldTryNextEndpoint(response.code(), error) && endpointIndex + 1 < endpointUrls.size()) {
                        askGeminiApi(endpointIndex + 1, endpointUrls, payload, callback);
                        return;
                    }
                    callback.onError(buildApiErrorMessage(response.code(), error));
                    return;
                }

                try {
                    callback.onSuccess(parseAiReply(responseBody));
                } catch (JSONException parseException) {
                    callback.onError("Gemini returned an unreadable response. Please try again.");
                }
            }
        });
    }

    private boolean shouldTryNextEndpoint(int statusCode, @NonNull String detail) {
        String lower = detail.toLowerCase();
        return statusCode == 404
                || lower.contains("not found")
                || lower.contains("not supported")
                || lower.contains("model")
                || lower.contains("api version");
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
            prefix = "Gemini API request failed.";
        }
        Log.e(TAG, "Gemini API error (" + statusCode + "): " + detail);
        return prefix + "\n\nDetails: API error (" + statusCode + "): " + detail;
    }

    @NonNull
    private String buildNetworkErrorMessage(@NonNull IOException error) {
        if (error instanceof UnknownHostException) {
            Log.e(TAG, "Cannot reach Gemini API.", error);
            return "Network error: cannot reach Gemini API. Check internet connectivity or emulator DNS settings.";
        }
        if (error instanceof SocketTimeoutException) {
            Log.e(TAG, "Gemini API request timed out.", error);
            return "Network error: Gemini API request timed out. Please retry on a stable connection.";
        }
        Log.e(TAG, "Network error while calling Gemini API.", error);
        return "Network error while calling Gemini API: " + error.getMessage();
    }

    @NonNull
    private String buildOfflineFarmingReplyFromPrompt(@Nullable JSONArray contents) {
        if (contents != null && contents.length() > 0) {
            JSONObject first = contents.optJSONObject(0);
            if (first != null) {
                JSONArray parts = first.optJSONArray("parts");
                if (parts != null && parts.length() > 0) {
                    JSONObject textPart = parts.optJSONObject(0);
                    if (textPart != null) {
                        String text = textPart.optString("text", "");
                        if (!text.trim().isEmpty()) {
                            return buildOfflineFarmingReply(text, null);
                        }
                    }
                }
            }
        }
        return buildOfflineFarmingReply("", null);
    }

    @NonNull
    private String buildOfflineFarmingReply(@Nullable String userQuestion, @Nullable String farmData) {
        String question = userQuestion == null ? "" : userQuestion.toLowerCase().trim();
        StringBuilder reply = new StringBuilder();

        reply.append("AI service is running in demo mode right now, so here is a local farming suggestion.\n\n");

        if (question.contains("unhealthy") || question.contains("disease") || question.contains("yellow")) {
            reply.append("Possible reasons your farm looks unhealthy are low soil moisture, nutrient deficiency, pest attack, or fungal disease.\n\n");
            reply.append("Recommended actions:\n");
            reply.append("1. Check irrigation and soil moisture.\n");
            reply.append("2. Inspect leaves for yellowing, spots, or insects.\n");
            reply.append("3. Review nitrogen and micronutrient levels.\n");
            reply.append("4. Remove infected plants if disease is spreading.\n");
        } else if (question.contains("water") || question.contains("irrigation")) {
            reply.append("Check whether the field is getting uniform irrigation. Uneven watering often causes weak growth and patchy crop health.\n");
        } else if (question.contains("fertilizer") || question.contains("nutrient")) {
            reply.append("A balanced fertilizer plan should be based on soil condition, crop stage, and visible deficiency symptoms. Nitrogen deficiency is a common cause of pale leaves.\n");
        } else {
            reply.append("Crop stress usually happens because of irrigation issues, nutrient imbalance, pests, disease, or weather stress.\n\n");
            reply.append("Start by checking water availability, leaf color, pest presence, and soil condition.\n");
        }

        if (farmData != null && !farmData.trim().isEmpty()) {
            reply.append("\nFarm context: ").append(farmData.trim()).append("\n");
        }

        reply.append("\nFor presentation: this response is shown through the app's AI assistant flow.");
        return reply.toString();
    }

    @NonNull
    private OkHttpClient createHttpClient() {
        OkHttpClient.Builder baseBuilder = new OkHttpClient.Builder()
                .connectTimeout(20, TimeUnit.SECONDS)
                .readTimeout(35, TimeUnit.SECONDS)
                .writeTimeout(20, TimeUnit.SECONDS);

        try {
            // Use DNS-over-HTTPS to bypass broken emulator/local DNS resolvers.
            OkHttpClient bootstrapClient = new OkHttpClient.Builder()
                    .connectTimeout(20, TimeUnit.SECONDS)
                    .readTimeout(20, TimeUnit.SECONDS)
                    .writeTimeout(20, TimeUnit.SECONDS)
                    .build();

            DnsOverHttps dohDns = new DnsOverHttps.Builder()
                    .client(bootstrapClient)
                    .url(HttpUrl.get("https://dns.google/dns-query"))
                    .bootstrapDnsHosts(
                            InetAddress.getByName("8.8.8.8"),
                            InetAddress.getByName("8.8.4.4")
                    )
                    .includeIPv6(false)
                    .build();

            baseBuilder.dns(dohDns);
        } catch (Exception ignored) {
            // Fallback to system DNS if DoH setup fails.
        }

        return baseBuilder.build();
    }
}
