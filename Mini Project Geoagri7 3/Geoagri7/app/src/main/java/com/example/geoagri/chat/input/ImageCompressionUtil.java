package com.example.geoagri.chat.input;

import android.content.ContentResolver;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Base64;
import android.webkit.MimeTypeMap;

import androidx.annotation.NonNull;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Utility for compressing image content and converting to Base64 payload.
 */
public final class ImageCompressionUtil {

    private static final int MAX_DIMENSION = 1280;
    private static final int MAX_BYTES = 600 * 1024;

    private ImageCompressionUtil() {
    }

    @NonNull
    public static EncodedImage compressAndEncode(@NonNull ContentResolver resolver, @NonNull Uri uri)
            throws IOException {
        Bitmap bitmap = decodeScaledBitmap(resolver, uri, MAX_DIMENSION);
        if (bitmap == null) {
            throw new IOException("Unable to decode selected image.");
        }

        byte[] jpeg = compressJpeg(bitmap, MAX_BYTES);
        String base64 = Base64.encodeToString(jpeg, Base64.NO_WRAP);
        String mimeType = resolveMimeType(resolver, uri);
        return new EncodedImage(base64, mimeType);
    }

    private static Bitmap decodeScaledBitmap(ContentResolver resolver, Uri uri, int maxDim) throws IOException {
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        try (InputStream in = resolver.openInputStream(uri)) {
            if (in == null) {
                return null;
            }
            BitmapFactory.decodeStream(in, null, bounds);
        }

        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            return null;
        }

        int sample = 1;
        int w = bounds.outWidth;
        int h = bounds.outHeight;
        while ((w / sample) > maxDim || (h / sample) > maxDim) {
            sample *= 2;
        }

        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inSampleSize = sample;
        try (InputStream in = resolver.openInputStream(uri)) {
            if (in == null) {
                return null;
            }
            return BitmapFactory.decodeStream(in, null, opts);
        }
    }

    private static byte[] compressJpeg(Bitmap bitmap, int maxBytes) {
        int quality = 88;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out);

        while (out.size() > maxBytes && quality > 35) {
            quality -= 8;
            out.reset();
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out);
        }

        return out.toByteArray();
    }

    @NonNull
    private static String resolveMimeType(@NonNull ContentResolver resolver, @NonNull Uri uri) {
        String mime = resolver.getType(uri);
        if (mime != null && !mime.trim().isEmpty()) {
            return mime;
        }

        String ext = MimeTypeMap.getFileExtensionFromUrl(uri.toString());
        if (ext != null) {
            String guessed = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext.toLowerCase());
            if (guessed != null) {
                return guessed;
            }
        }
        return "image/jpeg";
    }

    public static final class EncodedImage {
        @NonNull public final String base64;
        @NonNull public final String mimeType;

        public EncodedImage(@NonNull String base64, @NonNull String mimeType) {
            this.base64 = base64;
            this.mimeType = mimeType;
        }
    }
}
