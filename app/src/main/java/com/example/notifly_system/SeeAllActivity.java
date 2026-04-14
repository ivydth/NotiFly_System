package com.example.notifly_system;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

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

public class SeeAllActivity extends AppCompatActivity
        implements NotificationStore.StoreListener {

    public static final String EXTRA_CATEGORY = "extra_category";

    // ── Views ─────────────────────────────────────────────────────────────────

    private AppCompatImageView btnMenu;
    private TextView           btnProfile;
    private AppCompatImageView ivHome, ivSearch, ivBell;
    private TextView           tvSectionLabel;
    private LinearLayout       notifContainer;
    private SwipeRefreshLayout swipeRefreshLayout;

    // ── State ─────────────────────────────────────────────────────────────────

    private String            category = "All";
    private DatabaseReference notificationsRef;

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_star);

        category = getIntent().getStringExtra(EXTRA_CATEGORY);
        if (category == null) category = "All";

        bindViews();
        setupSectionLabel();
        setupSwipeRefresh();
        setupClickListeners();

        notificationsRef = FirebaseDatabase.getInstance(
                "https://notifly-94dba-default-rtdb.asia-southeast1.firebasedatabase.app/"
        ).getReference("notifications");
    }

    @Override
    protected void onResume() {
        super.onResume();
        NotificationStore.getInstance().addListener(this);
        renderFromStore();
    }

    @Override
    protected void onPause() {
        super.onPause();
        NotificationStore.getInstance().removeListener(this);
    }

    // ── StoreListener ─────────────────────────────────────────────────────────

    @Override
    public void onStoreChanged() {
        runOnUiThread(this::renderFromStore);
    }

    // ── Bind views ────────────────────────────────────────────────────────────

    private void bindViews() {
        btnMenu            = findViewById(R.id.btnMenu);
        btnProfile         = findViewById(R.id.btnProfile);
        ivHome             = findViewById(R.id.ivHome);
        ivSearch           = findViewById(R.id.ivSearch);
        ivBell             = findViewById(R.id.ivBell);
        tvSectionLabel     = findViewById(R.id.tvSectionLabel);
        notifContainer     = findViewById(R.id.notifContainer);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
    }

    private void setupSectionLabel() {
        if (tvSectionLabel != null) tvSectionLabel.setText(category.toUpperCase());
    }

    // ── Swipe refresh ─────────────────────────────────────────────────────────

    private void setupSwipeRefresh() {
        if (swipeRefreshLayout == null) return;
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

    // ── Pull-to-refresh ───────────────────────────────────────────────────────

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

            String cat       = mapTargetToCategory(target, topicOrUser);
            String dateLabel = formatTimestamp(ts);

            NotificationItem item = new NotificationItem(
                    id, title, body, dateLabel, cat, false, R.drawable.avatar_teal);
            if (ts != null) item.timestamp = ts;
            incoming.add(item);
        }
        incoming.sort((a, b) -> Long.compare(b.timestamp, a.timestamp));
        return incoming;
    }

    // ── Click listeners ───────────────────────────────────────────────────────

    private void setupClickListeners() {
        if (btnMenu    != null) btnMenu.setOnClickListener(v -> {
            startActivity(new Intent(this, UserMenu.class));
            overridePendingTransition(0, 0);
        });
        if (btnProfile != null) btnProfile.setOnClickListener(v ->
                startActivity(new Intent(this, ProfileActivity.class)));
        if (ivHome     != null) ivHome.setOnClickListener(v -> {
            startActivity(new Intent(this, UserActivity.class));
            finish();
        });
        if (ivSearch   != null) ivSearch.setOnClickListener(v -> { /* TODO */ });
        if (ivBell     != null) ivBell.setOnClickListener(v -> {
            NotificationStore.getInstance().markAllSeen();
            startActivity(new Intent(this, NotifActivity1.class));
        });
    }

    // ── Render from NotificationStore ─────────────────────────────────────────

    /**
     * Reads the correct subset from the store based on category passed from
     * UserActivity and builds rows the same way NotifActivity1 does —
     * programmatically into a LinearLayout so the visual style is identical.
     */
    private void renderFromStore() {
        if (notifContainer == null) return;
        notifContainer.removeAllViews();

        NotificationStore store = NotificationStore.getInstance();
        List<NotificationItem> items;

        if (category.equalsIgnoreCase("All")) {
            items = store.getAll();
        } else if (category.equalsIgnoreCase("Starred")) {
            items = store.getStarred();
        } else if (category.equalsIgnoreCase("Unread")) {
            items = store.getUnread();
        } else {
            items = store.getByCategory(category);
        }

        if (items.isEmpty()) {
            showEmptyState();
            return;
        }

        for (int i = 0; i < items.size(); i++) {
            notifContainer.addView(buildRow(items.get(i), i < items.size() - 1));
        }
    }

    // ── Row builder — identical visual style to NotifActivity1 ────────────────

    private View buildRow(NotificationItem item, boolean showDivider) {
        LinearLayout wrapper = new LinearLayout(this);
        wrapper.setOrientation(LinearLayout.VERTICAL);
        wrapper.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        // ── Row ──────────────────────────────────────────────────────────────
        RelativeLayout row = new RelativeLayout(this);
        row.setLayoutParams(new RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dpToPx(70)));
        row.setPadding(dpToPx(12), 0, dpToPx(12), 0);
        row.setClickable(true);
        row.setFocusable(true);
        int[] attrs = new int[]{android.R.attr.selectableItemBackground};
        row.setBackground(obtainStyledAttributes(attrs).getDrawable(0));

        // Avatar
        View avatar = new View(this);
        RelativeLayout.LayoutParams avatarParams = new RelativeLayout.LayoutParams(
                dpToPx(44), dpToPx(44));
        avatarParams.addRule(RelativeLayout.ALIGN_PARENT_START);
        avatarParams.addRule(RelativeLayout.CENTER_VERTICAL);
        avatar.setLayoutParams(avatarParams);
        avatar.setBackgroundResource(R.drawable.avatar_teal);
        avatar.setId(View.generateViewId());
        row.addView(avatar);

        // Text block
        LinearLayout textBlock = new LinearLayout(this);
        textBlock.setOrientation(LinearLayout.VERTICAL);
        textBlock.setGravity(Gravity.CENTER_VERTICAL);
        RelativeLayout.LayoutParams textParams = new RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT);
        textParams.addRule(RelativeLayout.CENTER_VERTICAL);
        textParams.setMarginStart(dpToPx(56));
        textParams.setMarginEnd(dpToPx(52));
        textBlock.setLayoutParams(textParams);

        // Title row (sender name)
        TextView tvTitle = new TextView(this);
        tvTitle.setText(item.senderName != null && !item.senderName.isEmpty()
                ? item.senderName : "Notification");
        tvTitle.setTextColor(item.isRead
                ? Color.parseColor("#668899") : Color.WHITE);
        tvTitle.setTextSize(15);
        tvTitle.setTypeface(null, Typeface.BOLD);
        tvTitle.setMaxLines(1);
        tvTitle.setEllipsize(TextUtils.TruncateAt.END);
        textBlock.addView(tvTitle);

        // Body / message preview
        if (item.message != null && !item.message.isEmpty()) {
            TextView tvBody = new TextView(this);
            tvBody.setText(item.message);
            tvBody.setTextColor(item.isRead
                    ? Color.parseColor("#446677") : Color.parseColor("#AACCDD"));
            tvBody.setTextSize(12);
            tvBody.setMaxLines(1);
            tvBody.setEllipsize(TextUtils.TruncateAt.END);
            LinearLayout.LayoutParams bodyParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            bodyParams.topMargin = dpToPx(2);
            tvBody.setLayoutParams(bodyParams);
            textBlock.addView(tvBody);
        }
        row.addView(textBlock);

        // Right column — date + new dot
        LinearLayout rightCol = new LinearLayout(this);
        rightCol.setOrientation(LinearLayout.VERTICAL);
        rightCol.setGravity(Gravity.CENTER);
        RelativeLayout.LayoutParams rightParams = new RelativeLayout.LayoutParams(
                dpToPx(44), ViewGroup.LayoutParams.WRAP_CONTENT);
        rightParams.addRule(RelativeLayout.ALIGN_PARENT_END);
        rightParams.addRule(RelativeLayout.CENTER_VERTICAL);
        rightCol.setLayoutParams(rightParams);

        TextView tvDate = new TextView(this);
        tvDate.setText(item.dateLabel != null ? item.dateLabel : "—");
        tvDate.setTextColor(Color.parseColor("#AACCDD"));
        tvDate.setTextSize(10);
        tvDate.setGravity(Gravity.CENTER);
        rightCol.addView(tvDate);

        // Blue dot — only for notifications the user hasn't seen yet
        if (NotificationStore.getInstance().isNew(item.id)) {
            View dot = new View(this);
            LinearLayout.LayoutParams dotParams =
                    new LinearLayout.LayoutParams(dpToPx(8), dpToPx(8));
            dotParams.topMargin = dpToPx(4);
            dotParams.gravity = Gravity.CENTER_HORIZONTAL;
            dot.setLayoutParams(dotParams);
            dot.setBackgroundResource(R.drawable.dot_blue);
            rightCol.addView(dot);
        }

        row.addView(rightCol);

        // Tap row → mark read + open detail
        row.setOnClickListener(v -> {
            NotificationStore.getInstance().markRead(item.id);
            Intent intent = new Intent(this, NotifActivity.class);
            intent.putExtra(NotifActivity.EXTRA_NOTIF_ID, item.id);
            startActivity(intent);
        });

        wrapper.addView(row);

        // Divider between rows
        if (showDivider) {
            View divider = new View(this);
            LinearLayout.LayoutParams divParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, dpToPx(1));
            divParams.setMarginStart(dpToPx(68));
            divider.setLayoutParams(divParams);
            divider.setBackgroundColor(Color.parseColor("#22FFFFFF"));
            wrapper.addView(divider);
        }

        return wrapper;
    }

    // ── Empty state ───────────────────────────────────────────────────────────

    private void showEmptyState() {
        TextView empty = new TextView(this);
        empty.setText("No notifications here.");
        empty.setTextColor(Color.parseColor("#AACCDD"));
        empty.setTextSize(14);
        empty.setGravity(Gravity.CENTER);
        notifContainer.addView(empty, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dpToPx(120)));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String mapTargetToCategory(String target, String topicOrUser) {
        if (target == null) return "Unread";
        switch (target.toLowerCase()) {
            case "all":   return "Announcements";
            case "topic":
                if (topicOrUser != null) {
                    if (topicOrUser.equalsIgnoreCase("announcements")) return "Announcements";
                    if (topicOrUser.equalsIgnoreCase("events"))        return "Events";
                }
                return "Announcements";
            case "single": return "Unread";
            default:       return "Unread";
        }
    }

    private String formatTimestamp(Long ts) {
        if (ts == null || ts == 0) return "Now";
        try {
            return new SimpleDateFormat("MMM d", Locale.getDefault()).format(new Date(ts));
        } catch (Exception e) { return "Now"; }
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }
}
