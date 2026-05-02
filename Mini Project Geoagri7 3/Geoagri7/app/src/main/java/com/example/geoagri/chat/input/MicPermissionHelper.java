package com.example.geoagri.chat.input;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

public final class MicPermissionHelper {

    public interface PermissionCallback {
        void onPermissionResult(boolean granted);
    }

    private final Fragment fragment;
    private final PermissionCallback callback;
    private final ActivityResultLauncher<String> permissionLauncher;

    public MicPermissionHelper(@NonNull Fragment fragment, @NonNull PermissionCallback callback) {
        this.fragment = fragment;
        this.callback = callback;
        this.permissionLauncher = fragment.registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                granted -> this.callback.onPermissionResult(Boolean.TRUE.equals(granted))
        );
    }

    public boolean hasRecordAudioPermission() {
        Context context = fragment.getContext();
        if (context == null) {
            return false;
        }
        return ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED;
    }

    public void requestRecordAudioPermission() {
        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO);
    }
}
