package com.example.notifly_system;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

public class ProfileActivity extends AppCompatActivity {

    // Avatar & display
    TextView tvAvatarInitials, tvDisplayName, tvDisplayEmail;

    // View mode
    View layoutViewMode;
    TextView tvViewName, tvViewEmail;
    TextView btnEditProfile;
    CardView btnEditName, btnEditEmail;

    // Edit mode
    View layoutEditMode;
    EditText etName, etEmail;
    View btnCancelEdit, btnSaveProfile;

    // Action rows
    View rowChangePassword, rowLogOut;

    // Back
    View btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        // ── INITIALIZE VIEWS ──────────────────────────────────────

        tvAvatarInitials = findViewById(R.id.tvAvatarInitials);
        tvDisplayName    = findViewById(R.id.tvDisplayName);
        tvDisplayEmail   = findViewById(R.id.tvDisplayEmail);

        layoutViewMode   = findViewById(R.id.layoutViewMode);
        tvViewName       = findViewById(R.id.tvViewName);
        tvViewEmail      = findViewById(R.id.tvViewEmail);
        btnEditProfile   = findViewById(R.id.btnEditProfile);
        btnEditName      = findViewById(R.id.btnEditName);
        btnEditEmail     = findViewById(R.id.btnEditEmail);

        layoutEditMode   = findViewById(R.id.layoutEditMode);
        etName           = findViewById(R.id.etName);
        etEmail          = findViewById(R.id.etEmail);
        btnCancelEdit    = findViewById(R.id.btnCancelEdit);
        btnSaveProfile   = findViewById(R.id.btnSaveProfile);

        rowChangePassword = findViewById(R.id.rowChangePassword);
        rowLogOut         = findViewById(R.id.rowLogOut);
        btnBack           = findViewById(R.id.btnBack);

        // ── LOAD PLACEHOLDER DATA ─────────────────────────────────

        // TODO: replace with Firebase fetch later
        String name  = "Juan dela Cruz";
        String email = "juan@example.com";
        populateViews(name, email);

        // ── LISTENERS ─────────────────────────────────────────────

        // Back
        btnBack.setOnClickListener(v -> finish());

        // Edit Profile button / edit name / edit email — all open edit mode
        btnEditProfile.setOnClickListener(v -> openEditMode());
        btnEditName.setOnClickListener(v -> openEditMode());
        btnEditEmail.setOnClickListener(v -> openEditMode());

        // Cancel — go back to view mode
        btnCancelEdit.setOnClickListener(v -> closeEditMode());

        // Save Changes
        btnSaveProfile.setOnClickListener(v -> saveProfile());

        // Change Password
        rowChangePassword.setOnClickListener(v -> {
            // TODO: navigate to ChangePasswordActivity
            Toast.makeText(this, "Change Password coming soon", Toast.LENGTH_SHORT).show();
        });

        // Log Out
        rowLogOut.setOnClickListener(v -> {
            // TODO: add Firebase sign out + redirect to LoginActivity
            Toast.makeText(this, "Log Out coming soon", Toast.LENGTH_SHORT).show();
        });
    }

    private void populateViews(String name, String email) {
        // Avatar initials — first letter of name
        String initial = (name != null && !name.isEmpty())
                ? String.valueOf(name.charAt(0)).toUpperCase()
                : "?";
        tvAvatarInitials.setText(initial);

        // Display section
        tvDisplayName.setText(name);
        tvDisplayEmail.setText(email);

        // View mode rows
        tvViewName.setText(name);
        tvViewEmail.setText(email);

        // Pre-fill edit fields
        etName.setText(name);
        etEmail.setText(email);
    }

    private void openEditMode() {
        layoutViewMode.setVisibility(View.GONE);
        layoutEditMode.setVisibility(View.VISIBLE);
    }

    private void closeEditMode() {
        layoutEditMode.setVisibility(View.GONE);
        layoutViewMode.setVisibility(View.VISIBLE);
    }

    private void saveProfile() {
        String newName  = etName.getText().toString().trim();
        String newEmail = etEmail.getText().toString().trim();

        if (newName.isEmpty()) {
            etName.setError("Name is required");
            etName.requestFocus();
            return;
        }

        if (newEmail.isEmpty()) {
            etEmail.setError("Email is required");
            etEmail.requestFocus();
            return;
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(newEmail).matches()) {
            etEmail.setError("Enter a valid email");
            etEmail.requestFocus();
            return;
        }

        // Update UI immediately
        populateViews(newName, newEmail);
        closeEditMode();

        // TODO: save to Firebase here
        Toast.makeText(this, "Profile updated!", Toast.LENGTH_SHORT).show();
    }
}
