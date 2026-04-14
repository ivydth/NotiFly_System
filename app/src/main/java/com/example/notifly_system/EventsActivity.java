package com.example.notifly_system;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class EventsActivity extends AppCompatActivity {

    // Top bar
    AppCompatImageView btnBack;
    AppCompatImageView btnSearch;
    TextView tvSectionLabel;

    // Search bar
    LinearLayout searchBarContainer;
    EditText etSearch;
    AppCompatImageView btnClearSearch;
    View searchDivider;

    // Filter row
    LinearLayout btnDateFilter;
    TextView tvDateFilterLabel;
    TextView btnClearDateFilter;
    TextView tvNotifCount;

    // Bottom nav
    AppCompatImageView ivHome, ivBell;
    TextView tvBellBadge;

    // Swipe refresh
    SwipeRefreshLayout swipeRefreshLayout;

    // Notification list container
    LinearLayout notifContainer;

    // Firebase
    FirebaseAuth mAuth;
    DatabaseReference notificationsRef;

    // State
    private final List<NotifItem> allNotifs = new ArrayList<>();
    private String activeSearchQuery = "";
    private String activeDateFilter = null; // "MMM d" formatted date string

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.events_activity); // update to your XML file name

        // ── INITIALIZE VIEWS ──────────────────────────────────────

        btnBack         = findViewById(R.id.btnBack);
        btnSearch       = findViewById(R.id.btnSearch);
        tvSectionLabel  = findViewById(R.id.tvSectionLabel);

        searchBarContainer = findViewById(R.id.searchBarContainer);
        etSearch           = findViewById(R.id.etSearch);
        btnClearSearch     = findViewById(R.id.btnClearSearch);
        searchDivider      = findViewById(R.id.searchDivider);

        btnDateFilter      = findViewById(R.id.btnDateFilter);
        tvDateFilterLabel  = findViewById(R.id.tvDateFilterLabel);
        btnClearDateFilter = findViewById(R.id.btnClearDateFilter);
        tvNotifCount       = findViewById(R.id.tvNotifCount);

        ivHome       = findViewById(R.id.ivHome);
        ivBell       = findViewById(R.id.ivBell);
        tvBellBadge  = findViewById(R.id.tvBellBadge);

        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        notifContainer     = findViewById(R.id.notifContainer);

        // ── FIREBASE ──────────────────────────────────────────────

        mAuth = FirebaseAuth.getInstance();
        notificationsRef = FirebaseDatabase.getInstance(
                "https://notifly-94dba-default-rtdb.asia-southeast1.firebasedatabase.app/"
        ).getReference("notifications");

        // ── BUTTON LISTENERS ──────────────────────────────────────

        btnBack.setOnClickListener(v -> finish());

        // Toggle search bar visibility
        btnSearch.setOnClickListener(v -> {
            boolean isVisible = searchBarContainer.getVisibility() == View.VISIBLE;
            searchBarContainer.setVisibility(isVisible ? View.GONE : View.VISIBLE);
            searchDivider.setVisibility(isVisible ? View.GONE : View.VISIBLE);
            if (isVisible) {
                etSearch.setText("");
                activeSearchQuery = "";
                renderFiltered();
            } else {
                etSearch.requestFocus();
            }
        });

        // Clear search field
        btnClearSearch.setOnClickListener(v -> {
            etSearch.setText("");
            activeSearchQuery = "";
            renderFiltered();
        });

        // Live search filtering
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                activeSearchQuery = s.toString().trim().toLowerCase(Locale.getDefault());
                btnClearSearch.setVisibility(activeSearchQuery.isEmpty() ? View.GONE : View.VISIBLE);
                renderFiltered();
            }
        });

        // Date filter chip
        btnDateFilter.setOnClickListener(v -> showDatePickerDialog());

        // Clear date filter
        btnClearDateFilter.setOnClickListener(v -> {
            activeDateFilter = null;
            tvDateFilterLabel.setText("Pick date");
            btnClearDateFilter.setVisibility(View.GONE);
            renderFiltered();
        });

        // Bottom nav
        ivHome.setOnClickListener(v -> {
            startActivity(new Intent(this, UserActivity.class));
            overridePendingTransition(0, 0);
        });

        ivBell.setOnClickListener(v -> {
            startActivity(new Intent(this, NotifActivity1.class));
            overridePendingTransition(0, 0);
        });

        // Swipe to refresh
        swipeRefreshLayout.setColorSchemeColors(0xFF00C9B1);
        swipeRefreshLayout.setOnRefreshListener(() -> {
            allNotifs.clear();
            loadNotifications();
        });

        // ── LOAD NOTIFICATIONS ────────────────────────────────────
        loadNotifications();
    }

    // ── DATE PICKER ───────────────────────────────────────────────

    private void showDatePickerDialog() {
        Calendar cal = Calendar.getInstance();
        new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            Calendar selected = Calendar.getInstance();
            selected.set(year, month, dayOfMonth);
            SimpleDateFormat sdf = new SimpleDateFormat("MMM d", Locale.getDefault());
            activeDateFilter = sdf.format(selected.getTime());
            tvDateFilterLabel.setText(activeDateFilter);
            btnClearDateFilter.setVisibility(View.VISIBLE);
            renderFiltered();
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
    }

    // ── FIREBASE LOAD ─────────────────────────────────────────────

    private void loadNotifications() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        notificationsRef.orderByChild("timestamp").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                allNotifs.clear();

                List<DataSnapshot> notifList = new ArrayList<>();
                for (DataSnapshot child : snapshot.getChildren()) {
                    notifList.add(0, child); // newest first
                }

                SimpleDateFormat sdf = new SimpleDateFormat("MMM d", Locale.getDefault());

                for (DataSnapshot notif : notifList) {
                    String title     = notif.child("title").getValue(String.class);
                    String body      = notif.child("body").getValue(String.class);
                    Long   timestamp = notif.child("timestamp").getValue(Long.class);

                    if (title == null) title = "Notification";
                    if (body  == null) body  = "";

                    String dateStr = "—";
                    if (timestamp != null) {
                        dateStr = sdf.format(new Date(timestamp));
                    }

                    allNotifs.add(new NotifItem(title, body, dateStr));
                }

                swipeRefreshLayout.setRefreshing(false);
                renderFiltered();
                updateBellBadge(allNotifs.size());
            }

            @Override
            public void onCancelled(DatabaseError error) {
                swipeRefreshLayout.setRefreshing(false);
                showErrorState(error.getMessage());
            }
        });
    }

    // ── FILTER + RENDER ───────────────────────────────────────────

    private void renderFiltered() {
        List<NotifItem> filtered = new ArrayList<>();

        for (NotifItem item : allNotifs) {
            // Date filter
            if (activeDateFilter != null && !item.date.equals(activeDateFilter)) continue;

            // Search filter
            if (!activeSearchQuery.isEmpty()) {
                boolean matchTitle = item.title.toLowerCase(Locale.getDefault()).contains(activeSearchQuery);
                boolean matchBody  = item.body.toLowerCase(Locale.getDefault()).contains(activeSearchQuery);
                if (!matchTitle && !matchBody) continue;
            }

            filtered.add(item);
        }

        notifContainer.removeAllViews();

        tvNotifCount.setText(filtered.isEmpty() ? "" : filtered.size() + " notification" + (filtered.size() == 1 ? "" : "s"));

        if (filtered.isEmpty()) {
            showEmptyState();
            return;
        }

        for (int i = 0; i < filtered.size(); i++) {
            NotifItem item = filtered.get(i);
            addNotifRow(item.title, item.body, item.date, i < filtered.size() - 1);
        }
    }

    // ── ROW BUILDER ───────────────────────────────────────────────

    private void addNotifRow(String title, String body, String date, boolean showDivider) {
        android.widget.RelativeLayout row = new android.widget.RelativeLayout(this);
        android.widget.RelativeLayout.LayoutParams rowParams =
                new android.widget.RelativeLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, dpToPx(70));
        row.setLayoutParams(rowParams);
        row.setPadding(dpToPx(12), 0, dpToPx(12), 0);

        // Avatar dot
        View avatar = new View(this);
        android.widget.RelativeLayout.LayoutParams avatarParams =
                new android.widget.RelativeLayout.LayoutParams(dpToPx(44), dpToPx(44));
        avatarParams.addRule(android.widget.RelativeLayout.ALIGN_PARENT_START);
        avatarParams.addRule(android.widget.RelativeLayout.CENTER_VERTICAL);
        avatar.setLayoutParams(avatarParams);
        avatar.setBackgroundResource(R.drawable.avatar_teal);
        avatar.setId(View.generateViewId());
        row.addView(avatar);

        // Text block
        LinearLayout textBlock = new LinearLayout(this);
        textBlock.setOrientation(LinearLayout.VERTICAL);
        textBlock.setGravity(android.view.Gravity.CENTER_VERTICAL);
        android.widget.RelativeLayout.LayoutParams textParams =
                new android.widget.RelativeLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT);
        textParams.addRule(android.widget.RelativeLayout.CENTER_VERTICAL);
        textParams.setMarginStart(dpToPx(56));
        textParams.setMarginEnd(dpToPx(52));
        textBlock.setLayoutParams(textParams);

        TextView tvTitle = new TextView(this);
        tvTitle.setText(title);
        tvTitle.setTextColor(0xFFFFFFFF);
        tvTitle.setTextSize(15);
        tvTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        tvTitle.setMaxLines(1);
        tvTitle.setEllipsize(TextUtils.TruncateAt.END);
        textBlock.addView(tvTitle);

        if (!body.isEmpty()) {
            TextView tvBody = new TextView(this);
            tvBody.setText(body);
            tvBody.setTextColor(0xFFAACCDD);
            tvBody.setTextSize(12);
            tvBody.setMaxLines(1);
            tvBody.setEllipsize(TextUtils.TruncateAt.END);
            LinearLayout.LayoutParams bodyParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            bodyParams.topMargin = dpToPx(2);
            tvBody.setLayoutParams(bodyParams);
            textBlock.addView(tvBody);
        }

        row.addView(textBlock);

        // Right column: date + dot
        LinearLayout rightCol = new LinearLayout(this);
        rightCol.setOrientation(LinearLayout.VERTICAL);
        rightCol.setGravity(android.view.Gravity.CENTER);
        android.widget.RelativeLayout.LayoutParams rightParams =
                new android.widget.RelativeLayout.LayoutParams(dpToPx(44), ViewGroup.LayoutParams.WRAP_CONTENT);
        rightParams.addRule(android.widget.RelativeLayout.ALIGN_PARENT_END);
        rightParams.addRule(android.widget.RelativeLayout.CENTER_VERTICAL);
        rightCol.setLayoutParams(rightParams);

        TextView tvDate = new TextView(this);
        tvDate.setText(date);
        tvDate.setTextColor(0xFFAACCDD);
        tvDate.setTextSize(10);
        tvDate.setGravity(android.view.Gravity.CENTER);
        rightCol.addView(tvDate);

        View dot = new View(this);
        LinearLayout.LayoutParams dotParams = new LinearLayout.LayoutParams(dpToPx(8), dpToPx(8));
        dotParams.topMargin = dpToPx(4);
        dotParams.gravity = android.view.Gravity.CENTER_HORIZONTAL;
        dot.setLayoutParams(dotParams);
        dot.setBackgroundResource(R.drawable.dot_blue);
        rightCol.addView(dot);

        row.addView(rightCol);
        notifContainer.addView(row);

        // Divider
        if (showDivider) {
            View divider = new View(this);
            LinearLayout.LayoutParams divParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, dpToPx(1));
            divParams.setMarginStart(dpToPx(68));
            divider.setLayoutParams(divParams);
            divider.setBackgroundColor(0x22FFFFFF);
            notifContainer.addView(divider);
        }
    }

    // ── BELL BADGE ────────────────────────────────────────────────

    private void updateBellBadge(int count) {
        if (count > 0) {
            tvBellBadge.setVisibility(View.VISIBLE);
            tvBellBadge.setText(count > 99 ? "99+" : String.valueOf(count));
        } else {
            tvBellBadge.setVisibility(View.GONE);
        }
    }

    // ── EMPTY / ERROR STATES ──────────────────────────────────────

    private void showEmptyState() {
        TextView empty = new TextView(this);
        empty.setText("No Announcements notifications.");
        empty.setTextColor(0xFFAACCDD);
        empty.setTextSize(14);
        empty.setGravity(android.view.Gravity.CENTER);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dpToPx(120));
        empty.setLayoutParams(p);
        notifContainer.addView(empty);
    }

    private void showErrorState(String msg) {
        TextView err = new TextView(this);
        err.setText("Error loading notifications:\n" + msg);
        err.setTextColor(0xFFFF6B6B);
        err.setTextSize(13);
        err.setGravity(android.view.Gravity.CENTER);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dpToPx(120));
        err.setLayoutParams(p);
        notifContainer.addView(err);
    }

    // ── DATA MODEL ────────────────────────────────────────────────

    private static class NotifItem {
        String title, body, date;
        NotifItem(String title, String body, String date) {
            this.title = title;
            this.body  = body;
            this.date  = date;
        }
    }

    // ── LIFECYCLE ─────────────────────────────────────────────────

    @Override
    protected void onResume() {
        super.onResume();
        setOnlineStatus(true);
    }

    @Override
    protected void onPause() {
        super.onPause();
        setOnlineStatus(false);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (mAuth.getCurrentUser() == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        }
    }

    private void setOnlineStatus(boolean isOnline) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) return;
        DatabaseReference presenceRef = FirebaseDatabase.getInstance(
                "https://notifly-94dba-default-rtdb.asia-southeast1.firebasedatabase.app/"
        ).getReference("users").child(currentUser.getUid()).child("online");
        presenceRef.setValue(isOnline);
        if (isOnline) presenceRef.onDisconnect().setValue(false);
    }

    // ── UTILITY ───────────────────────────────────────────────────

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }
}
