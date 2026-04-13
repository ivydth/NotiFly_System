package com.example.notifly_system;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageView;

import java.util.List;

public class ArcActivity extends AppCompatActivity
        implements NotificationStore.StoreListener {

    private AppCompatImageView btnMenu;
    private AppCompatImageView btnProfile;
    private AppCompatImageView ivHome;
    private AppCompatImageView ivSearch;
    private AppCompatImageView ivBell;

    private LinearLayout archiveContainer;
    private LinearLayout emptyState;

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.archive_activity);

        initViews();
        setListeners();
    }

    @Override
    protected void onResume() {
        super.onResume();
        NotificationStore.getInstance().addListener(this);
        loadArchivedNotifications();
    }

    @Override
    protected void onPause() {
        super.onPause();
        NotificationStore.getInstance().removeListener(this);
    }

    // ── StoreListener ─────────────────────────────────────────────────────────

    @Override
    public void onStoreChanged() {
        runOnUiThread(this::loadArchivedNotifications);
    }

    // ── Init ──────────────────────────────────────────────────────────────────

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
        btnMenu.setOnClickListener(v ->
                startActivity(new Intent(this, UserMenu.class)));

        btnProfile.setOnClickListener(v ->
                startActivity(new Intent(this, ProfileActivity.class)));

        ivHome.setOnClickListener(v -> {
            startActivity(new Intent(this, UserActivity.class));
            finish();
        });

        ivSearch.setOnClickListener(v -> { /* TODO */ });

        ivBell.setOnClickListener(v -> {
            startActivity(new Intent(this, NotifActivity1.class));
            finish();
        });
    }

    // ── Load ──────────────────────────────────────────────────────────────────

    private void loadArchivedNotifications() {
        archiveContainer.removeAllViews();

        List<NotificationItem> archived =
                NotificationStore.getInstance().getArchived();

        if (archived.isEmpty()) {
            emptyState.setVisibility(View.VISIBLE);
            archiveContainer.setVisibility(View.GONE);
            return;
        }

        emptyState.setVisibility(View.GONE);
        archiveContainer.setVisibility(View.VISIBLE);

        for (int i = 0; i < archived.size(); i++) {
            View row = buildArchiveRow(archived.get(i), i < archived.size() - 1);
            archiveContainer.addView(row);
        }
    }

    // ── Row builder ───────────────────────────────────────────────────────────

    private View buildArchiveRow(NotificationItem item, boolean showDivider) {
        View row = LayoutInflater.from(this)
                .inflate(R.layout.item_notification_row, archiveContainer, false);

        View     avatar    = row.findViewById(R.id.ivAvatar);
        TextView tvName    = row.findViewById(R.id.tvSenderName);
        TextView tvMessage = row.findViewById(R.id.tvMessage);
        TextView tvDate    = row.findViewById(R.id.tvDate);
        TextView tvStar    = row.findViewById(R.id.tvStar);
        View     divider   = row.findViewById(R.id.vDivider);

        avatar.setBackgroundResource(item.avatarResId);
        tvName.setText(item.senderName);
        tvMessage.setText(item.message);
        tvDate.setText(item.dateLabel);

        // Dim read items, bright for unread even in archive
        if (item.isRead) {
            tvName.setTextColor(Color.parseColor("#668899"));
            tvMessage.setTextColor(Color.parseColor("#446677"));
        } else {
            tvName.setTextColor(Color.WHITE);
            tvMessage.setTextColor(Color.parseColor("#AACCDD"));
        }

        // Star toggles — item stays in archive regardless
        applyStarColor(tvStar, item.isStarred);
        tvStar.setOnClickListener(v -> {
            if (item.isStarred) {
                NotificationStore.getInstance().unstar(item.id);
                item.isStarred = false;
            } else {
                NotificationStore.getInstance().star(item.id);
                item.isStarred = true;
            }
            applyStarColor(tvStar, item.isStarred);
        });

        // Tapping the row opens full notification detail
        // NotifActivity handles both archived and non-archived items
        row.setOnClickListener(v -> {
            Intent intent = new Intent(this, NotifActivity.class);
            intent.putExtra(NotifActivity.EXTRA_NOTIF_ID, item.id);
            startActivity(intent);
        });

        if (divider != null) {
            divider.setVisibility(showDivider ? View.VISIBLE : View.GONE);
        }

        return row;
    }

    private void applyStarColor(TextView tvStar, boolean starred) {
        tvStar.setTextColor(starred
                ? Color.parseColor("#FFB347")
                : Color.parseColor("#44AACCDD"));
    }
}
