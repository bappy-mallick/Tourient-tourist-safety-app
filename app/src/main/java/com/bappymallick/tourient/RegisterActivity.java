package com.bappymallick.tourient;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class RegisterActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        Button indianBtn = findViewById(R.id.indianBtn);
        Button foreignerBtn = findViewById(R.id.foreignerBtn);

        indianBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(RegisterActivity.this, StateSelectionActivity.class);
                startActivity(intent);
            }
        });

        foreignerBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Example: Navigate to a foreigner registration screen
                Intent intent = new Intent(RegisterActivity.this, ForeignerRegistrationActivity.class);
                startActivity(intent);
            }
        });
    }
}
