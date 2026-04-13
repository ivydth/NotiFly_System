package com.example.notifly_system;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.android.material.button.MaterialButton;

public class NotifActivity extends AppCompatActivity {

    public static final String EXTRA_NOTIF_ID = "extra_notif_id";

    private CardView       btnBack;
    private TextView       tvSenderName;
    private TextView       tvDate;
    private TextView       tvNotifTitle;
    private TextView       tvFullMessage;
    private TextView       tvFullDate;
    private View           unreadDot;
    private MaterialButton btnMarkRead;
    private MaterialButton btnArchive;

    private NotificationItem currentItem;

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
        btnArchive    = findViewById(R.id.btnDelete);
    }

    // ── Load data ─────────────────────────────────────────────────────────────

    private void loadNotificationData() {
        String notifId = getIntent().getStringExtra(EXTRA_NOTIF_ID);

        if (notifId != null) {
            // Search all items including archived ones so archive screen
            // can also open this detail view
            for (NotificationItem n : NotificationStore.getInstance().getArchived()) {
                if (n.id.equals(notifId)) {
                    currentItem = n;
                    break;
                }
            }
            // If not found in archive, check normal items
            if (currentItem == null) {
                for (NotificationItem n : NotificationStore.getInstance().getAll()) {
                    if (n.id.equals(notifId)) {
                        currentItem = n;
                        break;
                    }
                }
            }
        }

        if (currentItem == null) {
            Toast.makeText(this, "Notification not found.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        tvSenderName.setText(currentItem.senderName);
        tvDate.setText(currentItem.dateLabel);
        tvFullDate.setText(currentItem.dateLabel);
        tvNotifTitle.setText(currentItem.senderName);
        tvFullMessage.setText(currentItem.message);

        // No auto-mark — user must press the button
        updateReadState(currentItem.isRead);
        updateArchiveButton(currentItem.isArchived);
    }

    // ── Click listeners ───────────────────────────────────────────────────────

    private void setupClickListeners() {

        btnBack.setOnClickListener(v -> onBackPressed());

        // Toggles between Mark as Read and Mark as Unread
        btnMarkRead.setOnClickListener(v -> {
            if (!currentItem.isRead) {
                NotificationStore.getInstance().markRead(currentItem.id);
                currentItem.isRead = true;
                updateReadState(true);
                Toast.makeText(this, "Marked as read", Toast.LENGTH_SHORT).show();
            } else {
                NotificationStore.getInstance().markUnread(currentItem.id);
                currentItem.isRead = false;
                updateReadState(false);
                Toast.makeText(this, "Marked as unread", Toast.LENGTH_SHORT).show();
            }
        });

        // Toggles between Archive and Unarchive
        btnArchive.setOnClickListener(v -> {
            if (!currentItem.isArchived) {
                NotificationStore.getInstance().archive(currentItem.id);
                currentItem.isArchived = true;
                Toast.makeText(this, "Notification archived", Toast.LENGTH_SHORT).show();
                // Navigate to archive so user sees it land there
                Intent intent = new Intent(this, ArcActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                finish();
            } else {
                NotificationStore.getInstance().unarchive(currentItem.id);
                currentItem.isArchived = false;
                updateArchiveButton(false);
                Toast.makeText(this, "Removed from archive", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ── UI state ──────────────────────────────────────────────────────────────

    private void updateReadState(boolean read) {
        if (read) {
            unreadDot.setVisibility(View.GONE);
            btnMarkRead.setText("Mark as Unread");
            btnMarkRead.setAlpha(1f);
            btnMarkRead.setEnabled(true);
        } else {
            unreadDot.setVisibility(View.VISIBLE);
            btnMarkRead.setText("Mark as Read");
            btnMarkRead.setAlpha(1f);
            btnMarkRead.setEnabled(true);
        }
    }

    private void updateArchiveButton(boolean archived) {
        if (archived) {
            btnArchive.setText("Remove from Archive");
        } else {
            btnArchive.setText("Archive");
        }
    }
}
