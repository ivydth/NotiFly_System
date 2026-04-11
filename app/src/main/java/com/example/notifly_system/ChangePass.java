package com.example.notifly_system;

import android.content.Intent;
import android.os.Bundle;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class ChangePass extends AppCompatActivity {

    // Back
    CardView btnBack;

    // Fields
    EditText etCurrentPassword, etNewPassword, etConfirmPassword;

    // Eye toggles
    ImageView ivToggleCurrentPassword, ivToggleNewPassword, ivToggleConfirmPassword;

    // Save button
    View btnChangePassword;

    // Firebase
    FirebaseAuth mAuth;

    // Toggle states
    boolean isCurrentVisible = false;
    boolean isNewVisible     = false;
    boolean isConfirmVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change);

        // ── INITIALIZE VIEWS ──────────────────────────────────────

        btnBack               = findViewById(R.id.btnBack);
        etCurrentPassword     = findViewById(R.id.etCurrentPassword);
        etNewPassword         = findViewById(R.id.etNewPassword);
        etConfirmPassword     = findViewById(R.id.etConfirmPassword);
        ivToggleCurrentPassword = findViewById(R.id.ivToggleCurrentPassword);
        ivToggleNewPassword     = findViewById(R.id.ivToggleNewPassword);
        ivToggleConfirmPassword = findViewById(R.id.ivToggleConfirmPassword);
        btnChangePassword     = findViewById(R.id.btnChangePassword);

        // ── FIREBASE ──────────────────────────────────────────────

        mAuth = FirebaseAuth.getInstance();

        // ── LISTENERS ─────────────────────────────────────────────

        btnBack.setOnClickListener(v -> finish());

        // Eye toggles
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

        // Save button
        btnChangePassword.setOnClickListener(v -> changePassword());
    }

    private void togglePasswordVisibility(EditText field, ImageView icon, boolean isVisible) {
        if (isVisible) {
            field.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
            icon.setImageResource(R.drawable.ic_eye_on);
        } else {
            field.setTransformationMethod(PasswordTransformationMethod.getInstance());
            icon.setImageResource(R.drawable.ic_eye_off);
        }
        // keep cursor at end
        field.setSelection(field.getText().length());
    }

    private void changePassword() {
        String currentPassword = etCurrentPassword.getText().toString().trim();
        String newPassword     = etNewPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();

        // ── VALIDATIONS ───────────────────────────────────────────

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

        if (newPassword.length() < 6) {
            etNewPassword.setError("Password must be at least 6 characters");
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

        // ── REAUTHENTICATE THEN UPDATE ────────────────────────────

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null || currentUser.getEmail() == null) {
            Toast.makeText(this, "User not found. Please log in again.", Toast.LENGTH_SHORT).show();
            return;
        }

        // reauthenticate first — required by Firebase before sensitive operations
        AuthCredential credential = EmailAuthProvider.getCredential(
                currentUser.getEmail(), currentPassword
        );

        currentUser.reauthenticate(credential)
            .addOnSuccessListener(unused -> {
                // reauthentication passed — now update password
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
                // wrong current password
                etCurrentPassword.setError("Incorrect current password");
                etCurrentPassword.requestFocus();
            });
    }
}
