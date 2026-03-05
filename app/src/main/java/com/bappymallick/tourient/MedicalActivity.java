package com.bappymallick.tourient;

import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class MedicalActivity extends AppCompatActivity {

    private LinearLayout hospitalSection, pharmacySection, ambulanceSection, doctorSection;
    private Button btnBackToHome;
    private ImageView backArrow;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_medical);

        initializeViews();
        setupClickListeners();
    }

    private void initializeViews() {
        hospitalSection = findViewById(R.id.hospital_section);
        pharmacySection = findViewById(R.id.pharmacy_section);
        ambulanceSection = findViewById(R.id.ambulance_section);
        doctorSection = findViewById(R.id.doctor_section);
        btnBackToHome = findViewById(R.id.btn_back_to_home);
        backArrow = findViewById(R.id.back_arrow);
    }

    private void setupClickListeners() {
        hospitalSection.setOnClickListener(v -> {
            Toast.makeText(this, "Finding nearby hospitals...", Toast.LENGTH_SHORT).show();
            // TODO: Implement hospital search functionality
        });

        pharmacySection.setOnClickListener(v -> {
            Toast.makeText(this, "Finding nearby pharmacies...", Toast.LENGTH_SHORT).show();
            // TODO: Implement pharmacy search functionality
        });

        ambulanceSection.setOnClickListener(v -> {
            Toast.makeText(this, "Calling emergency ambulance service...", Toast.LENGTH_SHORT).show();
            // TODO: Implement ambulance calling functionality
        });

        doctorSection.setOnClickListener(v -> {
            Toast.makeText(this, "Finding nearby doctors...", Toast.LENGTH_SHORT).show();
            // TODO: Implement doctor search functionality
        });

        btnBackToHome.setOnClickListener(v -> finish());

        backArrow.setOnClickListener(v -> finish());
    }
}