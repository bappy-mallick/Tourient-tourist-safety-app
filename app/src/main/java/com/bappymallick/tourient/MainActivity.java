package com.bappymallick.tourient;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
        boolean isSignedIn = prefs.getBoolean("isSignedIn", false);

        if (isSignedIn) {
            // User already signed in, skip login/register screen
            startActivity(new Intent(MainActivity.this, HomePageActivity.class));
            finish();
            return; // Prevents further execution
        }

        // Else show login/register buttons
        setContentView(R.layout.activity_main);

        Button registerBtn = findViewById(R.id.registerBtn);
        Button signInBtn = findViewById(R.id.signInBtn);

        registerBtn.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, RegisterActivity.class);
            startActivity(intent);
        });

        signInBtn.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, SignInActivity.class);
            startActivity(intent);
        });
    }
}
