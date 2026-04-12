package com.example.notifly_system; // TODO: Replace with your actual package name

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageView;

public class ArcActivity extends AppCompatActivity {

    private AppCompatImageView btnMenu;
    private AppCompatImageView btnProfile;
    private AppCompatImageView ivHome;
    private AppCompatImageView ivSearch;
    private AppCompatImageView ivBell;

    private LinearLayout archiveContainer;
    private LinearLayout emptyState;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.archive_activity);

        initViews();
        setListeners();
        loadArchivedNotifications();
    }

    private void initViews() {
        btnMenu          = findViewById(R.id.btnMenu);
        btnProfile       = findViewById(R.id.btnProfile);
        ivHome           = findViewById(R.id.ivHome);
        ivSearch         = findViewById(R.id.ivSearch);
        ivBell           = findViewById(R.id.ivBell);
        archiveContainer = findViewById(R.id.archiveContainer);
        emptyState       = findViewById(R.id.emptyState);
    }

    private void setListeners() {

        btnMenu.setOnClickListener(v -> {
            startActivity(new Intent(this, UserMenu.class));
        });

        btnProfile.setOnClickListener(v -> {
            // TODO: Navigate to profile screen
        });

        ivHome.setOnClickListener(v -> {
            startActivity(new Intent(this, UserActivity.class));
            finish();
        });

        ivSearch.setOnClickListener(v -> {
            // TODO: Open search screen
        });

        ivBell.setOnClickListener(v -> {
            startActivity(new Intent(this, NotifActivity.class));
            finish();
        });
    }

    private void loadArchivedNotifications() {
        // TODO: Replace with your actual Firebase or data source logic
        // Example: query Firebase for notifications where archived == true

        // For now, toggle empty state based on whether archiveContainer has children
        checkIfEmpty();
    }

    private void checkIfEmpty() {
        if (archiveContainer.getChildCount() == 0) {
            emptyState.setVisibility(View.VISIBLE);
            archiveContainer.setVisibility(View.GONE);
        } else {
            emptyState.setVisibility(View.GONE);
            archiveContainer.setVisibility(View.VISIBLE);
        }
    }

    // Call this method to add a notification row dynamically
    private void addArchiveRow(String message, String date) {
        // TODO: Inflate your custom row layout and add it to archiveContainer
        // Example:
        // View row = LayoutInflater.from(this).inflate(R.layout.item_notification_row, archiveContainer, false);
        // TextView tvMessage = row.findViewById(R.id.tvMessage);
        // TextView tvDate    = row.findViewById(R.id.tvDate);
        // tvMessage.setText(message);
        // tvDate.setText(date);
        // archiveContainer.addView(row);
        checkIfEmpty();
    }
}
