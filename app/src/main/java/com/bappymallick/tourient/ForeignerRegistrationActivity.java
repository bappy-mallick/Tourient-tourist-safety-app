package com.bappymallick.tourient;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class ForeignerRegistrationActivity extends AppCompatActivity {

    private RadioGroup countryRadioGroup;
    private EditText passportEdit;
    private Button nextBtn;
    private DatabaseReference databaseReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_foreigner_registration);

        countryRadioGroup = findViewById(R.id.countryRadioGroup);
        passportEdit = findViewById(R.id.passportEdit);
        nextBtn = findViewById(R.id.nextBtn);

        // Initialize Firebase database reference (assume 'foreigners' node contains countries and passport data)
        databaseReference = FirebaseDatabase
                .getInstance("https://tourient-bappymallick-default-rtdb.asia-southeast1.firebasedatabase.app")
                .getReference("foreigners");

        nextBtn.setOnClickListener(v -> {
            int selectedCountryId = countryRadioGroup.getCheckedRadioButtonId();
            String passportNumber = passportEdit.getText().toString().trim();

            if (selectedCountryId == -1) {
                Toast.makeText(ForeignerRegistrationActivity.this, "Please select your country", Toast.LENGTH_SHORT).show();
                return;
            }

            if (passportNumber.isEmpty()) {
                passportEdit.setError("Please enter your passport number");
                passportEdit.requestFocus();
                return;
            }

            RadioButton selectedRadioButton = findViewById(selectedCountryId);
            String countryName = selectedRadioButton.getText().toString().trim();

            validatePassportInFirebase(countryName, passportNumber);
        });
    }

    private void validatePassportInFirebase(String countryName, String passportNumber) {
        DatabaseReference countryRef = databaseReference.child(countryName).child(passportNumber);

        countryRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Toast.makeText(ForeignerRegistrationActivity.this, "Verification successful", Toast.LENGTH_SHORT).show();
                    // Proceed to next activity (update with your actual next activity)
                    Intent intent = new Intent(ForeignerRegistrationActivity.this, EnterIdPassActivity.class);
                    startActivity(intent);
                } else {
                    passportEdit.setError("Invalid passport number, please try again.");
                    passportEdit.requestFocus();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ForeignerRegistrationActivity.this, "Database error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
