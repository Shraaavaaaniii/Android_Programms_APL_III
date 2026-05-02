package com.example.geoagri.dashboard;

import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.geoagri.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class PersonalInformationActivity extends AppCompatActivity {

    private TextView nameValue;
    private TextView phoneValue;
    private TextView emailValue;
    private TextView locationValue;
    private TextView emptyText;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_personal_information);

        nameValue = findViewById(R.id.valueName);
        phoneValue = findViewById(R.id.valuePhone);
        emailValue = findViewById(R.id.valueEmail);
        locationValue = findViewById(R.id.valueLocation);
        emptyText = findViewById(R.id.textEmptyInfo);
        progressBar = findViewById(R.id.progressInfo);

        fetchPersonalInformation();
    }

    private void fetchPersonalInformation() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            showNoData();
            showToast("Please login again.");
            return;
        }

        try {
            progressBar.setVisibility(View.VISIBLE);
            FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(user.getUid())
                    .get()
                    .addOnSuccessListener(this::renderUserData)
                    .addOnFailureListener(e -> {
                        progressBar.setVisibility(View.GONE);
                        showNoData();
                        showToast("Failed to fetch data. Check network.");
                    });
        } catch (Exception e) {
            progressBar.setVisibility(View.GONE);
            showNoData();
            showToast("Something went wrong while loading data.");
        }
    }

    private void renderUserData(DocumentSnapshot snapshot) {
        progressBar.setVisibility(View.GONE);
        if (snapshot == null || !snapshot.exists()) {
            showNoData();
            return;
        }

        String name = snapshot.getString("name");
        String phone = snapshot.getString("phone");
        String email = snapshot.getString("email");
        String location = snapshot.getString("location");

        nameValue.setText(valueOrFallback(name));
        phoneValue.setText(valueOrFallback(phone));
        emailValue.setText(valueOrFallback(email));
        locationValue.setText(valueOrFallback(location));

        emptyText.setVisibility(View.GONE);
    }

    private String valueOrFallback(String value) {
        return (value == null || value.trim().isEmpty()) ? "No data available" : value;
    }

    private void showNoData() {
        emptyText.setVisibility(View.VISIBLE);
        nameValue.setText("No data available");
        phoneValue.setText("No data available");
        emailValue.setText("No data available");
        locationValue.setText("No data available");
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
