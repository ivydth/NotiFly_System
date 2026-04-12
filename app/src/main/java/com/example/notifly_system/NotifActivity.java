package com.example.notifly_system;

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
    private CardView       btnBack;
    private TextView       tvSenderName;
    private TextView       tvDate;
    private TextView       tvNotifTitle;
    private TextView       tvFullMessage;
    private TextView       tvFullDate;
    private View           unreadDot;
    private MaterialButton btnMarkRead;
    private MaterialButton btnDelete;

    // The item being viewed
    private NotificationItem currentItem;

    // ── Extras keys (use these when launching this activity) ──────────────────
    public static final String EXTRA_NOTIF_ID = "extra_notif_id";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notif);

        initViews();
        loadNotificationData();
        setupClickListeners();
    }

    // ── Init ──────────────────────────────────────────────────────────────────

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
     * Reads the notification ID from the intent, looks it up in the store,
     * and auto-marks it as read as soon as the screen opens.
     */
    private void loadNotificationData() {
        String notifId = getIntent().getStringExtra(EXTRA_NOTIF_ID);

        if (notifId != null) {
            // Look up the live item from the store
            for (NotificationItem n : NotificationStore.getInstance().getAll()) {
                if (n.id.equals(notifId)) {
                    currentItem = n;
                    break;
                }
            }
        }

        if (currentItem == null) {
            // Fallback: nothing to show, just go back
            Toast.makeText(this, "Notification not found.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Populate views
        tvSenderName.setText(currentItem.senderName);
        tvDate.setText(currentItem.dateLabel);
        tvFullDate.setText(currentItem.dateLabel);

        // Use senderName as title if you don't have a separate title field,
        // or add a `title` field to NotificationItem later
        tvNotifTitle.setText(currentItem.senderName);
        tvFullMessage.setText(currentItem.message);

        // Auto-mark as read the moment the user opens this screen
        if (!currentItem.isRead) {
            NotificationStore.getInstance().markRead(currentItem.id);
            currentItem.isRead = true;
        }

        updateReadState(currentItem.isRead);
    }

    // ── Click listeners ───────────────────────────────────────────────────────

    private void setupClickListeners() {

        btnBack.setOnClickListener(v -> onBackPressed());

        btnMarkRead.setOnClickListener(v -> {
            if (!currentItem.isRead) {
                NotificationStore.getInstance().markRead(currentItem.id);
                currentItem.isRead = true;
                updateReadState(true);
                Toast.makeText(this, "Marked as read", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Already marked as read", Toast.LENGTH_SHORT).show();
            }
        });

        btnDelete.setOnClickListener(v -> showDeleteConfirmationDialog());
    }

    // ── UI state ──────────────────────────────────────────────────────────────

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

    // ── Delete ────────────────────────────────────────────────────────────────

    private void showDeleteConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Delete Notification")
                .setMessage("Are you sure you want to delete this notification? This cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> {
                    NotificationStore.getInstance().delete(currentItem.id);
                    Toast.makeText(this, "Notification deleted", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
