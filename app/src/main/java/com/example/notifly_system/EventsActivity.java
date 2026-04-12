package com.example.notifly_system; // TODO: Replace with your actual package name

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageView;

public class EventsActivity extends AppCompatActivity {

    private AppCompatImageView btnMenu;
    private AppCompatImageView btnProfile;
    private AppCompatImageView ivHome;
    private AppCompatImageView ivSearch;
    private AppCompatImageView ivBell;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.events_activity); // rename to match your actual XML file name

        initViews();
        setListeners();
    }

    private void initViews() {
        btnMenu    = findViewById(R.id.btnMenu);
        btnProfile = findViewById(R.id.btnProfile);
        ivHome     = findViewById(R.id.ivHome);
        ivSearch   = findViewById(R.id.ivSearch);
        ivBell     = findViewById(R.id.ivBell);
    }

    private void setListeners() {

        btnMenu.setOnClickListener(v -> {
            startActivity(new Intent(this, UserMenu.class));
            overridePendingTransition(0, 0);
        });

        btnProfile.setOnClickListener(v -> {
            startActivity(new Intent(this, ProfileActivity.class));
        });

        ivHome.setOnClickListener(v -> {
            startActivity(new Intent(this, UserActivity.class));
        });

        ivSearch.setOnClickListener(v -> {
            // TODO: navigate to search activity
        });

        ivBell.setOnClickListener(v -> {
            startActivity(new Intent(this, NotifActivity1.class));
        });
    }
}
