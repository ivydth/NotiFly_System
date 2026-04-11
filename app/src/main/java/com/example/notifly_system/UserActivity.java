package com.example.notifly_system;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class UserActivity extends AppCompatActivity {

    // Top bar
    AppCompatImageView btnMenu;
    TextView           btnProfile; // ← now a TextView for letter avatar

    // Welcome banner
    TextView tvWelcomeUser;

    // Summary counts
    TextView tvTotalCount, tvUnreadCount, tvStarredCount;

    // Bottom nav
    AppCompatImageView ivHome, ivSearch, ivBell;

    // Firebase
    FirebaseAuth      mAuth;
    DatabaseReference database;

    // User data
    String currentUsername = "User";
    String currentEmail    = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.user_activity);

        // ── INITIALIZE VIEWS ──────────────────────────────────────

        btnMenu    = findViewById(R.id.btnMenu);
        btnProfile = findViewById(R.id.btnProfile); // TextView now

        tvWelcomeUser = findViewById(R.id.tvWelcomeUser);

        tvTotalCount   = findViewById(R.id.tvTotalCount);
        tvUnreadCount  = findViewById(R.id.tvUnreadCount);
        tvStarredCount = findViewById(R.id.tvStarredCount);

        ivHome   = findViewById(R.id.ivHome);
        ivSearch = findViewById(R.id.ivSearch);
        ivBell   = findViewById(R.id.ivBell);

        // ── FIREBASE ──────────────────────────────────────────────

        mAuth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance(
                "https://notifly-94dba-default-rtdb.asia-southeast1.firebasedatabase.app/"
        ).getReference("users");

        // ── LOAD USER DATA ────────────────────────────────────────

        loadUserData();

        // ── BUTTON LISTENERS ──────────────────────────────────────

        btnMenu.setOnClickListener(v -> {
            startActivity(new Intent(this, UserMenu.class));
            overridePendingTransition(0, 0);
        });

        btnProfile.setOnClickListener(v -> {
            startActivity(new Intent(this, ProfileActivity.class));
        });

        ivHome.setOnClickListener(v -> {
            // already on home
        });

        ivSearch.setOnClickListener(v -> {
            // TODO: navigate to search activity
        });

        ivBell.setOnClickListener(v -> {
            // TODO: navigate to notifications activity
        });
    }

    private void loadUserData() {
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        String userId = currentUser.getUid();

        database.child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String firstName = snapshot.child("firstName").getValue(String.class);
                    String username  = snapshot.child("username").getValue(String.class);
                    String email     = snapshot.child("email").getValue(String.class);

                    // ── Set welcome text ──────────────────────────
                    if (username != null && !username.isEmpty()) {
                        tvWelcomeUser.setText(username + "!");
                        currentUsername = username;
                    } else if (firstName != null && !firstName.isEmpty()) {
                        tvWelcomeUser.setText(firstName + "!");
                        currentUsername = firstName;
                    } else {
                        tvWelcomeUser.setText("User!");
                        currentUsername = "User";
                    }

                    // ── Set profile avatar letter ─────────────────
                    // Takes the first letter of username, uppercased
                    String avatarLetter = currentUsername.substring(0, 1).toUpperCase();
                    btnProfile.setText(avatarLetter);

                    // ── Set email ─────────────────────────────────
                    currentEmail = (email != null && !email.isEmpty())
                            ? email
                            : (currentUser.getEmail() != null ? currentUser.getEmail() : "");
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                tvWelcomeUser.setText("User!");
                btnProfile.setText("U"); // fallback letter
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        }
    }
}
