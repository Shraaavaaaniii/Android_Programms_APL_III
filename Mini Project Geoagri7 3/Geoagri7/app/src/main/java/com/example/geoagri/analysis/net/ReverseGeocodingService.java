package com.example.geoagri.analysis.net;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.geoagri.analysis.core.FarmAnalysisException;
import com.example.geoagri.analysis.model.LocationInfo;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public final class ReverseGeocodingService {

    private final OkHttpClient httpClient;
    @NonNull private final String endpointTemplate;
    @Nullable private final String apiKey;

    /**
     * endpointTemplate example:
     * https://api.bigdatacloud.net/data/reverse-geocode-client?latitude=%f&longitude=%f&localityLanguage=en
     */
    public ReverseGeocodingService(
            @NonNull OkHttpClient httpClient,
            @NonNull String endpointTemplate,
            @Nullable String apiKey
    ) {
        this.httpClient = httpClient;
        this.endpointTemplate = endpointTemplate;
        this.apiKey = apiKey;
    }

    @NonNull
    public LocationInfo reverseGeocode(double latitude, double longitude) throws FarmAnalysisException {
        String url = String.format(endpointTemplate, latitude, longitude);
        Request.Builder requestBuilder = new Request.Builder().url(url).get();

        if (apiKey != null && !apiKey.trim().isEmpty()) {
            requestBuilder.addHeader("Authorization", "Bearer " + apiKey.trim());
            requestBuilder.addHeader("x-api-key", apiKey.trim());
        }

        try (Response response = httpClient.newCall(requestBuilder.build()).execute()) {
            String body = response.body() != null ? response.body().string() : "";
            if (!response.isSuccessful()) {
                throw new FarmAnalysisException("Reverse geocoding failed (" + response.code() + "): " + body);
            }
            return parseLocation(body, latitude, longitude);
        } catch (IOException e) {
            throw new FarmAnalysisException("Reverse geocoding network error.", e);
        }
    }

    @NonNull
    private LocationInfo parseLocation(@NonNull String json, double latitude, double longitude) throws FarmAnalysisException {
        try {
            JSONObject root = new JSONObject(json);
            JSONObject address = root.optJSONObject("address");

            String village = firstNonEmpty(
                    fromAddress(address, "village"),
                    fromAddress(address, "town"),
                    fromAddress(address, "city"),
                    root.optString("locality", null),
                    root.optString("city", null)
            );

            String district = firstNonEmpty(
                    fromAddress(address, "state_district"),
                    fromAddress(address, "county"),
                    root.optString("principalSubdivision", null)
            );

            String state = firstNonEmpty(
                    fromAddress(address, "state"),
                    root.optString("principalSubdivision", null)
            );

            String country = firstNonEmpty(
                    fromAddress(address, "country"),
                    root.optString("countryName", null),
                    root.optString("country", null)
            );

            return new LocationInfo(village, district, state, country, latitude, longitude);
        } catch (JSONException e) {
            throw new FarmAnalysisException("Invalid reverse geocoding response format.", e);
        }
    }

    @Nullable
    private String fromAddress(@Nullable JSONObject address, @NonNull String key) {
        return address == null ? null : address.optString(key, null);
    }

    @Nullable
    private String firstNonEmpty(String... values) {
        for (String v : values) {
            if (v != null && !v.trim().isEmpty() && !"null".equalsIgnoreCase(v.trim())) {
                return v.trim();
            }
        }
        return null;
    }
}
