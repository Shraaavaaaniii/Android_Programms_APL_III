package com.example.geoagri.dashboard;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.geoagri.R;
import com.example.geoagri.auth.LoginActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class ProfileFragment extends Fragment {
    private static final int REQUEST_PICK_PROFILE_IMAGE = 7001;

    private FirebaseAuth auth;
    private FirebaseFirestore firestore;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private ImageView profileImageView;
    private TextView farmerNameText;
    private TextView farmerLocationText;
    private TextView notificationBadge;
    private TextView currentPlanText;
    private String currentPlan = "No plan selected";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_profile, container, false);

        profileImageView = root.findViewById(R.id.imageProfilePhoto);
        farmerNameText = root.findViewById(R.id.textFarmerName);
        farmerLocationText = root.findViewById(R.id.textFarmerLocation);
        ImageButton backButton = root.findViewById(R.id.buttonBackProfile);
        ImageButton editProfileButton = root.findViewById(R.id.buttonEditProfilePhoto);
        LinearLayout personalInfoCard = root.findViewById(R.id.cardPersonalInfo);
        LinearLayout myFarmsCard = root.findViewById(R.id.cardMyFarms);
        LinearLayout notificationsCard = root.findViewById(R.id.cardNotifications);
        LinearLayout subscriptionCard = root.findViewById(R.id.cardSubscription);
        LinearLayout logoutCard = root.findViewById(R.id.cardLogout);
        notificationBadge = root.findViewById(R.id.textNotificationBadge);
        currentPlanText = root.findViewById(R.id.textCurrentPlan);
        initializeFirebaseSafely();

        if (backButton != null) {
            backButton.setOnClickListener(v -> navigateToHomeTab());
        }

        if (personalInfoCard != null) {
            personalInfoCard.setOnClickListener(v ->
                    startActivity(new Intent(requireContext(), PersonalInformationActivity.class)));
        }

        if (myFarmsCard != null) {
            myFarmsCard.setOnClickListener(v ->
                    startActivity(new Intent(requireContext(), MyFarmsActivity.class)));
        }

        if (notificationsCard != null) {
            notificationsCard.setOnClickListener(v -> {
                startActivity(new Intent(requireContext(), NotificationsActivity.class));
                if (notificationBadge != null) {
                    notificationBadge.setVisibility(View.GONE);
                }
            });
        }

        if (subscriptionCard != null) {
            subscriptionCard.setOnClickListener(v ->
                    showToast("Current plan: " + currentPlan));
        }

        if (editProfileButton != null) {
            editProfileButton.setOnClickListener(v -> launchImagePicker());
        }

        if (logoutCard != null) {
            logoutCard.setOnClickListener(v -> logoutUser());
        }

        return root;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadFarmerProfile();
        loadUnreadNotificationCount();
    }

    private void loadFarmerProfile() {
        if (auth == null || firestore == null) {
            applyDefaultProfile();
            return;
        }

        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            applyDefaultProfile();
            return;
        }

        try {
            firestore.collection("users")
                    .document(user.getUid())
                    .get()
                    .addOnSuccessListener(snapshot -> {
                        if (!isAdded()) {
                            return;
                        }

                        if (snapshot == null || !snapshot.exists()) {
                            applyDefaultProfile();
                            return;
                        }

                        String name = snapshot.getString("name");
                        String location = snapshot.getString("location");
                        String plan = snapshot.getString("subscriptionPlan");
                        String imageUrl = snapshot.getString("profileImageUrl");

                        if (farmerNameText != null) {
                            farmerNameText.setText(isBlank(name) ? "Farmer John" : name);
                        }
                        if (farmerLocationText != null) {
                            farmerLocationText.setText(isBlank(location) ? "Salinas Valley, CA" : location);
                        }
                        currentPlan = isBlank(plan) ? "No plan selected" : plan;
                        if (currentPlanText != null) {
                            currentPlanText.setText(currentPlan);
                        }

                        if (!isBlank(imageUrl)) {
                            loadProfileImage(imageUrl);
                        } else {
                            if (profileImageView != null) {
                                profileImageView.setImageResource(R.drawable.ic_profile);
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        applyDefaultProfile();
                        showToast("Unable to load profile. Check network.");
                    });
        } catch (Exception e) {
            applyDefaultProfile();
            showToast("Unable to load profile details.");
        }
    }

    private void applyDefaultProfile() {
        if (farmerNameText != null) {
            farmerNameText.setText("Farmer John");
        }
        if (farmerLocationText != null) {
            farmerLocationText.setText("Salinas Valley, CA");
        }
        currentPlan = "No plan selected";
        if (currentPlanText != null) {
            currentPlanText.setText(currentPlan);
        }
        if (profileImageView != null) {
            profileImageView.setImageResource(R.drawable.ic_profile);
        }
    }

    private void loadProfileImage(@NonNull String imagePath) {
        try {
            if (imagePath.startsWith("content://") || imagePath.startsWith("file://")) {
                if (profileImageView != null) {
                    profileImageView.setImageURI(Uri.parse(imagePath));
                }
                return;
            }
            if (imagePath.startsWith("http://") || imagePath.startsWith("https://")) {
                new Thread(() -> {
                    try (InputStream inputStream = new URL(imagePath).openStream()) {
                        Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                        if (bitmap != null && isAdded() && profileImageView != null) {
                            mainHandler.post(() -> profileImageView.setImageBitmap(bitmap));
                        }
                    } catch (Exception ignored) {
                        if (isAdded() && profileImageView != null) {
                            mainHandler.post(() -> profileImageView.setImageResource(R.drawable.ic_profile));
                        }
                    }
                }).start();
                return;
            }
            if (profileImageView != null) {
                profileImageView.setImageResource(R.drawable.ic_profile);
            }
        } catch (Exception e) {
            if (profileImageView != null) {
                profileImageView.setImageResource(R.drawable.ic_profile);
            }
        }
    }

    private void launchImagePicker() {
        if (!isAdded()) {
            return;
        }
        try {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("image/*");
            startActivityForResult(intent, REQUEST_PICK_PROFILE_IMAGE);
        } catch (Exception e) {
            showToast("Unable to open image picker.");
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQUEST_PICK_PROFILE_IMAGE || resultCode != android.app.Activity.RESULT_OK || data == null) {
            return;
        }
        Uri uri = data.getData();
        if (uri == null || profileImageView == null || !isAdded()) {
            return;
        }
        try {
            final int takeFlags = data.getFlags() & Intent.FLAG_GRANT_READ_URI_PERMISSION;
            requireContext().getContentResolver().takePersistableUriPermission(uri, takeFlags);
        } catch (Exception ignored) {
            // Best-effort only.
        }
        try {
            profileImageView.setImageURI(uri);
            updateProfilePhoto(uri.toString());
        } catch (Exception e) {
            showToast("Unable to set selected image.");
        }
    }

    private void updateProfilePhoto(@NonNull String imageUri) {
        if (auth == null || firestore == null) {
            showToast("Profile service unavailable.");
            return;
        }

        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            showToast("Please login again.");
            return;
        }

        try {
            Map<String, Object> updates = new HashMap<>();
            updates.put("profileImageUrl", imageUri);
            firestore.collection("users")
                    .document(user.getUid())
                    .set(updates, SetOptions.merge())
                    .addOnFailureListener(e -> showToast("Failed to update profile image."));
        } catch (Exception e) {
            showToast("Unable to update profile image.");
        }
    }

    private void loadUnreadNotificationCount() {
        if (auth == null || firestore == null) {
            if (notificationBadge != null) {
                notificationBadge.setVisibility(View.GONE);
            }
            return;
        }

        FirebaseUser user = auth.getCurrentUser();
        if (user == null || notificationBadge == null) {
            if (notificationBadge != null) {
                notificationBadge.setVisibility(View.GONE);
            }
            return;
        }

        try {
            firestore.collection("notifications")
                    .whereEqualTo("userId", user.getUid())
                    .whereEqualTo("read", false)
                    .get()
                    .addOnSuccessListener(querySnapshot -> {
                        int count = querySnapshot.size();
                        if (count > 0) {
                            notificationBadge.setText(String.valueOf(count));
                            notificationBadge.setVisibility(View.VISIBLE);
                        } else {
                            notificationBadge.setVisibility(View.GONE);
                        }
                    })
                    .addOnFailureListener(e -> {
                        notificationBadge.setVisibility(View.GONE);
                        showToast("Unable to load notifications. Check network.");
                    });
        } catch (Exception e) {
            notificationBadge.setVisibility(View.GONE);
            showToast("Unable to load notifications.");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private void logoutUser() {
        try {
            if (auth != null) {
                auth.signOut();
            } else {
                FirebaseAuth.getInstance().signOut();
            }
            Intent intent = new Intent(requireContext(), LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        } catch (Exception e) {
            showToast("Logout failed. Please try again.");
        }
    }

    private void initializeFirebaseSafely() {
        try {
            FirebaseApp.initializeApp(requireContext());
            auth = FirebaseAuth.getInstance();
            firestore = FirebaseFirestore.getInstance();
        } catch (Exception e) {
            auth = null;
            firestore = null;
            showToast("Firebase not initialized.");
        }
    }

    private void navigateToHomeTab() {
        if (!isAdded()) {
            return;
        }
        View nav = requireActivity().findViewById(R.id.bottomNavigation);
        if (nav instanceof BottomNavigationView) {
            ((BottomNavigationView) nav).setSelectedItemId(R.id.homeFragment);
            return;
        }
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, new HomeFragment())
                .commit();
    }

    private void showToast(String message) {
        if (isAdded()) {
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        profileImageView = null;
        farmerNameText = null;
        farmerLocationText = null;
        notificationBadge = null;
        currentPlanText = null;
    }
}
