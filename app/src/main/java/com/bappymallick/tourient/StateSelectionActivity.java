package com.bappymallick.tourient;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
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

public class StateSelectionActivity extends AppCompatActivity {

    private RadioGroup stateRadioGroup;
    private EditText aadharEdit;
    private Button nextBtn;
    private DatabaseReference databaseReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_state_selection);

        stateRadioGroup = findViewById(R.id.stateRadioGroup);
        aadharEdit = findViewById(R.id.aadhaarEdit);  // Check your layout for correct id
        nextBtn = findViewById(R.id.nextBtn);

        // Initialize Firebase Realtime Database reference
        databaseReference = FirebaseDatabase
                .getInstance("https://tourient-bappymallick-default-rtdb.asia-southeast1.firebasedatabase.app")
                .getReference("registeredUsers");


        nextBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int selectedStateId = stateRadioGroup.getCheckedRadioButtonId();
                String aadharNumber = aadharEdit.getText().toString().trim();

                if (selectedStateId == -1) {
                    Toast.makeText(StateSelectionActivity.this, "Please select a state", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (aadharNumber.isEmpty()) {
                    aadharEdit.setError("Please enter your Aadhaar number");
                    aadharEdit.requestFocus();
                    return;
                }

                // Get selected state name from the selected radio button
                RadioButton selectedRadioButton = findViewById(selectedStateId);
                String stateName = selectedRadioButton.getText().toString();

                // Validate Aadhaar existence in Firebase
                validateAadhaarInFirebase(stateName, aadharNumber);
            }
        });
    }

    private void validateAadhaarInFirebase(String stateName, String aadharNumber) {
        DatabaseReference stateRef = databaseReference.child(stateName).child(aadharNumber);

        stateRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    // Aadhaar exists - proceed
                    Toast.makeText(StateSelectionActivity.this, "Verification successful", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(StateSelectionActivity.this, EnterIdPassActivity.class);
                    startActivity(intent);
                } else {
                    aadharEdit.setError("Invalid Aadhaar number, please try again.");
                    aadharEdit.requestFocus();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(StateSelectionActivity.this, "Database error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
