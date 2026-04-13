package com.example.notifly_system;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

public class LoginActivity extends AppCompatActivity {

    EditText etUsernameOrEmail, etPassword;
    Button btnLogin;
    TextView tvForgotPassword;
    FirebaseAuth mAuth;
    DatabaseReference database;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login_activity);

        // ── INITIALIZE VIEWS ──────────────────────────────────────

        etUsernameOrEmail = findViewById(R.id.etUsernameOrEmail);
        etPassword        = findViewById(R.id.etPassword);
        btnLogin          = findViewById(R.id.btnLogin);
        tvForgotPassword  = findViewById(R.id.tvForgotPassword);

        // ── FIREBASE ──────────────────────────────────────────────

        mAuth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance("https://notifly-94dba-default-rtdb.asia-southeast1.firebasedatabase.app/")
                .getReference("users");

        // ── LISTENERS ─────────────────────────────────────────────

        btnLogin.setOnClickListener(v -> loginUser());
        tvForgotPassword.setOnClickListener(v -> showForgotPasswordDialog());
    }

    private void loginUser() {
        String usernameOrEmail = etUsernameOrEmail.getText().toString().trim();
        String password        = etPassword.getText().toString().trim();

        // ── VALIDATIONS ───────────────────────────────────────────

        if (usernameOrEmail.isEmpty()) {
            etUsernameOrEmail.setError("Username or Email is required");
            etUsernameOrEmail.requestFocus();
            return;
        }
        if (password.isEmpty()) {
            etPassword.setError("Password is required");
            etPassword.requestFocus();
            return;
        }

        // ── CHECK IF INPUT IS EMAIL OR USERNAME ───────────────────

        if (android.util.Patterns.EMAIL_ADDRESS.matcher(usernameOrEmail).matches()) {
            loginWithEmail(usernameOrEmail, password);
        } else {
            loginWithUsername(usernameOrEmail, password);
        }
    }

    private void loginWithEmail(String email, String password) {
        mAuth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Toast.makeText(this, "Login successful!", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(this, MainActivity.class));
                    finish();
                } else {
                    Toast.makeText(this, "Invalid email or password", Toast.LENGTH_SHORT).show();
                }
            });
    }

    private void loginWithUsername(String username, String password) {
        Query query = database.orderByChild("username").equalTo(username);
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                        String email = userSnapshot.child("email").getValue(String.class);
                        if (email != null) {
                            loginWithEmail(email, password);
                        }
                    }
                } else {
                    etUsernameOrEmail.setError("Username not found");
                    etUsernameOrEmail.requestFocus();
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(LoginActivity.this, "Something went wrong. Try again.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ─────────────────────────────────────────────────────────────
    // Forgot password — prompt for email first, then confirm
    // ─────────────────────────────────────────────────────────────
    private void showForgotPasswordDialog() {
        String input = etUsernameOrEmail.getText().toString().trim();

        // If the field already has a valid email, skip the prompt
        if (android.util.Patterns.EMAIL_ADDRESS.matcher(input).matches()) {
            showConfirmResetDialog(input);
            return;
        }

        // Otherwise ask the user to enter their email
        final EditText emailInput = new EditText(this);
        emailInput.setHint("Enter your email address");
        emailInput.setInputType(android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        emailInput.setPadding(48, 24, 48, 24);

        new AlertDialog.Builder(this)
            .setTitle("Forgot Password?")
            .setMessage("Enter the email linked to your account.")
            .setView(emailInput)
            .setPositiveButton("Continue", (dialog, which) -> {
                String email = emailInput.getText().toString().trim();
                if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    Toast.makeText(this, "Please enter a valid email address.", Toast.LENGTH_SHORT).show();
                } else {
                    showConfirmResetDialog(email);
                }
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void showConfirmResetDialog(String email) {
        new AlertDialog.Builder(this)
            .setTitle("Forgot Password?")
            .setMessage("A password reset link will be sent to:\n\n" + email + "\n\nDo you want to continue?")
            .setPositiveButton("Send Reset Link", (dialog, which) -> sendPasswordResetEmail(email))
            .setNegativeButton("Cancel", null)
            .show();
    }

    // ─────────────────────────────────────────────────────────────
    // Step 1 — Firebase sends reset link email
    // Step 2 — Fetch username from Realtime Database
    // Step 3 — EmailJS sends branded notification email
    // ─────────────────────────────────────────────────────────────
    private void sendPasswordResetEmail(String email) {
        mAuth.sendPasswordResetEmail(email)
            .addOnSuccessListener(unused -> {

                // Step 2: Try to find the user's UID by email to fetch their username
                // Since the user is not logged in, query by email field in the database
                database.orderByChild("email").equalTo(email)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot snapshot) {
                            String username = "User";
                            if (snapshot.exists()) {
                                for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                                    String name = userSnapshot.child("username").getValue(String.class);
                                    if (name != null && !name.isEmpty()) {
                                        username = name;
                                    }
                                    break;
                                }
                            }

                            // Step 3: EmailJS sends branded notification
                            EmailHelper.sendPasswordResetEmail(LoginActivity.this, username, email);

                            // Show confirmation
                            new AlertDialog.Builder(LoginActivity.this)
                                .setTitle("Email Sent!")
                                .setMessage("A password reset link has been sent to:\n\n"
                                        + email
                                        + "\n\nCheck your inbox and follow the instructions.")
                                .setPositiveButton("OK", null)
                                .show();
                        }

                        @Override
                        public void onCancelled(DatabaseError error) {
                            // Fallback — still send EmailJS with generic name
                            EmailHelper.sendPasswordResetEmail(LoginActivity.this, "User", email);
                            Toast.makeText(LoginActivity.this,
                                    "Reset email sent. Check your inbox.",
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
            })
            .addOnFailureListener(e -> {
                Toast.makeText(this,
                        "Failed to send reset email. Please try again.",
                        Toast.LENGTH_SHORT).show();
            });
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (mAuth.getCurrentUser() != null) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        }
    }
}
