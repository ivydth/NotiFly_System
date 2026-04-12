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
            "Unread",
            "Announcements",
            "Events",
            "Starred"
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

        if (isOnline) {
            presenceRef.onDisconnect().setValue(false);
        }
    }

    private void loadUserData() {
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        String userId = currentUser.getUid();

        database.child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists()) {
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

                    String avatarLetter = currentUsername.substring(0, 1).toUpperCase();
                    btnProfile.setText(avatarLetter);

                    currentEmail = (email != null && !email.isEmpty())
                            ? email
                            : (currentUser.getEmail() != null ? currentUser.getEmail() : "");
                }
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
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        }
    }

    // ── Lift/drop animations ───────────────────────────────────────

    static void animateLift(View card) {
        ObjectAnimator liftY    = ObjectAnimator.ofFloat(card, "translationY", card.getTranslationY(), -16f);
        ObjectAnimator liftElev = ObjectAnimator.ofFloat(card, "elevation", card.getElevation(), 20f);
        liftY.setDuration(180);
        liftElev.setDuration(180);
        AnimatorSet liftSet = new AnimatorSet();
        liftSet.playTogether(liftY, liftElev);
        liftSet.start();
    }

    static void animateDrop(View card) {
        ObjectAnimator dropY    = ObjectAnimator.ofFloat(card, "translationY", card.getTranslationY(), 0f);
        ObjectAnimator dropElev = ObjectAnimator.ofFloat(card, "elevation", card.getElevation(), 4f);
        dropY.setDuration(200);
        dropElev.setDuration(200);
        AnimatorSet dropSet = new AnimatorSet();
        dropSet.playTogether(dropY, dropElev);
        dropSet.start();
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

        // How many dp the finger must travel before we call it a scroll
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

            // Snap the visual state immediately on bind (handles RecyclerView rebinds)
            applySelectedState(holder.itemView, card, selected, false);

            float threshold = DRAG_THRESHOLD_DP
                    * context.getResources().getDisplayMetrics().density;

            holder.itemView.setOnTouchListener((v, event) -> {
                int pos = holder.getAdapterPosition();
                if (pos == RecyclerView.NO_ID) return false;

                switch (event.getAction()) {

                    case MotionEvent.ACTION_DOWN:
                        // Record where the finger landed and reset drag flag
                        touchStartX = event.getRawX();
                        touchStartY = event.getRawY();
                        isDragging  = false;

                        // Tell the parent not to intercept yet — we'll decide
                        v.getParent().requestDisallowInterceptTouchEvent(true);
                        return true; // consume DOWN so we get MOVE/UP

                    case MotionEvent.ACTION_MOVE:
                        float mdx = Math.abs(event.getRawX() - touchStartX);
                        float mdy = Math.abs(event.getRawY() - touchStartY);

                        if (!isDragging && mdx > threshold) {
                            // Clearly a horizontal scroll — hand control back to RecyclerView
                            isDragging = true;
                            v.getParent().requestDisallowInterceptTouchEvent(false);

                            // Drop any card that was lifted before we realised it was a scroll
                            if (selectedPosition == pos) {
                                selectedPosition = -1;
                                applySelectedState(v, card, false, true);
                                notifyItemChanged(pos);
                                if (listener != null) listener.onTapped(-1);
                            }
                        } else if (!isDragging && mdy > threshold) {
                            // Vertical scroll — also hand back
                            isDragging = true;
                            v.getParent().requestDisallowInterceptTouchEvent(false);
                        }
                        return !isDragging; // consume only while we still own the gesture

                    case MotionEvent.ACTION_UP:
                        v.getParent().requestDisallowInterceptTouchEvent(false);

                        if (!isDragging) {
                            // Confirmed tap — toggle selection
                            int prev = selectedPosition;

                            if (prev == pos) {
                                // Tap on already-selected card → deselect
                                selectedPosition = -1;
                                applySelectedState(v, card, false, true);
                                notifyItemChanged(pos);
                                if (listener != null) listener.onTapped(-1);
                            } else {
                                // Select this card, deselect previous
                                selectedPosition = pos;
                                applySelectedState(v, card, true, true);
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

                        // Drop the card if it got lifted before the cancel
                        if (selectedPosition == pos) {
                            selectedPosition = -1;
                            applySelectedState(v, card, false, true);
                            notifyItemChanged(pos);
                            if (listener != null) listener.onTapped(-1);
                        }
                        return true;
                }
                return false;
            });
        }

        /**
         * Applies the lifted/normal visual state to a card.
         *
         * @param view     the card's root view (CardView)
         * @param card     the data model (for accent colour)
         * @param selected true = lifted + underglow, false = normal
         * @param animate  true = animate the lift/drop, false = snap instantly
         */
        private void applySelectedState(View view, SummaryCard card,
                                        boolean selected, boolean animate) {
            // ── Lift ──────────────────────────────────────────────
            if (animate) {
                if (selected) animateLift(view);
                else          animateDrop(view);
            } else {
                view.setTranslationY(selected ? -16f : 0f);
                view.setElevation(selected ? 20f : 4f);
            }

            // ── CardView background tint ───────────────────────────
            if (view instanceof CardView) {
                CardView cv = (CardView) view;
                if (selected) {
                    int accent = Color.parseColor(card.colorHex);
                    int r = (int) (0x1E + 0.20f * (Color.red(accent)   - 0x1E));
                    int g = (int) (0x3A + 0.20f * (Color.green(accent) - 0x3A));
                    int b = (int) (0x4A + 0.20f * (Color.blue(accent)  - 0x4A));
                    cv.setCardBackgroundColor(Color.rgb(
                            Math.max(0, Math.min(255, r)),
                            Math.max(0, Math.min(255, g)),
                            Math.max(0, Math.min(255, b))
                    ));
                } else {
                    cv.setCardBackgroundColor(Color.parseColor("#1E3A4A"));
                }
            }

            // ── Underglow overlay ──────────────────────────────────
            View glow = view.findViewById(R.id.viewUnderglow);
            if (glow != null) {
                int accent    = Color.parseColor(card.colorHex);
                int glowColor = Color.argb(80,
                        Color.red(accent),
                        Color.green(accent),
                        Color.blue(accent));
                glow.setBackgroundColor(glowColor);
                glow.animate()
                        .alpha(selected ? 1f : 0f)
                        .setDuration(animate ? 180 : 0)
                        .start();
            }
        }

        @Override
        public int getItemCount() { return items.size(); }

        public void updateCount(int position, String newCount) {
            items.get(position).count = newCount;
            notifyItemChanged(position);
        }

        static class CardViewHolder extends RecyclerView.ViewHolder {
            TextView tvCount, tvLabel;

            CardViewHolder(@NonNull View itemView) {
                super(itemView);
                tvCount = itemView.findViewById(R.id.tvCount);
                tvLabel = itemView.findViewById(R.id.tvLabel);
            }
        }
    }
}
