package com.bappymallick.tourient;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import android.content.Intent;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
public class EnterIdPassActivity extends AppCompatActivity {
    private EditText usernameEdit;
    private EditText passwordEdit;
    private EditText confirmPasswordEdit;
    private Button enterBtn;
    private FirebaseDatabase database;
    private DatabaseReference usersRef;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_enter_id_pass);
        // Initialize Firebase with your specific database URL
        database = FirebaseDatabase
                .getInstance("https://tourient-bappymallick-default-rtdb.asia-southeast1.firebasedatabase.app");
        usersRef = database.getReference("users");
        usernameEdit = findViewById(R.id.usernameEdit);
        passwordEdit = findViewById(R.id.passwordEdit);
        confirmPasswordEdit = findViewById(R.id.confirmPasswordEdit);
        enterBtn = findViewById(R.id.enterBtn);
        enterBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String username = usernameEdit.getText().toString().trim();
                String password = passwordEdit.getText().toString();
                String confirmPassword = confirmPasswordEdit.getText().toString();
                if (username.isEmpty()) {
                    usernameEdit.setError("Please enter username");
                    usernameEdit.requestFocus();
                    return;
                }
                if (password.isEmpty()) {
                    passwordEdit.setError("Please enter password");
                    passwordEdit.requestFocus();
                    return;
                }
                if (!password.equals(confirmPassword)) {
                    confirmPasswordEdit.setError("Passwords do not match");
                    confirmPasswordEdit.requestFocus();
                    return;
                }
                // Check if the username already exists
                usersRef.orderByChild("username").equalTo(username)
                        .addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                if (snapshot.exists()) {
                                    usernameEdit.setError("Username already taken");
                                    usernameEdit.requestFocus();
                                } else {
                                    User user = new User(username, password);
                                    String key = usersRef.push().getKey();
                                    if (key != null) {
                                        usersRef.child(key).setValue(user).addOnCompleteListener(task -> {
                                            if (task.isSuccessful()) {
                                                Toast.makeText(EnterIdPassActivity.this, "User saved in database", Toast.LENGTH_SHORT).show();
                                                Intent intent = new Intent(EnterIdPassActivity.this, SignInActivity.class);
                                                startActivity(intent);
                                                finish();
                                            } else {
                                                Toast.makeText(EnterIdPassActivity.this, "Failed to save user", Toast.LENGTH_SHORT).show();
                                            }
                                        });
                                    } else {
                                        Toast.makeText(EnterIdPassActivity.this, "Error generating database key", Toast.LENGTH_SHORT).show();
                                    }
                                }
                            }
                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {
                                Toast.makeText(EnterIdPassActivity.this, "Database error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });
            }
        });
    }
    // User model class to hold username and password
    public static class User {
        public String username;
        public String password;
        // Default constructor required for Firebase
        public User() {
        }
        public User(String username, String password) {
            this.username = username;
            this.password = password;
        }
    }
}