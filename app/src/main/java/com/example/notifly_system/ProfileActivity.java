package com.example.notifly_system;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

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

    // Firebase
    FirebaseAuth mAuth;
    DatabaseReference database;

    // Holds the current email before any edits so we can detect a change
    private String currentEmailBeforeEdit = "";

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
        layoutEditMode   = findViewById(R.id.layoutEditMode);
        etName           = findViewById(R.id.etName);
        etEmail          = findViewById(R.id.etEmail);
        btnCancelEdit    = findViewById(R.id.btnCancelEdit);
        btnSaveProfile   = findViewById(R.id.btnSaveProfile);

        rowChangePassword = findViewById(R.id.rowChangePassword);
        rowLogOut         = findViewById(R.id.rowLogOut);
        btnBack           = findViewById(R.id.btnBack);

        // ── FIREBASE ──────────────────────────────────────────────

        mAuth    = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance(
                "https://notifly-94dba-default-rtdb.asia-southeast1.firebasedatabase.app/"
        ).getReference("users");

        // ── LOAD USER DATA ────────────────────────────────────────

        loadUserData();

        // ── LISTENERS ─────────────────────────────────────────────

        btnBack.setOnClickListener(v -> finish());

        btnEditProfile.setOnClickListener(v -> openEditMode());
        btnEditName.setOnClickListener(v -> openEditMode());
        btnEditEmail.setOnClickListener(v -> openEditMode());

        btnCancelEdit.setOnClickListener(v -> closeEditMode());

        btnSaveProfile.setOnClickListener(v -> saveProfile());

        rowChangePassword.setOnClickListener(v ->
                startActivity(new Intent(this, ChangePass.class)));

        rowLogOut.setOnClickListener(v -> {
            mAuth.signOut();
            startActivity(new Intent(this, MainActivity.class));
            finishAffinity();
        });
    }

    // ─────────────────────────────────────────────────────────────
    // Load user data from Firebase
    // ─────────────────────────────────────────────────────────────
    private void loadUserData() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) return;

        database.child(currentUser.getUid())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        if (!snapshot.exists()) return;

                        String firstName = snapshot.child("firstName").getValue(String.class);
                        String username  = snapshot.child("username").getValue(String.class);
                        String email     = snapshot.child("email").getValue(String.class);

                        // Resolve display name
                        String displayName;
                        if (username != null && !username.isEmpty()) {
                            displayName = username;
                        } else if (firstName != null && !firstName.isEmpty()) {
                            displayName = firstName;
                        } else {
                            displayName = "User";
                        }

                        // Fallback to Firebase Auth email
                        String displayEmail = (email != null && !email.isEmpty())
                                ? email
                                : (currentUser.getEmail() != null ? currentUser.getEmail() : "");

                        // Remember the current email so saveProfile() can detect a change
                        currentEmailBeforeEdit = displayEmail;

                        populateViews(displayName, displayEmail);
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        populateViews("User", "");
                    }
                });
    }

    // ─────────────────────────────────────────────────────────────
    // Populate all views with loaded data
    // ─────────────────────────────────────────────────────────────
    private void populateViews(String name, String email) {
        String initial = (name != null && !name.isEmpty())
                ? String.valueOf(name.charAt(0)).toUpperCase()
                : "?";

        tvAvatarInitials.setText(initial);
        tvDisplayName.setText(name);
        tvDisplayEmail.setText(email);
        tvViewName.setText(name);
        tvViewEmail.setText(email);
        etName.setText(name);
        etEmail.setText(email);
    }

    // ─────────────────────────────────────────────────────────────
    // Edit mode helpers
    // ─────────────────────────────────────────────────────────────
    private void openEditMode() {
        layoutViewMode.setVisibility(View.GONE);
        layoutEditMode.setVisibility(View.VISIBLE);
    }

    private void closeEditMode() {
        layoutEditMode.setVisibility(View.GONE);
        layoutViewMode.setVisibility(View.VISIBLE);
    }

    // ─────────────────────────────────────────────────────────────
    // Save profile — with email change notification
    // ─────────────────────────────────────────────────────────────
    private void saveProfile() {
        String newName  = etName.getText().toString().trim();
        String newEmail = etEmail.getText().toString().trim();

        // ── Validation ────────────────────────────────────────────

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

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) return;

        // Detect whether the email actually changed
        boolean emailChanged = !newEmail.equalsIgnoreCase(currentEmailBeforeEdit);

        // ── Save to Firebase Realtime Database ────────────────────

        Map<String, Object> updates = new HashMap<>();
        updates.put("username", newName);
        updates.put("email",    newEmail);

        database.child(currentUser.getUid()).updateChildren(updates)
            .addOnSuccessListener(unused -> {

                // ── Update Firebase Auth email ─────────────────────
                currentUser.updateEmail(newEmail)
                    .addOnSuccessListener(unused2 -> {
                        populateViews(newName, newEmail);

                        // Update the tracked email so future edits
                        // in the same session compare against the new value
                        currentEmailBeforeEdit = newEmail;

                        closeEditMode();
                        Toast.makeText(this, "Profile updated!", Toast.LENGTH_SHORT).show();

                        // ── Send email change notification ─────────
                        // Only fires when the user actually changed their email
                        if (emailChanged) {
                            EmailHelper.sendEmailChangedEmail(this, newName, newEmail);
                        }
                    })
                    .addOnFailureListener(e -> {
                        // DB was saved but Firebase Auth email update failed.
                        // Still update UI — the DB value is the source of truth.
                        populateViews(newName, newEmail);
                        currentEmailBeforeEdit = newEmail;
                        closeEditMode();
                        Toast.makeText(this, "Profile updated!", Toast.LENGTH_SHORT).show();

                        // Still send the notification since the DB email changed
                        if (emailChanged) {
                            EmailHelper.sendEmailChangedEmail(this, newName, newEmail);
                        }
                    });
            })
            .addOnFailureListener(e ->
                    Toast.makeText(this,
                            "Failed to update profile. Try again.",
                            Toast.LENGTH_SHORT).show());
    }
}
