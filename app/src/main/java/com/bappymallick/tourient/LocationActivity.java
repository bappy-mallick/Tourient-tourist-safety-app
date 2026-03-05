package com.bappymallick.tourient;

import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class LocationActivity extends AppCompatActivity {

    private LinearLayout currentLocationSection, nearbyPlacesSection, navigationSection, shareLocationSection;
    private Button btnBackToHome;
    private ImageView backArrow;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location);

        initializeViews();
        setupClickListeners();
    }

    private void initializeViews() {
        currentLocationSection = findViewById(R.id.current_location_section);
        nearbyPlacesSection = findViewById(R.id.nearby_places_section);
        navigationSection = findViewById(R.id.navigation_section);
        shareLocationSection = findViewById(R.id.share_location_section);
        btnBackToHome = findViewById(R.id.btn_back_to_home);
        backArrow = findViewById(R.id.back_arrow);
    }

    private void setupClickListeners() {
        currentLocationSection.setOnClickListener(v -> {
            Toast.makeText(this, "Getting your current location...", Toast.LENGTH_SHORT).show();
            // TODO: Implement current location functionality
        });

        nearbyPlacesSection.setOnClickListener(v -> {
            Toast.makeText(this, "Finding nearby places...", Toast.LENGTH_SHORT).show();
            // TODO: Implement nearby places functionality
        });

        navigationSection.setOnClickListener(v -> {
            Toast.makeText(this, "Opening navigation...", Toast.LENGTH_SHORT).show();
            // TODO: Implement navigation functionality
        });

        shareLocationSection.setOnClickListener(v -> {
            Toast.makeText(this, "Sharing your location...", Toast.LENGTH_SHORT).show();
            // TODO: Implement share location functionality
        });

        btnBackToHome.setOnClickListener(v -> finish());

        backArrow.setOnClickListener(v -> finish());
    }
}