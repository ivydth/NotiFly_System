package com.example.notifly_system;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
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

public class NotifActivity1 extends AppCompatActivity {

    // ── Views ─────────────────────────────────────────────────────────────────

    AppCompatImageView btnMenu;
    TextView           btnProfile;
    AppCompatImageView ivHome, ivBell;      // ivSearch removed

    AppCompatImageView btnSearch;
    LinearLayout       searchBarContainer;
    EditText           etSearch;
    AppCompatImageView btnClearSearch;
    android.view.View  searchDivider;

    LinearLayout       btnDateFilter;
    TextView           tvDateFilterLabel;
    TextView           btnClearDateFilter;
    TextView           tvNotifCount;

    LinearLayout       notifContainer;
    SwipeRefreshLayout swipeRefreshLayout;

    // ── Firebase ──────────────────────────────────────────────────────────────

    FirebaseAuth      mAuth;
    DatabaseReference usersRef;
    DatabaseReference notificationsRef;

    // ── State ─────────────────────────────────────────────────────────────────

    /** Full list fetched from Firebase — filters are applied on top of this. */
    private final List<NotifRow> allRows = new ArrayList<>();

    private String   searchQuery  = "";
    private Calendar selectedDate = null;
    private boolean  searchOpen   = false;

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.userdbtotalnotif_activity);

        btnMenu            = findViewById(R.id.btnMenu);
        ivHome             = findViewById(R.id.ivHome);
        ivBell             = findViewById(R.id.ivBell);
        notifContainer     = findViewById(R.id.notifContainer);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);

        btnSearch          = findViewById(R.id.btnSearch);
        searchBarContainer = findViewById(R.id.searchBarContainer);
        etSearch           = findViewById(R.id.etSearch);
        btnClearSearch     = findViewById(R.id.btnClearSearch);
        searchDivider      = findViewById(R.id.searchDivider);

        btnDateFilter      = findViewById(R.id.btnDateFilter);
        tvDateFilterLabel  = findViewById(R.id.tvDateFilterLabel);
        btnClearDateFilter = findViewById(R.id.btnClearDateFilter);
        tvNotifCount       = findViewById(R.id.tvNotifCount);

        mAuth = FirebaseAuth.getInstance();
        usersRef = FirebaseDatabase.getInstance(
                "https://notifly-94dba-default-rtdb.asia-southeast1.firebasedatabase.app/"
        ).getReference("users");
        notificationsRef = FirebaseDatabase.getInstance(
                "https://notifly-94dba-default-rtdb.asia-southeast1.firebasedatabase.app/"
        ).getReference("notifications");

        setupSwipeRefresh();
        setupSearchBar();
        setupDateFilter();
        setupClickListeners();

        fetchAndRenderNow();
    }

    // ── Swipe refresh ─────────────────────────────────────────────────────────

    private void setupSwipeRefresh() {
        if (swipeRefreshLayout == null) return;
        swipeRefreshLayout.setColorSchemeColors(
                android.graphics.Color.parseColor("#00C9B1"),
                android.graphics.Color.parseColor("#5BB8FF"),
                android.graphics.Color.parseColor("#C084FC")
        );
        swipeRefreshLayout.setProgressBackgroundColorSchemeColor(
                android.graphics.Color.parseColor("#1E3A4A")
        );
        swipeRefreshLayout.setOnRefreshListener(this::fetchAndRenderOnRefresh);
    }

    // ── Search bar ────────────────────────────────────────────────────────────

    private void setupSearchBar() {
        if (etSearch == null) return;

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void afterTextChanged(Editable s) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchQuery = s.toString().trim();
                if (btnClearSearch != null)
                    btnClearSearch.setVisibility(
                            searchQuery.isEmpty() ? android.view.View.GONE : android.view.View.VISIBLE);
                applyFiltersAndRender();
            }
        });

        etSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) { hideKeyboard(); return true; }
            return false;
        });

        if (btnClearSearch != null) {
            btnClearSearch.setOnClickListener(v -> {
                etSearch.setText("");
                searchQuery = "";
                applyFiltersAndRender();
            });
        }
    }

    private void toggleSearchBar() {
        searchOpen = !searchOpen;
        if (searchBarContainer == null || searchDivider == null) return;
        searchBarContainer.setVisibility(searchOpen ? android.view.View.VISIBLE : android.view.View.GONE);
        searchDivider.setVisibility(searchOpen ? android.view.View.VISIBLE : android.view.View.GONE);

        if (searchOpen) {
            etSearch.requestFocus();
            InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            if (imm != null) imm.showSoftInput(etSearch, InputMethodManager.SHOW_IMPLICIT);
        } else {
            if (etSearch != null) etSearch.setText("");
            searchQuery = "";
            hideKeyboard();
            applyFiltersAndRender();
        }
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null && etSearch != null)
            imm.hideSoftInputFromWindow(etSearch.getWindowToken(), 0);
    }

    // ── Date filter ───────────────────────────────────────────────────────────

    private void setupDateFilter() {
        if (btnDateFilter == null) return;
        btnDateFilter.setOnClickListener(v -> showDatePicker());

        if (btnClearDateFilter != null) {
            btnClearDateFilter.setOnClickListener(v -> {
                selectedDate = null;
                tvDateFilterLabel.setText("Pick date");
                btnClearDateFilter.setVisibility(android.view.View.GONE);
                applyFiltersAndRender();
            });
        }
    }

    private void showDatePicker() {
        Calendar initial = selectedDate != null ? selectedDate : Calendar.getInstance();
        new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    selectedDate = Calendar.getInstance();
                    selectedDate.set(year, month, dayOfMonth, 0, 0, 0);
                    selectedDate.set(Calendar.MILLISECOND, 0);

                    String label = new SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
                            .format(selectedDate.getTime());
                    if (tvDateFilterLabel != null) tvDateFilterLabel.setText(label);
                    if (btnClearDateFilter != null)
                        btnClearDateFilter.setVisibility(android.view.View.VISIBLE);

                    applyFiltersAndRender();
                },
                initial.get(Calendar.YEAR),
                initial.get(Calendar.MONTH),
                initial.get(Calendar.DAY_OF_MONTH)
        ).show();
    }

    // ── Click listeners ───────────────────────────────────────────────────────

    private void setupClickListeners() {
        if (btnMenu != null) btnMenu.setOnClickListener(v -> {
            startActivity(new Intent(this, UserMenu.class));
            overridePendingTransition(0, 0);
        });

        if (btnSearch != null) btnSearch.setOnClickListener(v -> toggleSearchBar());

        if (ivHome != null) ivHome.setOnClickListener(v -> {
            startActivity(new Intent(this, UserActivity.class));
            overridePendingTransition(0, 0);
        });

        if (ivBell != null) ivBell.setOnClickListener(v -> { /* already on this screen */ });

        loadProfileAvatar();
    }

    // ── Firebase fetches ──────────────────────────────────────────────────────

    private void fetchAndRenderNow() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) { startActivity(new Intent(this, LoginActivity.class)); finish(); return; }
        if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(true);

        notificationsRef.orderByChild("timestamp")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        storeAndRender(snapshot);
                        if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);
                    }
                    @Override
                    public void onCancelled(DatabaseError error) {
                        if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);
                        showErrorState(error.getMessage());
                    }
                });
    }

    private void fetchAndRenderOnRefresh() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) { startActivity(new Intent(this, LoginActivity.class)); finish(); return; }

        notificationsRef.orderByChild("timestamp")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        storeAndRender(snapshot);
                        if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);
                    }
                    @Override
                    public void onCancelled(DatabaseError error) {
                        if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);
                        showErrorState(error.getMessage());
                    }
                });
    }

    // ── Parse + store raw data ────────────────────────────────────────────────

    /**
     * Converts a Firebase snapshot into the allRows list, then applies filters.
     * Stored as plain NotifRow objects so we can re-filter without re-fetching.
     */
    private void storeAndRender(DataSnapshot snapshot) {
        allRows.clear();
        List<DataSnapshot> snapshotList = new ArrayList<>();
        for (DataSnapshot child : snapshot.getChildren()) {
            snapshotList.add(0, child); // newest first
        }
        for (DataSnapshot notif : snapshotList) {
            String title     = notif.child("title").getValue(String.class);
            String body      = notif.child("body").getValue(String.class);
            Long   timestamp = notif.child("timestamp").getValue(Long.class);

            if (title == null) title = "Notification";
            if (body  == null) body  = "";

            String dateStr = "—";
            if (timestamp != null)
                dateStr = new SimpleDateFormat("MMM d", Locale.getDefault())
                        .format(new Date(timestamp));

            allRows.add(new NotifRow(title, body, dateStr, timestamp != null ? timestamp : 0L));
        }
        applyFiltersAndRender();
    }

    // ── Filtering ─────────────────────────────────────────────────────────────

    /**
     * Applies the active search query and/or date filter to allRows,
     * then rebuilds the notifContainer view.
     */
    private void applyFiltersAndRender() {
        List<NotifRow> filtered = new ArrayList<>(allRows);

        // Date filter — keep only rows whose timestamp is on the selected day
        if (selectedDate != null) {
            Calendar dayStart = (Calendar) selectedDate.clone();
            dayStart.set(Calendar.HOUR_OF_DAY, 0);
            dayStart.set(Calendar.MINUTE, 0);
            dayStart.set(Calendar.SECOND, 0);
            dayStart.set(Calendar.MILLISECOND, 0);
            Calendar dayEnd = (Calendar) dayStart.clone();
            dayEnd.add(Calendar.DAY_OF_MONTH, 1);
            long start = dayStart.getTimeInMillis();
            long end   = dayEnd.getTimeInMillis();

            List<NotifRow> dateFiltered = new ArrayList<>();
            for (NotifRow row : filtered) {
                if (row.timestamp >= start && row.timestamp < end) dateFiltered.add(row);
            }
            filtered = dateFiltered;
        }

        // Search filter — match title (case-insensitive)
        if (!searchQuery.isEmpty()) {
            String lower = searchQuery.toLowerCase(Locale.getDefault());
            List<NotifRow> searched = new ArrayList<>();
            for (NotifRow row : filtered) {
                if (row.title.toLowerCase(Locale.getDefault()).contains(lower)) searched.add(row);
            }
            filtered = searched;
        }

        // Update count label
        if (tvNotifCount != null) {
            int c = filtered.size();
            tvNotifCount.setText(c == 0 ? "" : c + " notification" + (c == 1 ? "" : "s"));
        }

        // Render
        notifContainer.removeAllViews();
        if (filtered.isEmpty()) {
            showEmptyState();
            return;
        }
        for (int i = 0; i < filtered.size(); i++) {
            addNotifRow(filtered.get(i).title, filtered.get(i).body,
                    filtered.get(i).dateStr, i < filtered.size() - 1);
        }
    }

    // ── Profile avatar ────────────────────────────────────────────────────────

    private void loadProfileAvatar() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) return;
        usersRef.child(currentUser.getUid())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        String username  = snapshot.child("username").getValue(String.class);
                        String firstName = snapshot.child("firstName").getValue(String.class);
                        String displayName;
                        if (username != null && !username.isEmpty()) displayName = username;
                        else if (firstName != null && !firstName.isEmpty()) displayName = firstName;
                        else displayName = "U";
                        if (btnProfile != null)
                            btnProfile.setText(String.valueOf(displayName.charAt(0)).toUpperCase());
                    }
                    @Override
                    public void onCancelled(DatabaseError error) {
                        if (btnProfile != null) btnProfile.setText("U");
                    }
                });
    }

    // ── Row builder ───────────────────────────────────────────────────────────

    private void addNotifRow(String title, String body, String date, boolean showDivider) {
        android.widget.RelativeLayout row = new android.widget.RelativeLayout(this);
        android.widget.RelativeLayout.LayoutParams rowParams =
                new android.widget.RelativeLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, dpToPx(70));
        row.setLayoutParams(rowParams);
        row.setPadding(dpToPx(12), 0, dpToPx(12), 0);

        // Avatar
        android.view.View avatar = new android.view.View(this);
        android.widget.RelativeLayout.LayoutParams avatarParams =
                new android.widget.RelativeLayout.LayoutParams(dpToPx(44), dpToPx(44));
        avatarParams.addRule(android.widget.RelativeLayout.ALIGN_PARENT_START);
        avatarParams.addRule(android.widget.RelativeLayout.CENTER_VERTICAL);
        avatar.setLayoutParams(avatarParams);
        avatar.setBackgroundResource(R.drawable.avatar_teal);
        avatar.setId(android.view.View.generateViewId());
        row.addView(avatar);

        // Text block
        LinearLayout textBlock = new LinearLayout(this);
        textBlock.setOrientation(LinearLayout.VERTICAL);
        textBlock.setGravity(android.view.Gravity.CENTER_VERTICAL);
        android.widget.RelativeLayout.LayoutParams textParams =
                new android.widget.RelativeLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
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
        tvTitle.setEllipsize(android.text.TextUtils.TruncateAt.END);
        textBlock.addView(tvTitle);

        if (!body.isEmpty()) {
            TextView tvBody = new TextView(this);
            tvBody.setText(body);
            tvBody.setTextColor(0xFFAACCDD);
            tvBody.setTextSize(12);
            tvBody.setMaxLines(1);
            tvBody.setEllipsize(android.text.TextUtils.TruncateAt.END);
            LinearLayout.LayoutParams bodyParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            bodyParams.topMargin = dpToPx(2);
            tvBody.setLayoutParams(bodyParams);
            textBlock.addView(tvBody);
        }
        row.addView(textBlock);

        // Right column — date + dot
        LinearLayout rightCol = new LinearLayout(this);
        rightCol.setOrientation(LinearLayout.VERTICAL);
        rightCol.setGravity(android.view.Gravity.CENTER);
        android.widget.RelativeLayout.LayoutParams rightParams =
                new android.widget.RelativeLayout.LayoutParams(
                        dpToPx(44), ViewGroup.LayoutParams.WRAP_CONTENT);
        rightParams.addRule(android.widget.RelativeLayout.ALIGN_PARENT_END);
        rightParams.addRule(android.widget.RelativeLayout.CENTER_VERTICAL);
        rightCol.setLayoutParams(rightParams);

        TextView tvDate = new TextView(this);
        tvDate.setText(date);
        tvDate.setTextColor(0xFFAACCDD);
        tvDate.setTextSize(10);
        tvDate.setGravity(android.view.Gravity.CENTER);
        rightCol.addView(tvDate);

        android.view.View dot = new android.view.View(this);
        LinearLayout.LayoutParams dotParams = new LinearLayout.LayoutParams(dpToPx(8), dpToPx(8));
        dotParams.topMargin = dpToPx(4);
        dotParams.gravity   = android.view.Gravity.CENTER_HORIZONTAL;
        dot.setLayoutParams(dotParams);
        dot.setBackgroundResource(R.drawable.dot_blue);
        rightCol.addView(dot);

        row.addView(rightCol);
        notifContainer.addView(row);

        if (showDivider) {
            android.view.View divider = new android.view.View(this);
            LinearLayout.LayoutParams divParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, dpToPx(1));
            divParams.setMarginStart(dpToPx(68));
            divider.setLayoutParams(divParams);
            divider.setBackgroundColor(0x22FFFFFF);
            notifContainer.addView(divider);
        }
    }

    // ── Empty / error states ──────────────────────────────────────────────────

    private void showEmptyState() {
        String msg = !searchQuery.isEmpty()
                ? "No results for \"" + searchQuery + "\""
                : selectedDate != null
                        ? "No notifications on this date"
                        : "No notifications yet.";

        TextView empty = new TextView(this);
        empty.setText(msg);
        empty.setTextColor(0xFFAACCDD);
        empty.setTextSize(14);
        empty.setGravity(android.view.Gravity.CENTER);
        notifContainer.addView(empty, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dpToPx(120)));
    }

    private void showErrorState(String msg) {
        TextView err = new TextView(this);
        err.setText("Error loading notifications:\n" + msg);
        err.setTextColor(0xFFFF6B6B);
        err.setTextSize(13);
        err.setGravity(android.view.Gravity.CENTER);
        notifContainer.addView(err, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dpToPx(120)));
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    @Override
    protected void onResume() {
        super.onResume();
        loadProfileAvatar();
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

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void setOnlineStatus(boolean isOnline) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) return;
        DatabaseReference presenceRef = usersRef.child(currentUser.getUid()).child("online");
        presenceRef.setValue(isOnline);
        if (isOnline) presenceRef.onDisconnect().setValue(false);
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

    // ── Inner model ───────────────────────────────────────────────────────────

    /** Lightweight holder for a single notification row's display data. */
    private static class NotifRow {
        final String title, body, dateStr;
        final long   timestamp;
        NotifRow(String title, String body, String dateStr, long timestamp) {
            this.title     = title;
            this.body      = body;
            this.dateStr   = dateStr;
            this.timestamp = timestamp;
        }
    }
}
