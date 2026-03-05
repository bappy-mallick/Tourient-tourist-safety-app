package com.bappymallick.tourient;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

public class LauncherActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
        boolean isSignedIn = prefs.getBoolean("isSignedIn", false);

        if (isSignedIn) {
            // User is signed in, navigate to HomePageActivity
            startActivity(new Intent(this, HomePageActivity.class));
        } else {
            // User not signed in, navigate to RegistrationActivity
            startActivity(new Intent(this, MainActivity.class));
        }

        // Close this launcher activity so user cannot navigate back here
        finish();
    }
}
