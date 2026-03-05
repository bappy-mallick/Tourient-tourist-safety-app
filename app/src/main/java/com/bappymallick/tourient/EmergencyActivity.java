package com.bappymallick.tourient;

import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class EmergencyActivity extends AppCompatActivity {

    private LinearLayout policeSection, fireSection, ambulanceSection, emergencyContactsSection;
    private Button btnBackToHome;
    private ImageView backArrow;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_emergency);

        initializeViews();
        setupClickListeners();
    }

    private void initializeViews() {
        policeSection = findViewById(R.id.police_section);
        fireSection = findViewById(R.id.fire_section);
        ambulanceSection = findViewById(R.id.ambulance_section);
        emergencyContactsSection = findViewById(R.id.emergency_contacts_section);
        btnBackToHome = findViewById(R.id.btn_back_to_home);
        backArrow = findViewById(R.id.back_arrow);
    }

    private void setupClickListeners() {
        policeSection.setOnClickListener(v -> {
            Toast.makeText(this, "Calling Police Emergency - 100", Toast.LENGTH_SHORT).show();
            // TODO: Implement police emergency call functionality
        });

        fireSection.setOnClickListener(v -> {
            Toast.makeText(this, "Calling Fire Emergency - 101", Toast.LENGTH_SHORT).show();
            // TODO: Implement fire emergency call functionality
        });

        ambulanceSection.setOnClickListener(v -> {
            Toast.makeText(this, "Calling Ambulance Emergency - 108", Toast.LENGTH_SHORT).show();
            // TODO: Implement ambulance emergency call functionality
        });

        emergencyContactsSection.setOnClickListener(v -> {
            Toast.makeText(this, "Opening emergency contacts...", Toast.LENGTH_SHORT).show();
            // TODO: Implement emergency contacts functionality
        });

        btnBackToHome.setOnClickListener(v -> finish());

        backArrow.setOnClickListener(v -> finish());
    }
}