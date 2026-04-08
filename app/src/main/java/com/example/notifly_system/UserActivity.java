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

public class MainActivity extends AppCompatActivity {

    // Top bar
    AppCompatImageView btnMenu, btnProfile;

    // Welcome banner
    TextView tvWelcomeUser;

    // Summary counts
    TextView tvTotalCount, tvUnreadCount, tvStarredCount;

    // Bottom nav
    AppCompatImageView ivHome, ivSearch, ivBell;

    // Firebase
    FirebaseAuth mAuth;
    DatabaseReference database;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.user_activity);

        // ── INITIALIZE VIEWS ──────────────────────────────────────

        // Top bar
        btnMenu = findViewById(R.id.btnMenu);
        btnProfile = findViewById(R.id.btnProfile);

        // Welcome banner
        tvWelcomeUser = findViewById(R.id.tvWelcomeUser);

        // Summary counts
        tvTotalCount = findViewById(R.id.tvTotalCount);
        tvUnreadCount = findViewById(R.id.tvUnreadCount);
        tvStarredCount = findViewById(R.id.tvStarredCount);

        // Bottom nav
        ivHome = findViewById(R.id.ivHome);
        ivSearch = findViewById(R.id.ivSearch);
        ivBell = findViewById(R.id.ivBell);

        // ── FIREBASE ──────────────────────────────────────────────

        mAuth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance("https://notifly-94dba-default-rtdb.asia-southeast1.firebasedatabase.app/")
                .getReference("users");

        // ── LOAD USER DATA ────────────────────────────────────────

        loadUserData();

        // ── BUTTON LISTENERS ──────────────────────────────────────

        // Menu button
        btnMenu.setOnClickListener(v -> {
            // TODO: open drawer or menu
        });

        // Profile button
        btnProfile.setOnClickListener(v -> {
            // TODO: navigate to profile activity
        });

        // Bottom nav - Home
        ivHome.setOnClickListener(v -> {
            // already on home, do nothing or refresh
        });

        // Bottom nav - Search
        ivSearch.setOnClickListener(v -> {
            // TODO: navigate to search activity
        });

        // Bottom nav - Bell
        ivBell.setOnClickListener(v -> {
            // TODO: navigate to notifications activity
        });
    }

    private void loadUserData() {
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser == null) {
            // not logged in, go back to login
            startActivity(new Intent(this, login_activity.class));
            finish();
            return;
        }

        String userId = currentUser.getUid();

        // fetch user data from Realtime Database
        database.child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String firstName = snapshot.child("firstName").getValue(String.class);
                    String username = snapshot.child("username").getValue(String.class);

                    // display username if available, otherwise use first name
                    if (username != null && !username.isEmpty()) {
                        tvWelcomeUser.setText(username + "!");
                    } else if (firstName != null && !firstName.isEmpty()) {
                        tvWelcomeUser.setText(firstName + "!");
                    } else {
                        tvWelcomeUser.setText("User!");
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                tvWelcomeUser.setText("User!");
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        // check if user is still logged in
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            startActivity(new Intent(this, login_activity.class));
            finish();
        }
    }
}
