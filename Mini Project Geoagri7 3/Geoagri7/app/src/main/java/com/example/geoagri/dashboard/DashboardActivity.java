package com.example.geoagri.dashboard;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.activity.OnBackPressedCallback;
import androidx.fragment.app.Fragment;

import com.example.geoagri.R;
import com.example.geoagri.chat.ChatFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class DashboardActivity extends AppCompatActivity {
    private BottomNavigationView bottomNavigation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        bottomNavigation = findViewById(R.id.bottomNavigation);

        if (savedInstanceState == null) {
            replaceFragment(new HomeFragment());
        }

        bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.homeFragment) {
                replaceFragment(new HomeFragment());
                return true;
            }
            if (itemId == R.id.satelliteFragment) {
                replaceFragment(new SatelliteFragment());
                return true;
            }
            if (itemId == R.id.chatFragment) {
                replaceFragment(new ChatFragment());
                return true;
            }
            if (itemId == R.id.profileFragment) {
                replaceFragment(new ProfileFragment());
                return true;
            }
            return false;
        });

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (bottomNavigation.getSelectedItemId() == R.id.profileFragment) {
                    Toast.makeText(DashboardActivity.this, "Use bottom navigation.", Toast.LENGTH_SHORT).show();
                    return;
                }
                finish();
            }
        });
    }

    private void replaceFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }
}
