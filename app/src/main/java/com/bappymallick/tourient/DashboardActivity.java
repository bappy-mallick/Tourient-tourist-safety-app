package com.bappymallick.tourient;

import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class DashboardActivity extends AppCompatActivity {

    private LinearLayout profileSection, tripsSection, alertsSection, settingsSection;
    private Button btnBackToHome;
    private ImageView backArrow;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        initializeViews();
        setupClickListeners();
    }

    private void initializeViews() {
        profileSection = findViewById(R.id.profile_section);
        tripsSection = findViewById(R.id.trips_section);
        alertsSection = findViewById(R.id.alerts_section);
        settingsSection = findViewById(R.id.settings_section);
        btnBackToHome = findViewById(R.id.btn_back_to_home);
        backArrow = findViewById(R.id.back_arrow);
    }

    private void setupClickListeners() {
        profileSection.setOnClickListener(v -> {
            Toast.makeText(this, "Opening profile...", Toast.LENGTH_SHORT).show();
            // TODO: Implement profile functionality
        });

        tripsSection.setOnClickListener(v -> {
            Toast.makeText(this, "Viewing your trips...", Toast.LENGTH_SHORT).show();
            // TODO: Implement trips functionality
        });

        alertsSection.setOnClickListener(v -> {
            Toast.makeText(this, "Viewing alerts...", Toast.LENGTH_SHORT).show();
            // TODO: Implement alerts functionality
        });

        settingsSection.setOnClickListener(v -> {
            Toast.makeText(this, "Opening settings...", Toast.LENGTH_SHORT).show();
            // TODO: Implement settings functionality
        });

        btnBackToHome.setOnClickListener(v -> finish());

        backArrow.setOnClickListener(v -> finish());
    }
}