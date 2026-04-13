package com.example.notifly_system;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.PagerSnapHelper;
import androidx.recyclerview.widget.RecyclerView;
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
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class UserActivity extends AppCompatActivity
        implements NotificationStore.StoreListener {

    // ── Views ─────────────────────────────────────────────────────────────────

    AppCompatImageView btnMenu;
    TextView           btnProfile;
    TextView           tvWelcomeUser;

    RecyclerView       rvSummaryCards;
    SummaryCardAdapter summaryAdapter;

    TextView     tvSectionTitle;
    TextView     tvSeeAll;
    TextView     tvEmptyState;
    LinearLayout notificationsContainer;

    AppCompatImageView ivHome, ivSearch, ivBell;
    TextView           tvBellBadge;

    SwipeRefreshLayout swipeRefreshLayout;

    // ── Firebase ──────────────────────────────────────────────────────────────

    FirebaseAuth      mAuth;
    DatabaseReference database;
    DatabaseReference notificationsRef;
    DatabaseReference presenceRef;

    // ── State ─────────────────────────────────────────────────────────────────

    String currentUsername      = "User";
    String currentEmail         = "";
    int    selectedCardPosition = -1;

    // Only auto-fetch+apply once on first launch so existing notifications
    // are visible immediately. After that, only pull-to-refresh reveals new ones.
    private boolean initialLoadDone = false;

    private static final String[] SECTION_TITLES = {
            "Unread", "Announcements", "Events", "Starred"
    };

    private static final String[] EMPTY_MESSAGES = {
            "No unread notifications",
            "No announcements yet",
            "No events yet",
            "No starred notifications"
    };

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.user_activity);

        btnMenu                = findViewById(R.id.btnMenu);
        btnProfile             = findViewById(R.id.btnProfile);
        tvWelcomeUser          = findViewById(R.id.tvWelcomeUser);
        rvSummaryCards         = findViewById(R.id.rvSummaryCards);
        tvSectionTitle         = findViewById(R.id.tvSectionTitle);
        tvSeeAll               = findViewById(R.id.tvSeeAll);
        tvEmptyState           = findViewById(R.id.tvEmptyState);
        notificationsContainer = findViewById(R.id.notificationsContainer);
        ivHome                 = findViewById(R.id.ivHome);
        ivSearch               = findViewById(R.id.ivSearch);
        ivBell                 = findViewById(R.id.ivBell);
        tvBellBadge            = findViewById(R.id.tvBellBadge);
        swipeRefreshLayout     = findViewById(R.id.swipeRefreshLayout);

        mAuth            = FirebaseAuth.getInstance();
        database         = FirebaseDatabase.getInstance(
                "https://notifly-94dba-default-rtdb.asia-southeast1.firebasedatabase.app/"
        ).getReference("users");
        notificationsRef = FirebaseDatabase.getInstance(
                "https://notifly-94dba-default-rtdb.asia-southeast1.firebasedatabase.app/"
        ).getReference("notifications");

        setupSwipeRefresh();
        setupSummaryCarousel();
        setupClickListeners();
        showNotificationsForCategory(null);
    }

    @Override
    protected void onResume() {
        super.onResume();
        NotificationStore.getInstance().addListener(this);
        loadUserData();
        setOnlineStatus(true);

        if (!initialLoadDone) {
            initialLoadDone = true;
            fetchAndApplyNow();
        } else {
            refreshAll();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        NotificationStore.getInstance().removeListener(this);
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

    // ── First-launch fetch ────────────────────────────────────────────────────

    private void fetchAndApplyNow() {
        if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(true);

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
                Toast.makeText(UserActivity.this,
                        "Failed to load notifications.", Toast.LENGTH_SHORT).show();
            }
        });
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
                Toast.makeText(UserActivity.this,
                        "Failed to refresh.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ── Parse Firebase snapshot ───────────────────────────────────────────────

    /**
     * Reads every notification from Firebase.
     *
     * Firebase fields written by the admin HTML:
     *   target      → "all" | "topic" | "single"
     *   topicOrUser → "announcements" | "events"  (when target == "topic")
     *                 user email                   (when target == "single")
     *                 ""                           (when target == "all")
     *
     * Category mapping:
     *   target == "all"    → "Announcements"   (broadcast to everyone)
     *   target == "topic"  + topicOrUser == "announcements" → "Announcements"
     *   target == "topic"  + topicOrUser == "events"        → "Events"
     *   target == "single" → "Unread"          (direct message)
     *   anything else      → "Unread"
     */
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

            // ── FIX: derive category from BOTH target and topicOrUser ──
            String category = mapTargetToCategory(target, topicOrUser);

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

    // ── Pull-to-refresh setup ─────────────────────────────────────────────────

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

    // ── StoreListener ─────────────────────────────────────────────────────────

    @Override
    public void onStoreChanged() {
        runOnUiThread(this::refreshAll);
    }

    // ── Refresh UI ────────────────────────────────────────────────────────────

    private void refreshAll() {
        refreshSummaryCounts();
        refreshBellBadge();
        String category = selectedCardPosition >= 0
                ? SECTION_TITLES[selectedCardPosition] : null;
        showNotificationsForCategory(category);
    }

    private void refreshSummaryCounts() {
        if (summaryAdapter == null) return;
        NotificationStore store = NotificationStore.getInstance();
        summaryAdapter.updateCount(0, String.valueOf(store.getUnreadCount()));
        for (int i = 1; i <= 2; i++) {
            summaryAdapter.updateCount(i,
                    String.valueOf(store.getByCategory(SECTION_TITLES[i]).size()));
        }
        summaryAdapter.updateCount(3, String.valueOf(store.getStarred().size()));
    }

    private void refreshBellBadge() {
        if (tvBellBadge == null) return;
        int count = NotificationStore.getInstance().getNewCount();
        if (count <= 0) {
            tvBellBadge.setVisibility(View.GONE);
        } else {
            tvBellBadge.setVisibility(View.VISIBLE);
            tvBellBadge.setText(count > 99 ? "99+" : String.valueOf(count));
        }
    }

    // ── Summary Carousel ──────────────────────────────────────────────────────

    private void setupSummaryCarousel() {
        NotificationStore store = NotificationStore.getInstance();

        ArrayList<SummaryCard> cards = new ArrayList<>();
        cards.add(new SummaryCard(String.valueOf(store.getUnreadCount()),
                "Unread",        "#5BB8FF"));
        cards.add(new SummaryCard(String.valueOf(store.getByCategory("Announcements").size()),
                "Announcements", "#00C9B1"));
        cards.add(new SummaryCard(String.valueOf(store.getByCategory("Events").size()),
                "Events",        "#C084FC"));
        cards.add(new SummaryCard(String.valueOf(store.getStarred().size()),
                "Starred",       "#FFB347"));

        summaryAdapter = new SummaryCardAdapter(this, cards, this::onCardTapped);

        LinearLayoutManager llm = new LinearLayoutManager(
                this, LinearLayoutManager.HORIZONTAL, false);
        rvSummaryCards.setLayoutManager(llm);
        rvSummaryCards.setAdapter(summaryAdapter);
        new PagerSnapHelper().attachToRecyclerView(rvSummaryCards);
        rvSummaryCards.setClipChildren(false);
        rvSummaryCards.setClipToPadding(false);
    }

    // ── Click Listeners ───────────────────────────────────────────────────────

    private void setupClickListeners() {
        btnMenu.setOnClickListener(v -> {
            startActivity(new Intent(this, UserMenu.class));
            overridePendingTransition(0, 0);
        });

        btnProfile.setOnClickListener(v ->
                startActivity(new Intent(this, ProfileActivity.class)));

        ivHome.setOnClickListener(v ->
                startActivity(new Intent(this, UserActivity.class)));

        ivSearch.setOnClickListener(v -> { /* TODO */ });

        ivBell.setOnClickListener(v -> {
            NotificationStore.getInstance().markAllSeen();
            refreshBellBadge();
            startActivity(new Intent(this, NotifActivity1.class));
        });

        tvSeeAll.setOnClickListener(v -> {
            String category = selectedCardPosition >= 0
                    ? SECTION_TITLES[selectedCardPosition]
                    : "All";
            Intent intent = new Intent(this, SeeAllActivity.class);
            intent.putExtra(SeeAllActivity.EXTRA_CATEGORY, category);
            startActivity(intent);
        });
    }

    // ── Card tap ──────────────────────────────────────────────────────────────

    private void onCardTapped(int position) {
        selectedCardPosition = position;
        if (position < 0) {
            tvSectionTitle.setText("New");
            showNotificationsForCategory(null);
        } else {
            tvSectionTitle.setText(SECTION_TITLES[position]);
            showNotificationsForCategory(SECTION_TITLES[position]);
        }
    }

    // ── Notification list ─────────────────────────────────────────────────────

    private void showNotificationsForCategory(String category) {
        while (notificationsContainer.getChildCount() > 1) {
            notificationsContainer.removeViewAt(1);
        }

        List<NotificationItem> items;
        if (category == null) {
            items = NotificationStore.getInstance().getAll();
        } else if (category.equalsIgnoreCase("Unread")) {
            items = NotificationStore.getInstance().getUnread();
        } else if (category.equalsIgnoreCase("Starred")) {
            items = NotificationStore.getInstance().getStarred();
        } else {
            items = NotificationStore.getInstance().getByCategory(category);
        }

        if (items.isEmpty()) {
            tvEmptyState.setVisibility(View.VISIBLE);
            int idx = category == null ? -1 : indexOf(SECTION_TITLES, category);
            tvEmptyState.setText(idx >= 0 ? EMPTY_MESSAGES[idx] : "No new notifications");
            return;
        }

        tvEmptyState.setVisibility(View.GONE);
        for (NotificationItem item : items) {
            notificationsContainer.addView(buildNotificationRow(item));
        }
    }

    private View buildNotificationRow(NotificationItem item) {
        View row = LayoutInflater.from(this)
                .inflate(R.layout.item_notification_row, notificationsContainer, false);

        TextView tvAvatar  = row.findViewById(R.id.tvAvatar);
        TextView tvName    = row.findViewById(R.id.tvSenderName);
        TextView tvMessage = row.findViewById(R.id.tvMessage);
        TextView tvDate    = row.findViewById(R.id.tvDate);
        TextView tvStar    = row.findViewById(R.id.tvStar);
        View     divider   = row.findViewById(R.id.vDivider);

        View vNewDot = row.findViewById(R.id.vNewDot);
        if (vNewDot != null) {
            boolean isNew = NotificationStore.getInstance().isNew(item.id);
            vNewDot.setVisibility(isNew ? View.VISIBLE : View.INVISIBLE);
            if (isNew) startGlowPulse(vNewDot);
        }

        if (tvAvatar != null) {
            String name = (item.senderName != null && !item.senderName.isEmpty())
                    ? item.senderName : "N";
            tvAvatar.setText(String.valueOf(name.charAt(0)).toUpperCase());
        }

        if (tvName    != null) tvName.setText(item.senderName);
        if (tvMessage != null) tvMessage.setText(item.message);
        if (tvDate    != null) tvDate.setText(item.dateLabel);
        if (divider   != null) divider.setVisibility(View.GONE);

        applyReadStyle(tvName, tvMessage, item.isRead);
        if (tvStar != null) applyStarColor(tvStar, item.isStarred);

        row.setOnClickListener(v -> {
            Intent intent = new Intent(this, NotifActivity.class);
            intent.putExtra(NotifActivity.EXTRA_NOTIF_ID, item.id);
            startActivity(intent);
        });

        if (tvStar != null) {
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

        return row;
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

    /**
     * Maps the Firebase fields [target, topicOrUser] to a UI category.
     *
     * Admin HTML writes:
     *   "All Users"               → target="all",    topicOrUser=""
     *   "By Topic → Announcements"→ target="topic",  topicOrUser="announcements"
     *   "By Topic → Events"       → target="topic",  topicOrUser="events"
     *   "Single User"             → target="single", topicOrUser=<email>
     *
     * UI categories: "Announcements" | "Events" | "Unread"
     */
    private String mapTargetToCategory(String target, String topicOrUser) {
        if (target == null) return "Unread";

        switch (target.toLowerCase()) {
            case "all":
                // Broadcast to everyone → Announcements card
                return "Announcements";

            case "topic":
                // Check which topic was chosen by the admin
                if (topicOrUser != null) {
                    switch (topicOrUser.toLowerCase()) {
                        case "announcements": return "Announcements";
                        case "events":        return "Events";
                    }
                }
                // topicOrUser missing or unknown → default to Announcements
                return "Announcements";

            case "single":
                // Direct message to one user → Unread card
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

    private void setOnlineStatus(boolean isOnline) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) return;
        presenceRef = database.child(currentUser.getUid()).child("online");
        presenceRef.setValue(isOnline);
        if (isOnline) presenceRef.onDisconnect().setValue(false);
    }

    private void loadUserData() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }
        database.child(currentUser.getUid())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        if (!snapshot.exists()) return;
                        String firstName = snapshot.child("firstName").getValue(String.class);
                        String username  = snapshot.child("username").getValue(String.class);
                        String email     = snapshot.child("email").getValue(String.class);
                        if (username != null && !username.isEmpty()) {
                            tvWelcomeUser.setText(username + "!");
                            currentUsername = username;
                        } else if (firstName != null && !firstName.isEmpty()) {
                            tvWelcomeUser.setText(firstName + "!");
                            currentUsername = firstName;
                        } else {
                            tvWelcomeUser.setText("User!");
                            currentUsername = "User";
                        }
                        btnProfile.setText(currentUsername.substring(0, 1).toUpperCase());
                        currentEmail = (email != null && !email.isEmpty()) ? email
                                : (currentUser.getEmail() != null ? currentUser.getEmail() : "");
                    }
                    @Override
                    public void onCancelled(DatabaseError error) {
                        tvWelcomeUser.setText("User!");
                        btnProfile.setText("U");
                    }
                });
    }

    private static int indexOf(String[] arr, String value) {
        for (int i = 0; i < arr.length; i++) {
            if (arr[i].equalsIgnoreCase(value)) return i;
        }
        return -1;
    }

    // ── Animation helpers ─────────────────────────────────────────────────────

    static void animateLift(View root) {
        ObjectAnimator ty   = ObjectAnimator.ofFloat(root, "translationY", root.getTranslationY(), -14f);
        ObjectAnimator elev = ObjectAnimator.ofFloat(root, "elevation", root.getElevation(), 20f);
        ty.setDuration(180);
        elev.setDuration(180);
        AnimatorSet set = new AnimatorSet();
        set.playTogether(ty, elev);
        set.start();
    }

    static void animateDrop(View root) {
        ObjectAnimator ty   = ObjectAnimator.ofFloat(root, "translationY", root.getTranslationY(), 0f);
        ObjectAnimator elev = ObjectAnimator.ofFloat(root, "elevation", root.getElevation(), 4f);
        ty.setDuration(200);
        elev.setDuration(200);
        AnimatorSet set = new AnimatorSet();
        set.playTogether(ty, elev);
        set.start();
    }

    // ── Inner types ───────────────────────────────────────────────────────────

    interface OnCardTappedListener { void onTapped(int position); }

    static class SummaryCard {
        String count, label, colorHex;
        SummaryCard(String count, String label, String colorHex) {
            this.count = count; this.label = label; this.colorHex = colorHex;
        }
    }

    static class SummaryCardAdapter
            extends RecyclerView.Adapter<SummaryCardAdapter.CardViewHolder> {

        private final Context              context;
        private final List<SummaryCard>    items;
        private final OnCardTappedListener listener;
        private int     selectedPosition = -1;
        private float   touchStartX = 0f, touchStartY = 0f;
        private boolean isDragging  = false;
        private static final int DRAG_THRESHOLD_DP = 8;

        SummaryCardAdapter(Context context, List<SummaryCard> items, OnCardTappedListener listener) {
            this.context = context; this.items = items; this.listener = listener;
        }

        @NonNull @Override
        public CardViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new CardViewHolder(LayoutInflater.from(context)
                    .inflate(R.layout.item_summary_card, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull CardViewHolder holder, int position) {
            SummaryCard card     = items.get(position);
            boolean     selected = (position == selectedPosition);
            holder.tvCount.setText(card.count);
            holder.tvCount.setTextColor(Color.parseColor(card.colorHex));
            holder.tvLabel.setText(card.label);
            applyState(holder, card, selected, false);

            float threshold = DRAG_THRESHOLD_DP * context.getResources().getDisplayMetrics().density;
            holder.itemView.setOnTouchListener((v, event) -> {
                int pos = holder.getAdapterPosition();
                if (pos == RecyclerView.NO_ID) return false;
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        touchStartX = event.getRawX(); touchStartY = event.getRawY();
                        isDragging = false;
                        v.getParent().requestDisallowInterceptTouchEvent(true);
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        float dx = Math.abs(event.getRawX() - touchStartX);
                        float dy = Math.abs(event.getRawY() - touchStartY);
                        if (!isDragging && (dx > threshold || dy > threshold)) {
                            isDragging = true;
                            v.getParent().requestDisallowInterceptTouchEvent(false);
                        }
                        return !isDragging;
                    case MotionEvent.ACTION_UP:
                        v.getParent().requestDisallowInterceptTouchEvent(false);
                        if (!isDragging) {
                            int prev = selectedPosition;
                            if (prev == pos) {
                                selectedPosition = -1;
                                applyState(holder, card, false, true);
                                notifyItemChanged(pos);
                                if (listener != null) listener.onTapped(-1);
                            } else {
                                selectedPosition = pos;
                                applyState(holder, card, true, true);
                                notifyItemChanged(pos);
                                if (prev != -1) notifyItemChanged(prev);
                                if (listener != null) listener.onTapped(pos);
                            }
                        }
                        isDragging = false;
                        return true;
                    case MotionEvent.ACTION_CANCEL:
                        v.getParent().requestDisallowInterceptTouchEvent(false);
                        isDragging = false;
                        return true;
                }
                return false;
            });
        }

        private void applyState(CardViewHolder holder, SummaryCard card, boolean selected, boolean animate) {
            View root = holder.itemView;
            if (animate) { if (selected) animateLift(root); else animateDrop(root); }
            else { root.setTranslationY(selected ? -14f : 0f); root.setElevation(selected ? 20f : 4f); }
            if (holder.cardView != null) {
                if (selected) {
                    int a = Color.parseColor(card.colorHex);
                    int r = Math.max(0, Math.min(255, (int)(0x1E + 0.20f * (Color.red(a)   - 0x1E))));
                    int g = Math.max(0, Math.min(255, (int)(0x3A + 0.20f * (Color.green(a) - 0x3A))));
                    int b = Math.max(0, Math.min(255, (int)(0x4A + 0.20f * (Color.blue(a)  - 0x4A))));
                    holder.cardView.setCardBackgroundColor(Color.rgb(r, g, b));
                } else {
                    holder.cardView.setCardBackgroundColor(Color.parseColor("#1E3A4A"));
                }
            }
            if (holder.viewGlow != null) {
                holder.viewGlow.setGlowColor(Color.parseColor(card.colorHex));
                holder.viewGlow.animate().alpha(selected ? 1f : 0f).setDuration(animate ? 220 : 0).start();
            }
        }

        @Override public int getItemCount() { return items.size(); }

        public void updateCount(int position, String newCount) {
            if (position < 0 || position >= items.size()) return;
            items.get(position).count = newCount;
            notifyItemChanged(position);
        }

        static class CardViewHolder extends RecyclerView.ViewHolder {
            TextView tvCount, tvLabel;
            CardView cardView;
            GlowView viewGlow;
            CardViewHolder(@NonNull View itemView) {
                super(itemView);
                tvCount  = itemView.findViewById(R.id.tvCount);
                tvLabel  = itemView.findViewById(R.id.tvLabel);
                cardView = itemView.findViewById(R.id.summaryCard);
                viewGlow = itemView.findViewById(R.id.viewGlow);
            }
        }
    }
}
