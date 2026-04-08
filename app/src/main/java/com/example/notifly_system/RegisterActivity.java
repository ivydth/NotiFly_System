package com.example.notifly_system;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import java.util.HashMap;

public class RegisterActivity extends AppCompatActivity {

    EditText etLastName, etFirstName, etEmail, etPassword, etConfirmPassword;
    CheckBox cbTerms;
    Button btnRegister;
    FirebaseAuth mAuth;
    DatabaseReference database;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.register_activity);

        // initialize views
        etLastName = findViewById(R.id.etLastName);
        etFirstName = findViewById(R.id.etFirstName);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        cbTerms = findViewById(R.id.cbTerms);
        btnRegister = findViewById(R.id.btnRegister);

        // initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance().getReference("users");

        btnRegister.setOnClickListener(v -> registerUser());
    }

    private void registerUser() {
        String lastName = etLastName.getText().toString().trim();
        String firstName = etFirstName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();

        // ─── NAME VALIDATIONS ───────────────────────────────────────

        // last name
        if (lastName.isEmpty()) {
            etLastName.setError("Last name is required");
            etLastName.requestFocus();
            return;
        }
        if (lastName.length() < 2) {
            etLastName.setError("Last name must be at least 2 characters");
            etLastName.requestFocus();
            return;
        }
        if (!lastName.matches("[a-zA-Z ]+")) {
            etLastName.setError("Last name must not contain numbers or special characters");
            etLastName.requestFocus();
            return;
        }

        // first name
        if (firstName.isEmpty()) {
            etFirstName.setError("First name is required");
            etFirstName.requestFocus();
            return;
        }
        if (firstName.length() < 2) {
            etFirstName.setError("First name must be at least 2 characters");
            etFirstName.requestFocus();
            return;
        }
        if (!firstName.matches("[a-zA-Z ]+")) {
            etFirstName.setError("First name must not contain numbers or special characters");
            etFirstName.requestFocus();
            return;
        }

        // ─── EMAIL VALIDATION ───────────────────────────────────────

        if (email.isEmpty()) {
            etEmail.setError("Email is required");
            etEmail.requestFocus();
            return;
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Please enter a valid email address");
            etEmail.requestFocus();
            return;
        }

        // ─── PASSWORD VALIDATIONS ────────────────────────────────────

        if (password.isEmpty()) {
            etPassword.setError("Password is required");
            etPassword.requestFocus();
            return;
        }
        if (password.length() < 8) {
            etPassword.setError("Password must be at least 8 characters");
            etPassword.requestFocus();
            return;
        }
        if (!password.matches(".*[A-Z].*")) {
            etPassword.setError("Password must contain at least 1 capital letter");
            etPassword.requestFocus();
            return;
        }
        if (!password.matches(".*[0-9].*")) {
            etPassword.setError("Password must contain at least 1 number");
            etPassword.requestFocus();
            return;
        }
        if (!password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{}|;':\",./<>?].*")) {
            etPassword.setError("Password must contain at least 1 special character");
            etPassword.requestFocus();
            return;
        }

        // ─── CONFIRM PASSWORD ────────────────────────────────────────

        if (confirmPassword.isEmpty()) {
            etConfirmPassword.setError("Please confirm your password");
            etConfirmPassword.requestFocus();
            return;
        }
        if (!password.equals(confirmPassword)) {
            etConfirmPassword.setError("Passwords do not match");
            etConfirmPassword.requestFocus();
            return;
        }

        // ─── TERMS & CONDITIONS ──────────────────────────────────────

        if (!cbTerms.isChecked()) {
            Toast.makeText(this, "Please agree to Terms & Conditions", Toast.LENGTH_SHORT).show();
            return;
        }

        // ─── FIREBASE REGISTER ───────────────────────────────────────

        mAuth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    String userId = mAuth.getCurrentUser().getUid();

                    HashMap<String, Object> userMap = new HashMap<>();
                    userMap.put("lastName", lastName);
                    userMap.put("firstName", firstName);
                    userMap.put("email", email);
                    userMap.put("role", "user");

                    database.child(userId).setValue(userMap)
                        .addOnCompleteListener(task2 -> {
                            if (task2.isSuccessful()) {
                                Toast.makeText(this, "Account created successfully!", Toast.LENGTH_SHORT).show();
                                startActivity(new Intent(this, LoginActivity.class));
                                finish();
                            } else {
                                Toast.makeText(this, "Failed to save user data", Toast.LENGTH_SHORT).show();
                            }
                        });
                } else {
                    Toast.makeText(this, task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
    }
}
