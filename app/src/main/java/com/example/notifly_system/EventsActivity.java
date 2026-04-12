package com.example.notifly; // TODO: Replace with your actual package name

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
        setContentView(R.layout.activity_events); // rename to match your actual XML file name

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
            // TODO: Open navigation drawer or menu
        });

        btnProfile.setOnClickListener(v -> {
            // TODO: Navigate to profile screen
        });

        ivHome.setOnClickListener(v -> {
            // TODO: Navigate to home screen
            finish();
        });

        ivSearch.setOnClickListener(v -> {
            // TODO: Open search screen
        });

        ivBell.setOnClickListener(v -> {
            // TODO: Navigate to notifications screen
        });
    }
}
