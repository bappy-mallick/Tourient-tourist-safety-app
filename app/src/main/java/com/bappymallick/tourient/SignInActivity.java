package com.bappymallick.tourient;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class SignInActivity extends AppCompatActivity {

    private EditText usernameEdit;
    private EditText passwordEdit;
    private Button loginButton;
    private TextView forgotPassword;

    private DatabaseReference usersRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signin);

        usernameEdit = findViewById(R.id.usernameEdit);
        passwordEdit = findViewById(R.id.passwordEdit);
        loginButton = findViewById(R.id.loginBtn);
        forgotPassword = findViewById(R.id.forgotPassword);

        usersRef = FirebaseDatabase
                .getInstance("https://tourient-bappymallick-default-rtdb.asia-southeast1.firebasedatabase.app")
                .getReference("users");

        loginButton.setOnClickListener(v -> {
            String enteredUsername = usernameEdit.getText().toString().trim();
            String enteredPassword = passwordEdit.getText().toString();

            if (enteredUsername.isEmpty()) {
                usernameEdit.setError("Please enter username");
                usernameEdit.requestFocus();
                return;
            }
            if (enteredPassword.isEmpty()) {
                passwordEdit.setError("Please enter password");
                passwordEdit.requestFocus();
                return;
            }

            usersRef.orderByChild("username").equalTo(enteredUsername)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            if (snapshot.exists()) {
                                boolean passwordMatched = false;
                                String documentId = null;
                                for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                                    String dbPassword = userSnapshot.child("password").getValue(String.class);
                                    if (dbPassword != null && dbPassword.equals(enteredPassword)) {
                                        passwordMatched = true;
                                        documentId = userSnapshot.getKey();  // Save documentId
                                        break;
                                    }
                                }
                                if (passwordMatched) {
                                    Toast.makeText(SignInActivity.this, "Login successful", Toast.LENGTH_SHORT).show();

                                    SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
                                    SharedPreferences.Editor editor = prefs.edit();
                                    editor.putString("documentId", documentId);
                                    editor.putString("username", enteredUsername);
                                    editor.putBoolean("isSignedIn", true); // <-- Set this flag on successful login
                                    editor.apply();

                                    Intent intent = new Intent(SignInActivity.this, HomePageActivity.class);
                                    startActivity(intent);
                                    finish();
                                } else {
                                    Toast.makeText(SignInActivity.this, "Incorrect password", Toast.LENGTH_SHORT).show();
                                }
                            } else {
                                Toast.makeText(SignInActivity.this, "Username not found", Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            Toast.makeText(SignInActivity.this, "Database error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        });

        forgotPassword.setOnClickListener(v -> {
            Intent intent = new Intent(SignInActivity.this, RegisterActivity.class);
            startActivity(intent);
        });
    }
}
