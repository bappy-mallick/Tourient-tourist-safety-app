package com.bappymallick.tourient;

import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class HighRiskActivity extends AppCompatActivity {

    private LinearLayout weatherAlertsSection, crimeAlertsSection, healthAlertsSection, trafficAlertsSection;
    private Button btnBackToHome;
    private ImageView backArrow;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_high_risk);

        initializeViews();
        setupClickListeners();
    }

    private void initializeViews() {
        weatherAlertsSection = findViewById(R.id.weather_alerts_section);
        crimeAlertsSection = findViewById(R.id.crime_alerts_section);
        healthAlertsSection = findViewById(R.id.health_alerts_section);
        trafficAlertsSection = findViewById(R.id.traffic_alerts_section);
        btnBackToHome = findViewById(R.id.btn_back_to_home);
        backArrow = findViewById(R.id.back_arrow);
    }

    private void setupClickListeners() {
        weatherAlertsSection.setOnClickListener(v -> {
            Toast.makeText(this, "Checking weather alerts...", Toast.LENGTH_SHORT).show();
            // TODO: Implement weather alerts functionality
        });

        crimeAlertsSection.setOnClickListener(v -> {
            Toast.makeText(this, "Checking crime alerts in your area...", Toast.LENGTH_SHORT).show();
            // TODO: Implement crime alerts functionality
        });

        healthAlertsSection.setOnClickListener(v -> {
            Toast.makeText(this, "Checking health alerts...", Toast.LENGTH_SHORT).show();
            // TODO: Implement health alerts functionality
        });

        trafficAlertsSection.setOnClickListener(v -> {
            Toast.makeText(this, "Checking traffic alerts...", Toast.LENGTH_SHORT).show();
            // TODO: Implement traffic alerts functionality
        });

        btnBackToHome.setOnClickListener(v -> finish());

        backArrow.setOnClickListener(v -> finish());
    }
}