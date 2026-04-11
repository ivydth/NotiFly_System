package com.example.notifly_system;

import android.os.Bundle;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONObject;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.regex.Pattern;

public class ChangePass extends AppCompatActivity {

    // Back
    CardView btnBack;

    // Fields
    EditText etCurrentPassword, etNewPassword, etConfirmPassword;

    // Eye toggles
    ImageView ivToggleCurrentPassword, ivToggleNewPassword, ivToggleConfirmPassword;

    // Save button
    View btnChangePassword;

    // Forgot password link
    TextView tvForgotPassword;

    // Firebase
    FirebaseAuth     mAuth;
    DatabaseReference mDatabase;

    // Toggle states
    boolean isCurrentVisible = false;
    boolean isNewVisible     = false;
    boolean isConfirmVisible = false;

    // ── PASSWORD RULES ────────────────────────────────────────────
    private static final int     MIN_LENGTH    = 8;
    private static final Pattern HAS_UPPERCASE = Pattern.compile("[A-Z]");
    private static final Pattern HAS_DIGIT     = Pattern.compile("[0-9]");
    private static final Pattern HAS_SPECIAL   = Pattern.compile("[^a-zA-Z0-9]");

    // ── EMAILJS CREDENTIALS ───────────────────────────────────────
    private static final String EMAILJS_SERVICE_ID  = "service_i8crmql";
    private static final String EMAILJS_TEMPLATE_ID = "template_0wzz2sp";
    private static final String EMAILJS_PUBLIC_KEY  = "juGO7uo9O6udgw2xl";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change);

        // ── INITIALIZE VIEWS ──────────────────────────────────────

        btnBack                 = findViewById(R.id.btnBack);
        etCurrentPassword       = findViewById(R.id.etCurrentPassword);
        etNewPassword           = findViewById(R.id.etNewPassword);
        etConfirmPassword       = findViewById(R.id.etConfirmPassword);
        ivToggleCurrentPassword = findViewById(R.id.ivToggleCurrentPassword);
        ivToggleNewPassword     = findViewById(R.id.ivToggleNewPassword);
        ivToggleConfirmPassword = findViewById(R.id.ivToggleConfirmPassword);
        btnChangePassword       = findViewById(R.id.btnChangePassword);
        tvForgotPassword        = findViewById(R.id.tvForgotPassword);

        // ── FIREBASE ──────────────────────────────────────────────

        mAuth     = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        // ── LISTENERS ─────────────────────────────────────────────

        btnBack.setOnClickListener(v -> finish());

        ivToggleCurrentPassword.setOnClickListener(v -> {
            isCurrentVisible = !isCurrentVisible;
            togglePasswordVisibility(etCurrentPassword, ivToggleCurrentPassword, isCurrentVisible);
        });

        ivToggleNewPassword.setOnClickListener(v -> {
            isNewVisible = !isNewVisible;
            togglePasswordVisibility(etNewPassword, ivToggleNewPassword, isNewVisible);
        });

        ivToggleConfirmPassword.setOnClickListener(v -> {
            isConfirmVisible = !isConfirmVisible;
            togglePasswordVisibility(etConfirmPassword, ivToggleConfirmPassword, isConfirmVisible);
        });

        btnChangePassword.setOnClickListener(v -> changePassword());
        tvForgotPassword.setOnClickListener(v -> showForgotPasswordDialog());
    }

    // ─────────────────────────────────────────────────────────────
    // Toggle password visibility
    // ─────────────────────────────────────────────────────────────
    private void togglePasswordVisibility(EditText field, ImageView icon, boolean isVisible) {
        if (isVisible) {
            field.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
            icon.setImageResource(R.drawable.ic_eye_on);
        } else {
            field.setTransformationMethod(PasswordTransformationMethod.getInstance());
            icon.setImageResource(R.drawable.ic_eye_off);
        }
        field.setSelection(field.getText().length());
    }

    // ─────────────────────────────────────────────────────────────
    // Password strength validation
    // ─────────────────────────────────────────────────────────────
    private String getPasswordError(String password) {
        if (password.length() < MIN_LENGTH) {
            return "Password must be at least 8 characters";
        }
        if (!HAS_UPPERCASE.matcher(password).find()) {
            return "Password must contain at least 1 uppercase letter";
        }
        if (!HAS_DIGIT.matcher(password).find()) {
            return "Password must contain at least 1 number";
        }
        if (!HAS_SPECIAL.matcher(password).find()) {
            return "Password must contain at least 1 special character (e.g. @, #, $)";
        }
        return null;
    }

    // ─────────────────────────────────────────────────────────────
    // Change password flow
    // ─────────────────────────────────────────────────────────────
    private void changePassword() {
        String currentPassword = etCurrentPassword.getText().toString().trim();
        String newPassword     = etNewPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();

        if (currentPassword.isEmpty()) {
            etCurrentPassword.setError("Current password is required");
            etCurrentPassword.requestFocus();
            return;
        }

        if (newPassword.isEmpty()) {
            etNewPassword.setError("New password is required");
            etNewPassword.requestFocus();
            return;
        }

        String passwordError = getPasswordError(newPassword);
        if (passwordError != null) {
            etNewPassword.setError(passwordError);
            etNewPassword.requestFocus();
            return;
        }

        if (confirmPassword.isEmpty()) {
            etConfirmPassword.setError("Please confirm your new password");
            etConfirmPassword.requestFocus();
            return;
        }

        if (!newPassword.equals(confirmPassword)) {
            etConfirmPassword.setError("Passwords do not match");
            etConfirmPassword.requestFocus();
            return;
        }

        if (newPassword.equals(currentPassword)) {
            etNewPassword.setError("New password must be different from current password");
            etNewPassword.requestFocus();
            return;
        }

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null || currentUser.getEmail() == null) {
            Toast.makeText(this, "User not found. Please log in again.", Toast.LENGTH_SHORT).show();
            return;
        }

        AuthCredential credential = EmailAuthProvider.getCredential(
                currentUser.getEmail(), currentPassword
        );

        currentUser.reauthenticate(credential)
            .addOnSuccessListener(unused -> {
                currentUser.updatePassword(newPassword)
                    .addOnSuccessListener(unused2 -> {
                        Toast.makeText(this, "Password updated successfully!", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Failed to update password. Try again.", Toast.LENGTH_SHORT).show();
                    });
            })
            .addOnFailureListener(e -> {
                etCurrentPassword.setError("Incorrect current password");
                etCurrentPassword.requestFocus();
            });
    }

    // ─────────────────────────────────────────────────────────────
    // Forgot password — confirm before sending
    // ─────────────────────────────────────────────────────────────
    private void showForgotPasswordDialog() {
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser == null || currentUser.getEmail() == null) {
            Toast.makeText(this, "No account found. Please log in again.", Toast.LENGTH_SHORT).show();
            return;
        }

        String email = currentUser.getEmail();

        new AlertDialog.Builder(this)
            .setTitle("Forgot Password?")
            .setMessage("A password reset link will be sent to:\n\n" + email + "\n\nDo you want to continue?")
            .setPositiveButton("Send Reset Link", (dialog, which) -> sendPasswordResetEmail(email))
            .setNegativeButton("Cancel", null)
            .show();
    }

    // ─────────────────────────────────────────────────────────────
    // Step 1 — Firebase sends reset email
    // Step 2 — Fetch username from Realtime Database
    // Step 3 — EmailJS sends branded notification email
    // ─────────────────────────────────────────────────────────────
    private void sendPasswordResetEmail(String email) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) return;

        // Step 1: Firebase sends the actual reset link email
        mAuth.sendPasswordResetEmail(email)
            .addOnSuccessListener(unused -> {

                // Step 2: Fetch username from Realtime Database
                String userId = currentUser.getUid();
                mDatabase.child("users").child(userId).child("username")
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot snapshot) {
                            String username = snapshot.getValue(String.class);

                            // Fallback if username is null
                            if (username == null || username.isEmpty()) {
                                username = "User";
                            }

                            // Step 3: Send EmailJS notification
                            sendEmailJS(email, username);
                        }

                        @Override
                        public void onCancelled(DatabaseError error) {
                            // Still send EmailJS even if username fetch fails
                            sendEmailJS(email, "User");
                        }
                    });
            })
            .addOnFailureListener(e -> {
                Toast.makeText(this, "Failed to send reset email. Please try again.",
                        Toast.LENGTH_SHORT).show();
            });
    }

    // ─────────────────────────────────────────────────────────────
    // EmailJS API call — runs on a background thread
    // ─────────────────────────────────────────────────────────────
    private void sendEmailJS(String email, String username) {
        new Thread(() -> {
            try {
                URL url = new URL("https://api.emailjs.com/api/v1.0/email/send");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("origin", "http://localhost");
                conn.setDoOutput(true);

                // Build JSON payload
                JSONObject templateParams = new JSONObject();
                templateParams.put("to_email", email);
                templateParams.put("username", username);

                JSONObject payload = new JSONObject();
                payload.put("service_id",  EMAILJS_SERVICE_ID);
                payload.put("template_id", EMAILJS_TEMPLATE_ID);
                payload.put("user_id",     EMAILJS_PUBLIC_KEY);
                payload.put("template_params", templateParams);

                // Send request
                OutputStream os = conn.getOutputStream();
                os.write(payload.toString().getBytes());
                os.flush();
                os.close();

                int responseCode = conn.getResponseCode();

                // Back to main thread for UI
                runOnUiThread(() -> {
                    if (responseCode == 200) {
                        // Success — show confirmation dialog
                        new AlertDialog.Builder(this)
                            .setTitle("Email Sent!")
                            .setMessage("A password reset link has been sent to:\n\n" + email +
                                        "\n\nCheck your inbox and follow the instructions.")
                            .setPositiveButton("OK", (dialog, which) -> finish())
                            .show();
                    } else {
                        // Firebase email was sent, EmailJS just failed silently
                        Toast.makeText(this,
                                "Reset email sent. Check your inbox.",
                                Toast.LENGTH_SHORT).show();
                        finish();
                    }
                });

                conn.disconnect();

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    // Firebase email was still sent, so still notify user
                    Toast.makeText(this,
                            "Reset email sent. Check your inbox.",
                            Toast.LENGTH_SHORT).show();
                    finish();
                });
            }
        }).start();
    }
}
