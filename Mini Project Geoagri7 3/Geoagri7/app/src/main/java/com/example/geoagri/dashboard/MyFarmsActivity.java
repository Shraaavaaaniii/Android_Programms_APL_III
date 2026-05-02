package com.example.geoagri.dashboard;

import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.geoagri.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class MyFarmsActivity extends AppCompatActivity {

    private final List<FarmItem> farms = new ArrayList<>();
    private FarmAdapter farmAdapter;

    private RecyclerView farmsRecycler;
    private ProgressBar progressBar;
    private TextView emptyText;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_farms);

        farmsRecycler = findViewById(R.id.recyclerFarms);
        progressBar = findViewById(R.id.progressFarms);
        emptyText = findViewById(R.id.textEmptyFarms);

        farmAdapter = new FarmAdapter(farms);
        farmsRecycler.setLayoutManager(new LinearLayoutManager(this));
        farmsRecycler.setAdapter(farmAdapter);

        fetchMyFarms();
    }

    private void fetchMyFarms() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            showEmptyState();
            showToast("Please login again.");
            return;
        }

        try {
            progressBar.setVisibility(View.VISIBLE);
            FirebaseFirestore.getInstance()
                    .collection("farms")
                    .whereEqualTo("userId", user.getUid())
                    .orderBy("timestamp")
                    .get()
                    .addOnSuccessListener(querySnapshot -> {
                        progressBar.setVisibility(View.GONE);
                        farms.clear();
                        farms.addAll(querySnapshot.toObjects(FarmItem.class));
                        farmAdapter.notifyDataSetChanged();

                        if (farms.isEmpty()) {
                            showEmptyState();
                        } else {
                            emptyText.setVisibility(View.GONE);
                            farmsRecycler.setVisibility(View.VISIBLE);
                        }
                    })
                    .addOnFailureListener(e -> {
                        progressBar.setVisibility(View.GONE);
                        showEmptyState();
                        showToast("Unable to load farms. Check network.");
                    });
        } catch (Exception e) {
            progressBar.setVisibility(View.GONE);
            showEmptyState();
            showToast("Something went wrong while loading farms.");
        }
    }

    private void showEmptyState() {
        farmsRecycler.setVisibility(View.GONE);
        emptyText.setVisibility(View.VISIBLE);
        emptyText.setText("No farms added yet");
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
