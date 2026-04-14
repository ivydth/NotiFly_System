package com.example.notifly_system;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class NotifActivity extends AppCompatActivity {

    public static final String EXTRA_NOTIF_ID = "extra_notif_id";

    private CardView       btnBack;
    private TextView       tvSenderName;
    private TextView       tvDate;
    private TextView       tvNotifTitle;
    private TextView       tvFullMessage;
    private TextView       tvFullDate;
    private TextView       tvAvatar;
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
        tvAvatar      = findViewById(R.id.tvAvatar);
        unreadDot     = findViewById(R.id.unreadDot);
        btnMarkRead   = findViewById(R.id.btnMarkRead);
        btnArchive    = findViewById(R.id.btnDelete);
    }

    // ── Load data ─────────────────────────────────────────────────────────────

    private void loadNotificationData() {
        String notifId = getIntent().getStringExtra(EXTRA_NOTIF_ID);

        if (notifId != null) {
            // Check archived items first so archive screen rows open correctly
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

        // Split "title: body" merged message into separate parts
        String rawMessage = currentItem.message != null ? currentItem.message : "";
        String displayTitle;
        String displayBody;

        int separatorIdx = rawMessage.indexOf(": ");
        if (separatorIdx > 0) {
            displayTitle = rawMessage.substring(0, separatorIdx);
            displayBody  = rawMessage.substring(separatorIdx + 2);
        } else {
            displayTitle = currentItem.senderName != null ? currentItem.senderName : "Notification";
            displayBody  = rawMessage;
        }

        // ── Format timestamps in Philippine Time (Asia/Manila, UTC+8) ─────────
        String shortDate = formatShortDate(currentItem.timestamp);   // e.g. "Apr 14"
        String fullDate  = formatFullDate(currentItem.timestamp);    // e.g. "April 14, 2026 — 10:45 AM"

        tvSenderName.setText(currentItem.senderName);
        tvDate.setText(shortDate);          // sender row — short date + time
        tvFullDate.setText(fullDate);       // DATE RECEIVED section — full date + time
        tvNotifTitle.setText(displayTitle);
        tvFullMessage.setText(displayBody);

        // Avatar — first letter of sender name
        if (tvAvatar != null) {
            String name = (currentItem.senderName != null && !currentItem.senderName.isEmpty())
                    ? currentItem.senderName : "N";
            tvAvatar.setText(String.valueOf(name.charAt(0)).toUpperCase());
        }

        updateReadState(currentItem.isRead);
        updateArchiveButton(currentItem.isArchived);
    }

    // ── Philippine Time formatters ─────────────────────────────────────────────

    /**
     * Short label shown in the sender row.
     * e.g.  "Apr 14, 10:45 AM"
     */
    private String formatShortDate(long timestamp) {
        if (timestamp <= 0) return currentItem.dateLabel != null ? currentItem.dateLabel : "—";
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM d, h:mm a", Locale.getDefault());
            sdf.setTimeZone(TimeZone.getTimeZone("Asia/Manila"));
            return sdf.format(new Date(timestamp));
        } catch (Exception e) {
            return currentItem.dateLabel != null ? currentItem.dateLabel : "—";
        }
    }

    /**
     * Full label shown in the DATE RECEIVED section.
     * e.g.  "April 14, 2026 — 10:45 AM"
     */
    private String formatFullDate(long timestamp) {
        if (timestamp <= 0) return currentItem.dateLabel != null ? currentItem.dateLabel : "—";
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("MMMM d, yyyy — h:mm a", Locale.getDefault());
            sdf.setTimeZone(TimeZone.getTimeZone("Asia/Manila"));
            return sdf.format(new Date(timestamp));
        } catch (Exception e) {
            return currentItem.dateLabel != null ? currentItem.dateLabel : "—";
        }
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

        // Toggles between Archive and Remove from Archive
        btnArchive.setOnClickListener(v -> {
            if (!currentItem.isArchived) {
                NotificationStore.getInstance().archive(currentItem.id);
                currentItem.isArchived = true;
                updateArchiveButton(true);
                Toast.makeText(this, "Archived", Toast.LENGTH_SHORT).show();
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
        } else {
            unreadDot.setVisibility(View.VISIBLE);
            btnMarkRead.setText("Mark as Read");
        }
        btnMarkRead.setAlpha(1f);
        btnMarkRead.setEnabled(true);
    }

    private void updateArchiveButton(boolean archived) {
        if (archived) {
            btnArchive.setText("Remove from Archive");
        } else {
            btnArchive.setText("Archive");
        }
    }
}
