package com.example.notifly_system;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class EventsActivity extends AppCompatActivity
        implements NotificationStore.StoreListener {

    // ── Views ─────────────────────────────────────────────────────────────────

    private AppCompatImageView btnMenu;
    private AppCompatImageView btnProfile;
    private AppCompatImageView ivHome;
    private AppCompatImageView ivSearch;
    private AppCompatImageView ivBell;
    private TextView           tvBellBadge;
    private SwipeRefreshLayout swipeRefreshLayout;
    private LinearLayout       notificationsContainer;
    private TextView           tvEmptyState;

    // ── Firebase ──────────────────────────────────────────────────────────────

    private DatabaseReference notificationsRef;

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.events_activity);

        initViews();
        initFirebase();
        setListeners();
    }

    @Override
    protected void onResume() {
        super.onResume();
        NotificationStore.getInstance().addListener(this);
        refreshAll();
    }

    @Override
    protected void onPause() {
        super.onPause();
        NotificationStore.getInstance().removeListener(this);
    }

    // ── Init ──────────────────────────────────────────────────────────────────

    private void initViews() {
        btnMenu                = findViewById(R.id.btnMenu);
        btnProfile             = findViewById(R.id.btnProfile);
        ivHome                 = findViewById(R.id.ivHome);
        ivSearch               = findViewById(R.id.ivSearch);
        ivBell                 = findViewById(R.id.ivBell);
        tvBellBadge            = findViewById(R.id.tvBellBadge);
        swipeRefreshLayout     = findViewById(R.id.swipeRefreshLayout);
        notificationsContainer = findViewById(R.id.notificationsContainer);
        tvEmptyState           = findViewById(R.id.tvEmptyState);
    }

    private void initFirebase() {
        notificationsRef = FirebaseDatabase.getInstance(
                "https://notifly-94dba-default-rtdb.asia-southeast1.firebasedatabase.app/"
        ).getReference("notifications");
    }

    // ── Listeners ─────────────────────────────────────────────────────────────

    private void setListeners() {
        if (btnMenu != null) {
            btnMenu.setOnClickListener(v -> {
                startActivity(new Intent(this, UserMenu.class));
                overridePendingTransition(0, 0);
            });
        }

        if (btnProfile != null) {
            btnProfile.setOnClickListener(v ->
                    startActivity(new Intent(this, ProfileActivity.class)));
        }

        if (ivHome != null) {
            ivHome.setOnClickListener(v ->
                    startActivity(new Intent(this, UserActivity.class)));
        }

        if (ivSearch != null) {
            ivSearch.setOnClickListener(v -> {
                // TODO: Open search screen
            });
        }

        if (ivBell != null) {
            ivBell.setOnClickListener(v -> {
                NotificationStore.getInstance().markAllSeen();
                refreshBellBadge();
                startActivity(new Intent(this, NotifActivity1.class));
            });
        }

        // ── FIX: guard swipeRefreshLayout — it is null when the view is
        //         missing from events_activity.xml. Add the SwipeRefreshLayout
        //         to your layout to re-enable pull-to-refresh. ──────────────
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setColorSchemeColors(
                    Color.parseColor("#00C9B1"),
                    Color.parseColor("#5BB8FF"),
                    Color.parseColor("#C084FC")
            );
            swipeRefreshLayout.setProgressBackgroundColorSchemeColor(
                    Color.parseColor("#1E3A4A")
            );
            swipeRefreshLayout.setOnRefreshListener(this::fetchAndApplyOnRefresh);
        }
    }

    // ── StoreListener ─────────────────────────────────────────────────────────

    @Override
    public void onStoreChanged() {
        runOnUiThread(this::refreshAll);
    }

    // ── Pull-to-refresh fetch ─────────────────────────────────────────────────

    private void fetchAndApplyOnRefresh() {
        notificationsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                NotificationStore store = NotificationStore.getInstance();
                store.syncFromFirebase(parseSnapshot(snapshot));
                store.applyPending();
                if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);
            }

            @Override
            public void onCancelled(DatabaseError error) {
                if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);
                Toast.makeText(EventsActivity.this,
                        "Failed to refresh.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ── Parse Firebase snapshot ───────────────────────────────────────────────

    private List<NotificationItem> parseSnapshot(DataSnapshot snapshot) {
        List<NotificationItem> incoming = new ArrayList<>();

        for (DataSnapshot child : snapshot.getChildren()) {
            String id          = child.getKey();
            String title       = child.child("title").getValue(String.class);
            String body        = child.child("body").getValue(String.class);
            String target      = child.child("target").getValue(String.class);
            String topicOrUser = child.child("topicOrUser").getValue(String.class);
            Long   ts          = child.child("timestamp").getValue(Long.class);

            if (title == null) title = "Notification";
            if (body  == null) body  = "";

            String category  = mapTargetToCategory(target, topicOrUser);
            String dateLabel = formatTimestamp(ts);

            NotificationItem item = new NotificationItem(
                    id, title, body, dateLabel, category, false,
                    R.drawable.avatar_teal
            );
            if (ts != null) item.timestamp = ts;
            incoming.add(item);
        }

        incoming.sort((a, b) -> Long.compare(b.timestamp, a.timestamp));
        return incoming;
    }

    // ── Refresh UI ────────────────────────────────────────────────────────────

    private void refreshAll() {
        refreshBellBadge();
        showEvents();
    }

    private void refreshBellBadge() {
        if (tvBellBadge == null) return;
        int count = NotificationStore.getInstance().getNewCount();
        tvBellBadge.setVisibility(count > 0 ? View.VISIBLE : View.GONE);
        if (count > 0) tvBellBadge.setText(count > 99 ? "99+" : String.valueOf(count));
    }

    // ── Render event rows ─────────────────────────────────────────────────────

    private void showEvents() {
        if (notificationsContainer == null) return;
        notificationsContainer.removeAllViews();

        List<NotificationItem> items =
                NotificationStore.getInstance().getByCategory("Events");

        if (items.isEmpty()) {
            if (tvEmptyState != null) {
                tvEmptyState.setVisibility(View.VISIBLE);
                tvEmptyState.setText("No events yet");
            }
            return;
        }

        if (tvEmptyState != null) tvEmptyState.setVisibility(View.GONE);

        for (int i = 0; i < items.size(); i++) {
            NotificationItem item = items.get(i);
            notificationsContainer.addView(buildNotificationRow(item));

            if (i < items.size() - 1) {
                notificationsContainer.addView(buildDivider());
            }
        }
    }

    // ── Build row ─────────────────────────────────────────────────────────────

    private View buildNotificationRow(NotificationItem item) {
        View row = LayoutInflater.from(this)
                .inflate(R.layout.item_notification_row, notificationsContainer, false);

        TextView tvAvatar  = row.findViewById(R.id.tvAvatar);
        TextView tvName    = row.findViewById(R.id.tvSenderName);
        TextView tvMessage = row.findViewById(R.id.tvMessage);
        TextView tvDate    = row.findViewById(R.id.tvDate);
        TextView tvStar    = row.findViewById(R.id.tvStar);
        View     vNewDot   = row.findViewById(R.id.vNewDot);

        if (tvAvatar != null) {
            String name = (item.senderName != null && !item.senderName.isEmpty())
                    ? item.senderName : "N";
            tvAvatar.setText(String.valueOf(name.charAt(0)).toUpperCase());
        }

        if (tvName    != null) tvName.setText(item.senderName);
        if (tvMessage != null) tvMessage.setText(item.message);
        if (tvDate    != null) tvDate.setText(item.dateLabel);

        if (vNewDot != null) {
            boolean isNew = NotificationStore.getInstance().isNew(item.id);
            vNewDot.setVisibility(isNew ? View.VISIBLE : View.INVISIBLE);
            if (isNew) startGlowPulse(vNewDot);
        }

        applyReadStyle(tvName, tvMessage, item.isRead);

        if (tvStar != null) {
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
        }

        row.setOnClickListener(v -> {
            Intent intent = new Intent(this, NotifActivity.class);
            intent.putExtra(NotifActivity.EXTRA_NOTIF_ID, item.id);
            startActivity(intent);
        });

        return row;
    }

    private View buildDivider() {
        View divider = new View(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 1);
        params.setMarginStart((int) (68 * getResources().getDisplayMetrics().density));
        divider.setLayoutParams(params);
        divider.setBackgroundColor(Color.parseColor("#22FFFFFF"));
        return divider;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void startGlowPulse(View dot) {
        AlphaAnimation pulse = new AlphaAnimation(1f, 0.2f);
        pulse.setDuration(900);
        pulse.setRepeatMode(Animation.REVERSE);
        pulse.setRepeatCount(Animation.INFINITE);
        dot.startAnimation(pulse);
    }

    private void applyReadStyle(TextView tvName, TextView tvMessage, boolean isRead) {
        if (tvName == null || tvMessage == null) return;
        if (isRead) {
            tvName.setTextColor(Color.parseColor("#668899"));
            tvMessage.setTextColor(Color.parseColor("#446677"));
        } else {
            tvName.setTextColor(Color.WHITE);
            tvMessage.setTextColor(Color.parseColor("#AACCDD"));
        }
    }

    private void applyStarColor(TextView tvStar, boolean starred) {
        tvStar.setTextColor(starred
                ? Color.parseColor("#FFB347")
                : Color.parseColor("#44AACCDD"));
    }

    private String mapTargetToCategory(String target, String topicOrUser) {
        if (target == null) return "Unread";
        switch (target.toLowerCase()) {
            case "all":
                return "Announcements";
            case "topic":
                if (topicOrUser == null) return "Unread";
                if (topicOrUser.equalsIgnoreCase("announcements")) return "Announcements";
                if (topicOrUser.equalsIgnoreCase("events"))        return "Events";
                return "Unread";
            case "single":
                return "Unread";
            default:
                return "Unread";
        }
    }

    private String formatTimestamp(Long ts) {
        if (ts == null || ts == 0) return "Now";
        try {
            return new SimpleDateFormat("MMM d", Locale.getDefault()).format(new Date(ts));
        } catch (Exception e) {
            return "Now";
        }
    }
}
