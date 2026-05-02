package com.example.geoagri.chat.input;

import android.Manifest;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Handles camera capture and gallery image selection.
 */
public final class CameraImageInputManager {

    public interface Callback {
        void onImageReady(@NonNull ImageCompressionUtil.EncodedImage image);

        void onError(@NonNull String message);
    }

    private final Fragment fragment;
    private final Callback callback;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private final ActivityResultLauncher<String[]> permissionLauncher;
    private final ActivityResultLauncher<Uri> takePictureLauncher;
    private final ActivityResultLauncher<PickVisualMediaRequest> pickMediaLauncher;

    @Nullable
    private Uri pendingCameraUri;
    @Nullable
    private Runnable pendingAction;

    public CameraImageInputManager(@NonNull Fragment fragment, @NonNull Callback callback) {
        this.fragment = fragment;
        this.callback = callback;

        permissionLauncher = fragment.registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                result -> {
                    boolean granted = true;
                    for (Boolean value : result.values()) {
                        if (!Boolean.TRUE.equals(value)) {
                            granted = false;
                            break;
                        }
                    }
                    if (!granted) {
                        callback.onError("Required camera/gallery permission denied.");
                        pendingAction = null;
                        return;
                    }
                    if (pendingAction != null) {
                        Runnable action = pendingAction;
                        pendingAction = null;
                        action.run();
                    }
                }
        );

        takePictureLauncher = fragment.registerForActivityResult(
                new ActivityResultContracts.TakePicture(),
                success -> {
                    if (!Boolean.TRUE.equals(success) || pendingCameraUri == null) {
                        callback.onError("Camera capture failed.");
                        return;
                    }
                    processUri(pendingCameraUri);
                }
        );

        pickMediaLauncher = fragment.registerForActivityResult(
                new ActivityResultContracts.PickVisualMedia(),
                uri -> {
                    if (uri == null) {
                        callback.onError("No image selected.");
                        return;
                    }
                    processUri(uri);
                }
        );
    }

    public void openChooser() {
        Context context = fragment.getContext();
        if (context == null) {
            callback.onError("Context unavailable for image selection.");
            return;
        }

        String[] options = new String[]{"Capture from Camera", "Select from Gallery"};
        new AlertDialog.Builder(context)
                .setTitle("Upload Plant Image")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        launchCamera();
                    } else {
                        launchGallery();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void launchCamera() {
        String[] perms = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                ? new String[]{Manifest.permission.CAMERA}
                : new String[]{Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE};

        if (!hasPermissions(perms)) {
            pendingAction = this::launchCamera;
            permissionLauncher.launch(perms);
            return;
        }

        Context context = fragment.getContext();
        if (context == null) {
            callback.onError("Context unavailable for camera.");
            return;
        }

        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.DISPLAY_NAME, "farm_capture_" + System.currentTimeMillis() + ".jpg");
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
        pendingCameraUri = context.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

        if (pendingCameraUri == null) {
            callback.onError("Unable to allocate image file for camera capture.");
            return;
        }

        takePictureLauncher.launch(pendingCameraUri);
    }

    private void launchGallery() {
        String[] perms = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                ? new String[]{Manifest.permission.READ_MEDIA_IMAGES}
                : new String[]{Manifest.permission.READ_EXTERNAL_STORAGE};

        if (!hasPermissions(perms)) {
            pendingAction = this::launchGallery;
            permissionLauncher.launch(perms);
            return;
        }

        pickMediaLauncher.launch(new PickVisualMediaRequest.Builder()
                .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                .build());
    }

    private boolean hasPermissions(@NonNull String[] permissions) {
        Context context = fragment.getContext();
        if (context == null) {
            return false;
        }
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    private void processUri(@NonNull Uri uri) {
        Context context = fragment.getContext();
        if (context == null) {
            callback.onError("Context unavailable while processing image.");
            return;
        }
        ContentResolver resolver = context.getContentResolver();

        executor.execute(() -> {
            try {
                ImageCompressionUtil.EncodedImage image = ImageCompressionUtil.compressAndEncode(resolver, uri);
                if (fragment.isAdded()) {
                    fragment.requireActivity().runOnUiThread(() -> callback.onImageReady(image));
                }
            } catch (IOException e) {
                if (fragment.isAdded()) {
                    fragment.requireActivity().runOnUiThread(
                            () -> callback.onError("Image processing failed: " + e.getMessage())
                    );
                }
            }
        });
    }

    public void release() {
        executor.shutdownNow();
    }
}
