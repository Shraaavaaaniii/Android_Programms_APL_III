package com.example.geoagri.analysis.net;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.geoagri.analysis.core.FarmAnalysisException;
import com.example.geoagri.analysis.core.PolygonMath;
import com.example.geoagri.analysis.model.GeoPoint;
import com.example.geoagri.analysis.model.SpectralPixel;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public final class SatelliteSpectralService {

    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private final OkHttpClient httpClient;
    @NonNull private final String endpointUrl;
    @Nullable private final String apiKey;

    public SatelliteSpectralService(
            @NonNull OkHttpClient httpClient,
            @NonNull String endpointUrl,
            @Nullable String apiKey
    ) {
        this.httpClient = httpClient;
        this.endpointUrl = endpointUrl;
        this.apiKey = apiKey;
    }

    @NonNull
    public List<SpectralPixel> fetchSpectralData(@NonNull List<GeoPoint> polygon) throws FarmAnalysisException {
        PolygonMath.validatePolygon(polygon);

        JSONObject payload = new JSONObject();
        try {
            payload.put("polygon", PolygonMath.encodePolygon(polygon));
            payload.put("bands", new JSONArray().put("RED").put("NIR").put("SWIR"));
        } catch (JSONException e) {
            throw new FarmAnalysisException("Failed to build satellite request payload.", e);
        }

        RequestBody requestBody = RequestBody.create(payload.toString(), JSON);
        Request.Builder requestBuilder = new Request.Builder()
                .url(endpointUrl)
                .addHeader("Content-Type", "application/json")
                .post(requestBody);

        if (apiKey != null && !apiKey.trim().isEmpty()) {
            requestBuilder.addHeader("Authorization", "Bearer " + apiKey.trim());
            requestBuilder.addHeader("x-api-key", apiKey.trim());
        }

        try (Response response = httpClient.newCall(requestBuilder.build()).execute()) {
            String body = response.body() != null ? response.body().string() : "";
            if (!response.isSuccessful()) {
                throw new FarmAnalysisException("Satellite API failed (" + response.code() + "): " + body);
            }
            return parsePixels(body);
        } catch (IOException e) {
            throw new FarmAnalysisException("Satellite API network error.", e);
        }
    }

    @NonNull
    private List<SpectralPixel> parsePixels(@NonNull String json) throws FarmAnalysisException {
        try {
            JSONObject root = new JSONObject(json);
            JSONArray pixelArray = firstArray(root,
                    "pixels",
                    "data",
                    "samples",
                    "results"
            );

            if (pixelArray == null) {
                throw new FarmAnalysisException("Satellite response missing pixel array.");
            }

            List<SpectralPixel> pixels = new ArrayList<>(pixelArray.length());
            for (int i = 0; i < pixelArray.length(); i++) {
                JSONObject pixel = pixelArray.optJSONObject(i);
                if (pixel == null) {
                    continue;
                }

                double red = readBand(pixel, "RED", "red", "B4", "band4");
                double nir = readBand(pixel, "NIR", "nir", "B8", "band8");
                double swir = readBand(pixel, "SWIR", "swir", "B11", "band11");

                SpectralPixel spectralPixel = new SpectralPixel(red, nir, swir);
                if (spectralPixel.isValid()) {
                    pixels.add(spectralPixel);
                }
            }

            if (pixels.isEmpty()) {
                throw new FarmAnalysisException("Satellite response contains no valid RED/NIR/SWIR pixels.");
            }
            return pixels;
        } catch (JSONException e) {
            throw new FarmAnalysisException("Invalid satellite response format.", e);
        }
    }

    @Nullable
    private JSONArray firstArray(@NonNull JSONObject root, String... keys) {
        for (String key : keys) {
            JSONArray arr = root.optJSONArray(key);
            if (arr != null) {
                return arr;
            }
        }
        return null;
    }

    private double readBand(@NonNull JSONObject pixel, String... keys) {
        for (String key : keys) {
            if (pixel.has(key)) {
                return pixel.optDouble(key, Double.NaN);
            }
            JSONObject bands = pixel.optJSONObject("bands");
            if (bands != null && bands.has(key)) {
                return bands.optDouble(key, Double.NaN);
            }
            JSONObject properties = pixel.optJSONObject("properties");
            if (properties != null && properties.has(key)) {
                return properties.optDouble(key, Double.NaN);
            }
        }
        return Double.NaN;
    }
}
