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
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.PagerSnapHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Arrays;
import java.util.List;

public class UserActivity extends AppCompatActivity {

    // Top bar
    AppCompatImageView btnMenu;
    TextView           btnProfile;

    // Welcome banner
    TextView tvWelcomeUser;

    // Summary carousel
    RecyclerView       rvSummaryCards;
    SummaryCardAdapter summaryAdapter;

    // Notifications section
    TextView     tvSectionTitle;
    TextView     tvEmptyState;
    LinearLayout notificationsContainer;

    // Bottom nav
    AppCompatImageView ivHome, ivSearch, ivBell;

    // Firebase
    FirebaseAuth      mAuth;
    DatabaseReference database;
    DatabaseReference presenceRef;

    // User data
    String currentUsername = "User";
    String currentEmail    = "";

    private static final String[] SECTION_TITLES = {
            "Unread", "Announcements", "Events", "Starred"
    };

    private static final String[] EMPTY_MESSAGES = {
            "No unread notifications",
            "No announcements yet",
            "No events yet",
            "No starred notifications"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.user_activity);

        btnMenu    = findViewById(R.id.btnMenu);
        btnProfile = findViewById(R.id.btnProfile);

        tvWelcomeUser          = findViewById(R.id.tvWelcomeUser);
        rvSummaryCards         = findViewById(R.id.rvSummaryCards);
        tvSectionTitle         = findViewById(R.id.tvSectionTitle);
        tvEmptyState           = findViewById(R.id.tvEmptyState);
        notificationsContainer = findViewById(R.id.notificationsContainer);

        ivHome   = findViewById(R.id.ivHome);
        ivSearch = findViewById(R.id.ivSearch);
        ivBell   = findViewById(R.id.ivBell);

        // ── SUMMARY CAROUSEL ──────────────────────────────────────

        List<SummaryCard> cards = Arrays.asList(
                new SummaryCard("0", "Unread",        "#5BB8FF"),
                new SummaryCard("0", "Announcements", "#00C9B1"),
                new SummaryCard("0", "Events",        "#C084FC"),
                new SummaryCard("0", "Starred",       "#FFB347")
        );

        summaryAdapter = new SummaryCardAdapter(this, cards, this::onCardTapped);

        LinearLayoutManager llm = new LinearLayoutManager(
                this, LinearLayoutManager.HORIZONTAL, false);
        rvSummaryCards.setLayoutManager(llm);
        rvSummaryCards.setAdapter(summaryAdapter);
        new PagerSnapHelper().attachToRecyclerView(rvSummaryCards);

        // Allow the glow to render outside the RecyclerView clip bounds
        rvSummaryCards.setClipChildren(false);
        rvSummaryCards.setClipToPadding(false);

        // ── FIREBASE ──────────────────────────────────────────────

        mAuth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance(
                "https://notifly-94dba-default-rtdb.asia-southeast1.firebasedatabase.app/"
        ).getReference("users");

        // ── BUTTON LISTENERS ──────────────────────────────────────

        btnMenu.setOnClickListener(v -> {
            startActivity(new Intent(this, UserMenu.class));
            overridePendingTransition(0, 0);
        });

        btnProfile.setOnClickListener(v ->
                startActivity(new Intent(this, ProfileActivity.class)));

        ivHome.setOnClickListener(v ->
                startActivity(new Intent(this, UserActivity.class)));

        ivSearch.setOnClickListener(v -> {
            // TODO: navigate to search activity
        });

