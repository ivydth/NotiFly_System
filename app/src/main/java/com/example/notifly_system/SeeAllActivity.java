package com.example.notifly_system;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
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
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class SeeAllActivity extends AppCompatActivity
        implements NotificationStore.StoreListener {

    public static final String EXTRA_CATEGORY = "extra_category";

    // ── Views ─────────────────────────────────────────────────────────────────

    private AppCompatImageView btnBack;
    private AppCompatImageView btnSearch;
    private AppCompatImageView ivHome, ivBell;

    private LinearLayout  searchBarContainer;
    private EditText      etSearch;
    private AppCompatImageView btnClearSearch;
    private View          searchDivider;

    private LinearLayout  btnDateFilter;
    private TextView      tvDateFilterLabel;
    private TextView      btnClearDateFilter;
    private TextView      tvNotifCount;

    private TextView      tvSectionLabel;
    private LinearLayout  notificationsContainer;
    private SwipeRefreshLayout swipeRefreshLayout;

    // ── State ─────────────────────────────────────────────────────────────────

    private String            category    = "All";
    private String            searchQuery = "";
    private Calendar          selectedDate = null;      // null = no date filter active
    private boolean           searchOpen  = false;
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
        setupSearchBar();
        setupDateFilter();
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
        btnBack                = findViewById(R.id.btnBack);
        btnSearch              = findViewById(R.id.btnSearch);
        ivHome                 = findViewById(R.id.ivHome);
        ivBell                 = findViewById(R.id.ivBell);
        tvSectionLabel         = findViewById(R.id.tvSectionLabel);
        notificationsContainer = findViewById(R.id.notificationsContainer);
        swipeRefreshLayout     = findViewById(R.id.swipeRefreshLayout);

        searchBarContainer     = findViewById(R.id.searchBarContainer);
        etSearch               = findViewById(R.id.etSearch);
        btnClearSearch         = findViewById(R.id.btnClearSearch);
        searchDivider          = findViewById(R.id.searchDivider);

        btnDateFilter          = findViewById(R.id.btnDateFilter);
        tvDateFilterLabel      = findViewById(R.id.tvDateFilterLabel);
        btnClearDateFilter     = findViewById(R.id.btnClearDateFilter);
        tvNotifCount           = findViewById(R.id.tvNotifCount);
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

    // ── Search bar setup ──────────────────────────────────────────────────────

    private void setupSearchBar() {
        if (etSearch == null) return;

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void afterTextChanged(Editable s) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchQuery = s.toString().trim();
                if (btnClearSearch != null) {
                    btnClearSearch.setVisibility(
                            searchQuery.isEmpty() ? View.GONE : View.VISIBLE);
                }
                renderFromStore();
            }
        });

        etSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                hideKeyboard();
                return true;
            }
            return false;
        });

        if (btnClearSearch != null) {
            btnClearSearch.setOnClickListener(v -> {
                etSearch.setText("");
                searchQuery = "";
                renderFromStore();
            });
        }
    }

    // ── Toggle search bar ─────────────────────────────────────────────────────

    private void toggleSearchBar() {
        searchOpen = !searchOpen;
        if (searchBarContainer == null || searchDivider == null) return;

        searchBarContainer.setVisibility(searchOpen ? View.VISIBLE : View.GONE);
        searchDivider.setVisibility(searchOpen ? View.VISIBLE : View.GONE);

        if (searchOpen) {
            etSearch.requestFocus();
            InputMethodManager imm =
                    (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            if (imm != null) imm.showSoftInput(etSearch, InputMethodManager.SHOW_IMPLICIT);
        } else {
            // Close: clear query too
            if (etSearch != null) etSearch.setText("");
            searchQuery = "";
            hideKeyboard();
            renderFromStore();
        }
    }

    private void hideKeyboard() {
        InputMethodManager imm =
                (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null && etSearch != null)
            imm.hideSoftInputFromWindow(etSearch.getWindowToken(), 0);
    }

    // ── Date filter setup ─────────────────────────────────────────────────────

    private void setupDateFilter() {
        if (btnDateFilter == null) return;

        btnDateFilter.setOnClickListener(v -> showDatePicker());

        if (btnClearDateFilter != null) {
            btnClearDateFilter.setOnClickListener(v -> {
                selectedDate = null;
                tvDateFilterLabel.setText("Pick date");
                btnClearDateFilter.setVisibility(View.GONE);
                renderFromStore();
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
                        btnClearDateFilter.setVisibility(View.VISIBLE);

                    renderFromStore();
                },
                initial.get(Calendar.YEAR),
                initial.get(Calendar.MONTH),
                initial.get(Calendar.DAY_OF_MONTH)
        ).show();
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
        // BACK BUTTON → UserActivity
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> {
                startActivity(new Intent(this, UserActivity.class));
                finish();
            });
        }

        // SEARCH ICON → toggle search bar
        if (btnSearch != null) {
            btnSearch.setOnClickListener(v -> toggleSearchBar());
        }

        // HOME
        if (ivHome != null) {
            ivHome.setOnClickListener(v -> {
                startActivity(new Intent(this, UserActivity.class));
                finish();
            });
        }

        // BELL
        if (ivBell != null) {
            ivBell.setOnClickListener(v -> {
                NotificationStore.getInstance().markAllSeen();
                startActivity(new Intent(this, NotifActivity1.class));
            });
        }
    }

    // ── Render from NotificationStore ─────────────────────────────────────────

    /**
     * Steps:
     *  1. Get list based on category tab.
     *  2. Apply date filter if a date was picked.
     *  3. Apply search query filter on notification title.
     *  4. Render rows.
     */
    private void renderFromStore() {
        if (notificationsContainer == null) return;
        notificationsContainer.removeAllViews();

        NotificationStore store = NotificationStore.getInstance();
        List<NotificationItem> items;

        // Step 1 — category
        if (category.equalsIgnoreCase("All")) {
            items = store.getAll();
        } else if (category.equalsIgnoreCase("Starred")) {
            items = store.getStarred();
        } else if (category.equalsIgnoreCase("Unread")) {
            items = store.getUnread();
        } else {
            items = store.getByCategory(category);
        }

        // Step 2 — date filter
        if (selectedDate != null) {
            items = filterByDate(items, selectedDate);
        }

        // Step 3 — search query (matches notification title, case-insensitive)
        if (!searchQuery.isEmpty()) {
            items = filterByTitle(items, searchQuery);
        }

        // Step 4 — update count label
        updateCountLabel(items.size());

        if (items.isEmpty()) {
            showEmptyState();
            return;
        }

        for (int i = 0; i < items.size(); i++) {
            notificationsContainer.addView(buildRow(items.get(i), i < items.size() - 1));
        }
    }

    // ── Filters ───────────────────────────────────────────────────────────────

    /**
     * Keeps only items whose timestamp falls on the same calendar day
     * as the selected date (in the device's local timezone).
     */
    private List<NotificationItem> filterByDate(
            List<NotificationItem> source, Calendar day) {

        List<NotificationItem> result = new ArrayList<>();

        Calendar dayStart = (Calendar) day.clone();
        dayStart.set(Calendar.HOUR_OF_DAY, 0);
        dayStart.set(Calendar.MINUTE, 0);
        dayStart.set(Calendar.SECOND, 0);
        dayStart.set(Calendar.MILLISECOND, 0);

        Calendar dayEnd = (Calendar) dayStart.clone();
        dayEnd.add(Calendar.DAY_OF_MONTH, 1);

        long start = dayStart.getTimeInMillis();
        long end   = dayEnd.getTimeInMillis();

        for (NotificationItem item : source) {
            if (item.timestamp >= start && item.timestamp < end) {
                result.add(item);
            }
        }
        return result;
    }

    /**
     * Keeps only items whose title contains the query string
     * (case-insensitive). Matches against item.senderName (title field).
     */
    private List<NotificationItem> filterByTitle(
            List<NotificationItem> source, String query) {

        String lower = query.toLowerCase(Locale.getDefault());
        List<NotificationItem> result = new ArrayList<>();
        for (NotificationItem item : source) {
            String title = item.senderName != null ? item.senderName : "";
            if (title.toLowerCase(Locale.getDefault()).contains(lower)) {
                result.add(item);
            }
        }
        return result;
    }

    private void updateCountLabel(int count) {
        if (tvNotifCount == null) return;
        tvNotifCount.setText(count == 0 ? "" : count + " notification" + (count == 1 ? "" : "s"));
    }

    // ── Row builder ───────────────────────────────────────────────────────────

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

        // Title
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

        // Body
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

        // Blue dot for unseen
        if (NotificationStore.getInstance().isNew(item.id)) {
            View dot = new View(this);
            LinearLayout.LayoutParams dotParams =
                    new LinearLayout.LayoutParams(dpToPx(8), dpToPx(8));
            dotParams.topMargin  = dpToPx(4);
            dotParams.gravity    = Gravity.CENTER_HORIZONTAL;
            dot.setLayoutParams(dotParams);
            dot.setBackgroundResource(R.drawable.dot_blue);
            rightCol.addView(dot);
        }

        row.addView(rightCol);

        // Tap → mark read + open detail
        row.setOnClickListener(v -> {
            NotificationStore.getInstance().markRead(item.id);
            Intent intent = new Intent(this, NotifActivity.class);
            intent.putExtra(NotifActivity.EXTRA_NOTIF_ID, item.id);
            startActivity(intent);
        });

        wrapper.addView(row);

        // Divider
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
        String msg = !searchQuery.isEmpty()
                ? "No results for \"" + searchQuery + "\""
                : selectedDate != null
                        ? "No notifications on this date"
                        : "No notifications here.";

        TextView empty = new TextView(this);
        empty.setText(msg);
        empty.setTextColor(Color.parseColor("#AACCDD"));
        empty.setTextSize(14);
        empty.setGravity(Gravity.CENTER);
        notificationsContainer.addView(empty, new LinearLayout.LayoutParams(
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
