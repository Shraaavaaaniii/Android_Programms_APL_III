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
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.List;

public class NotificationsActivity extends AppCompatActivity {

    private final List<AppNotification> notifications = new ArrayList<>();
    private NotificationAdapter adapter;

    private RecyclerView notificationsRecycler;
    private ProgressBar progressBar;
    private TextView emptyText;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);

        notificationsRecycler = findViewById(R.id.recyclerNotifications);
        progressBar = findViewById(R.id.progressNotifications);
        emptyText = findViewById(R.id.textEmptyNotifications);

        adapter = new NotificationAdapter(notifications);
        notificationsRecycler.setLayoutManager(new LinearLayoutManager(this));
        notificationsRecycler.setAdapter(adapter);

        fetchLatestNotifications();
        markAllAsRead();
    }

    private void fetchLatestNotifications() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            showEmptyState("No data available");
            showToast("Please login again.");
            return;
        }

        try {
            progressBar.setVisibility(View.VISIBLE);
            FirebaseFirestore.getInstance()
                    .collection("notifications")
                    .whereEqualTo("userId", user.getUid())
                    .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                    .limit(3)
                    .get()
                    .addOnSuccessListener(querySnapshot -> {
                        progressBar.setVisibility(View.GONE);
                        notifications.clear();
                        notifications.addAll(querySnapshot.toObjects(AppNotification.class));
                        adapter.notifyDataSetChanged();

                        if (notifications.isEmpty()) {
                            showEmptyState("No data available");
                        } else {
                            emptyText.setVisibility(View.GONE);
                            notificationsRecycler.setVisibility(View.VISIBLE);
                        }
                    })
                    .addOnFailureListener(e -> {
                        progressBar.setVisibility(View.GONE);
                        showEmptyState("No data available");
                        showToast("Unable to load notifications. Check network.");
                    });
        } catch (Exception e) {
            progressBar.setVisibility(View.GONE);
            showEmptyState("No data available");
            showToast("Something went wrong while loading notifications.");
        }
    }

    private void markAllAsRead() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            return;
        }

        try {
            FirebaseFirestore.getInstance()
                    .collection("notifications")
                    .whereEqualTo("userId", user.getUid())
                    .whereEqualTo("read", false)
                    .get()
                    .addOnSuccessListener(querySnapshot -> {
                        if (querySnapshot.isEmpty()) {
                            return;
                        }

                        WriteBatch batch = FirebaseFirestore.getInstance().batch();
                        for (QueryDocumentSnapshot doc : querySnapshot) {
                            batch.update(doc.getReference(), "read", true);
                        }
                        batch.commit();
                    });
        } catch (Exception e) {
            showToast("Unable to update notification status.");
        }
    }

    private void showEmptyState(String text) {
        notificationsRecycler.setVisibility(View.GONE);
        emptyText.setVisibility(View.VISIBLE);
        emptyText.setText(text);
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
