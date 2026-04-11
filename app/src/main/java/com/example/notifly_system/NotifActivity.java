package com.example.notifly_system; // TODO: Replace with your actual package name

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.android.material.button.MaterialButton;

public class NotifActivity extends AppCompatActivity {

    // Views
    private CardView btnBack;
    private TextView tvSenderName;
    private TextView tvDate;
    private TextView tvNotifTitle;
    private TextView tvFullMessage;
    private TextView tvFullDate;
    private View unreadDot;
    private MaterialButton btnMarkRead;
    private MaterialButton btnDelete;

    // State
    private boolean isRead = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notif); // TODO: Replace with your actual layout file name

        initViews();
        loadNotificationData();
        setupClickListeners();
    }

    /**
     * Bind all views from XML
     */
    private void initViews() {
        btnBack       = findViewById(R.id.btnBack);
        tvSenderName  = findViewById(R.id.tvSenderName);
        tvDate        = findViewById(R.id.tvDate);
        tvNotifTitle  = findViewById(R.id.tvNotifTitle);
        tvFullMessage = findViewById(R.id.tvFullMessage);
        tvFullDate    = findViewById(R.id.tvFullDate);
        unreadDot     = findViewById(R.id.unreadDot);
        btnMarkRead   = findViewById(R.id.btnMarkRead);
        btnDelete     = findViewById(R.id.btnDelete);
    }

    /**
     * Populate views with data passed via Intent extras,
     * or fall back to placeholder values already set in XML.
     */
    private void loadNotificationData() {
        if (getIntent() == null) return;

        String senderName = getIntent().getStringExtra("sender_name");
        String date       = getIntent().getStringExtra("date");
        String title      = getIntent().getStringExtra("title");
        String message    = getIntent().getStringExtra("message");
        boolean read      = getIntent().getBooleanExtra("is_read", false);

        if (senderName != null) tvSenderName.setText(senderName);
        if (date       != null) {
            tvDate.setText(date);
            tvFullDate.setText(date);
        }
        if (title   != null) tvNotifTitle.setText(title);
        if (message != null) tvFullMessage.setText(message);

        // Reflect read/unread state
        isRead = read;
        updateReadState(isRead);
    }

    /**
     * Wire up all click listeners
     */
    private void setupClickListeners() {

        // ── Back button ──────────────────────────────────────────────
        btnBack.setOnClickListener(v -> onBackPressed());

        // ── Mark as Read ─────────────────────────────────────────────
        btnMarkRead.setOnClickListener(v -> {
            if (!isRead) {
                isRead = true;
                updateReadState(true);
                Toast.makeText(this, "Notification marked as read", Toast.LENGTH_SHORT).show();

                // TODO: Persist the read state to your database / repository here
                //   e.g.: NotificationRepository.markAsRead(notifId);
            } else {
                Toast.makeText(this, "Already marked as read", Toast.LENGTH_SHORT).show();
            }
        });

        // ── Delete Notification ───────────────────────────────────────
        btnDelete.setOnClickListener(v -> showDeleteConfirmationDialog());
    }

    /**
     * Toggle the unread indicator dot and button label
     */
    private void updateReadState(boolean read) {
        if (read) {
            unreadDot.setVisibility(View.GONE);
            btnMarkRead.setText("Already Read");
            btnMarkRead.setAlpha(0.5f);
            btnMarkRead.setEnabled(false);
        } else {
            unreadDot.setVisibility(View.VISIBLE);
            btnMarkRead.setText("Mark as Read");
            btnMarkRead.setAlpha(1f);
            btnMarkRead.setEnabled(true);
        }
    }

    /**
     * Confirm before deleting
     */
    private void showDeleteConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Delete Notification")
                .setMessage("Are you sure you want to delete this notification? This action cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> {
                    // TODO: Call your delete logic here
                    //   e.g.: NotificationRepository.delete(notifId);
                    Toast.makeText(this, "Notification deleted", Toast.LENGTH_SHORT).show();
                    finish(); // Close the screen after deletion
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