        ivBell.setOnClickListener(v ->
                startActivity(new Intent(this, NotifActivity1.class)));
    }

    private void onCardTapped(int position) {
        if (position < 0) {
            tvSectionTitle.setText("New");
            tvEmptyState.setText("No new notifications");
            return;
        }
        tvSectionTitle.setText(SECTION_TITLES[position]);
        tvEmptyState.setText(EMPTY_MESSAGES[position]);
        // TODO: filter notificationsContainer by category here
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadUserData();
        setOnlineStatus(true);
    }

    @Override
    protected void onPause() {
        super.onPause();
        setOnlineStatus(false);
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

                        currentEmail = (email != null && !email.isEmpty())
                                ? email
                                : (currentUser.getEmail() != null
                                        ? currentUser.getEmail() : "");
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        tvWelcomeUser.setText("User!");
                        btnProfile.setText("U");
                    }
                });
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (mAuth.getCurrentUser() == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        }
    }

    // ── Lift / drop the FrameLayout (card + glow together) ────────

    static void animateLift(View root) {
        ObjectAnimator ty   = ObjectAnimator.ofFloat(root, "translationY",
                root.getTranslationY(), -14f);
        ObjectAnimator elev = ObjectAnimator.ofFloat(root, "elevation",
                root.getElevation(), 20f);
        ty.setDuration(180);
        elev.setDuration(180);
        AnimatorSet set = new AnimatorSet();
        set.playTogether(ty, elev);
        set.start();
    }

    static void animateDrop(View root) {
        ObjectAnimator ty   = ObjectAnimator.ofFloat(root, "translationY",
                root.getTranslationY(), 0f);
        ObjectAnimator elev = ObjectAnimator.ofFloat(root, "elevation",
                root.getElevation(), 4f);
        ty.setDuration(200);
        elev.setDuration(200);
        AnimatorSet set = new AnimatorSet();
        set.playTogether(ty, elev);
        set.start();
    }

    // ── Interfaces / Models ────────────────────────────────────────

    interface OnCardTappedListener {
        void onTapped(int position);
    }

    static class SummaryCard {
        String count;
        String label;
        String colorHex;

        SummaryCard(String count, String label, String colorHex) {
            this.count    = count;
            this.label    = label;
            this.colorHex = colorHex;
        }
    }

    // ══════════════════════════════════════════════════════════════
    // SummaryCardAdapter
    // ══════════════════════════════════════════════════════════════
    static class SummaryCardAdapter
            extends RecyclerView.Adapter<SummaryCardAdapter.CardViewHolder> {

        private final Context              context;
        private final List<SummaryCard>    items;
        private final OnCardTappedListener listener;

        private int     selectedPosition = -1;
        private float   touchStartX      = 0f;
        private float   touchStartY      = 0f;
        private boolean isDragging       = false;

        private static final int DRAG_THRESHOLD_DP = 8;

        SummaryCardAdapter(Context context, List<SummaryCard> items,
                           OnCardTappedListener listener) {
            this.context  = context;
            this.items    = items;
            this.listener = listener;
        }

        @NonNull
        @Override
        public CardViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(context)
                    .inflate(R.layout.item_summary_card, parent, false);
            return new CardViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull CardViewHolder holder, int position) {
            SummaryCard card     = items.get(position);
            boolean     selected = (position == selectedPosition);

            holder.tvCount.setText(card.count);
            holder.tvCount.setTextColor(Color.parseColor(card.colorHex));
            holder.tvLabel.setText(card.label);

            // Snap to correct state instantly on bind (no animation needed on rebind)
            applyState(holder, card, selected, false);

            float threshold = DRAG_THRESHOLD_DP
                    * context.getResources().getDisplayMetrics().density;

            holder.itemView.setOnTouchListener((v, event) -> {
                int pos = holder.getAdapterPosition();
                if (pos == RecyclerView.NO_ID) return false;

                switch (event.getAction()) {

                    case MotionEvent.ACTION_DOWN:
                        touchStartX = event.getRawX();
                        touchStartY = event.getRawY();
                        isDragging  = false;
                        // Claim the gesture — we decide if it's a tap or scroll
                        v.getParent().requestDisallowInterceptTouchEvent(true);
                        return true;

                    case MotionEvent.ACTION_MOVE:
                        float dx = Math.abs(event.getRawX() - touchStartX);
                        float dy = Math.abs(event.getRawY() - touchStartY);
                        if (!isDragging && (dx > threshold || dy > threshold)) {
                            // Finger moved too much — it's a scroll, hand back to RecyclerView
                            isDragging = true;
                            v.getParent().requestDisallowInterceptTouchEvent(false);
                        }
                        return !isDragging;

                    case MotionEvent.ACTION_UP:
                        v.getParent().requestDisallowInterceptTouchEvent(false);
                        if (!isDragging) {
                            int prev = selectedPosition;
                            if (prev == pos) {
                                // Tap already-selected → deselect
                                selectedPosition = -1;
                                applyState(holder, card, false, true);
                                notifyItemChanged(pos);
                                if (listener != null) listener.onTapped(-1);
                            } else {
                                // Select this card, drop previous
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
                        // RecyclerView took the gesture (fling/scroll).
                        // Card stays lifted — user didn't intentionally deselect.
                        v.getParent().requestDisallowInterceptTouchEvent(false);
                        isDragging = false;
                        return true;
                }
                return false;
            });
        }

        /**
         * Applies the selected or normal visual state to a card.
         *
         * @param holder   ViewHolder to update
         * @param card     data model (accent colour)
         * @param selected true = lifted + glowing
         * @param animate  true = animate transitions, false = snap instantly
         */
        private void applyState(CardViewHolder holder, SummaryCard card,
                                boolean selected, boolean animate) {

            View root = holder.itemView; // the FrameLayout

            // 1. Lift the whole item (card + glow rise together)
            if (animate) {
                if (selected) animateLift(root);
                else          animateDrop(root);
            } else {
                root.setTranslationY(selected ? -14f : 0f);
                root.setElevation(selected ? 20f : 4f);
            }

            // 2. Subtle card background tint when selected
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

            // 3. Radial gradient glow below the card
            if (holder.viewGlow != null) {
                holder.viewGlow.setGlowColor(Color.parseColor(card.colorHex));
                holder.viewGlow.animate()
                        .alpha(selected ? 1f : 0f)
                        .setDuration(animate ? 220 : 0)
                        .start();
            }
        }

        @Override
        public int getItemCount() { return items.size(); }

        public void updateCount(int position, String newCount) {
            items.get(position).count = newCount;
            notifyItemChanged(position);
        }

        // ── ViewHolder ─────────────────────────────────────────────
        static class CardViewHolder extends RecyclerView.ViewHolder {
            TextView tvCount;
            TextView tvLabel;
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
