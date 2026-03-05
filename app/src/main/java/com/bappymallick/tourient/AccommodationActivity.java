package com.bappymallick.tourient;

import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class AccommodationActivity extends AppCompatActivity {

    private LinearLayout hotelSection, guestHouseSection, resortSection;
    private Button btnBackToHome;
    private ImageView backArrow;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_accommodation);

        initializeViews();
        setupClickListeners();
    }

    private void initializeViews() {
//        hotelSection = findViewById(R.id.hotel_section);
//        guestHouseSection = findViewById(R.id.guest_house_section);
//        resortSection = findViewById(R.id.resort_section);
        btnBackToHome = findViewById(R.id.btn_back_to_home);
        backArrow = findViewById(R.id.back_arrow);
    }

    private void setupClickListeners() {
        hotelSection.setOnClickListener(v -> {
            Toast.makeText(this, "Searching for Hotels...", Toast.LENGTH_SHORT).show();
            // TODO: Implement hotel search functionality
        });

        guestHouseSection.setOnClickListener(v -> {
            Toast.makeText(this, "Searching for Guest Houses...", Toast.LENGTH_SHORT).show();
            // TODO: Implement guest house search functionality
        });

        resortSection.setOnClickListener(v -> {
            Toast.makeText(this, "Searching for Resorts...", Toast.LENGTH_SHORT).show();
            // TODO: Implement resort search functionality
        });

        btnBackToHome.setOnClickListener(v -> {
            finish(); // Go back to previous activity
        });

        backArrow.setOnClickListener(v -> {
            finish(); // Go back to previous activity
        });
    }
}