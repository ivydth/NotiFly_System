package com.example.notifly_system;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageView;
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
    RecyclerView rvSummaryCards;
    SummaryCardAdapter summaryAdapter;

    // Bottom nav
    AppCompatImageView ivHome, ivSearch, ivBell;

    // Firebase
    FirebaseAuth      mAuth;
    DatabaseReference database;
    DatabaseReference presenceRef;

    // User data
    String currentUsername = "User";
    String currentEmail    = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.user_activity);

        // ── INITIALIZE VIEWS ──────────────────────────────────────

        btnMenu    = findViewById(R.id.btnMenu);
        btnProfile = findViewById(R.id.btnProfile);

        tvWelcomeUser = findViewById(R.id.tvWelcomeUser);

        rvSummaryCards = findViewById(R.id.rvSummaryCards);

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

        summaryAdapter = new SummaryCardAdapter(this, cards);

        LinearLayoutManager llm = new LinearLayoutManager(
                this, LinearLayoutManager.HORIZONTAL, false);
        rvSummaryCards.setLayoutManager(llm);
        rvSummaryCards.setAdapter(summaryAdapter);

        // Snap so each swipe locks cleanly to the next card
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

        btnProfile.setOnClickListener(v -> {
            startActivity(new Intent(this, ProfileActivity.class));
        });

        ivHome.setOnClickListener(v -> {
            startActivity(new Intent(this, UserActivity.class));
        });

        ivSearch.setOnClickListener(v -> {
            // TODO: navigate to search activity
        });

        ivBell.setOnClickListener(v -> {
            startActivity(new Intent(this, NotifActivity1.class));
        });
    }

    // ── Runs every time the screen comes back into view ────────────
    @Override
    protected void onResume() {
        super.onResume();
        loadUserData();
        setOnlineStatus(true);
    }

    // ── Runs when user leaves the screen ──────────────────────────
    @Override
    protected void onPause() {
        super.onPause();
        setOnlineStatus(false);
    }

    // ── Sets online/offline in Firebase ───────────────────────────
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

                    // ── Set welcome text ──────────────────────────
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

                    // ── Set profile avatar letter ─────────────────
                    String avatarLetter = currentUsername.substring(0, 1).toUpperCase();
                    btnProfile.setText(avatarLetter);

                    // ── Set email ─────────────────────────────────
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

    // ══════════════════════════════════════════════════════════════
    // SummaryCard  —  simple data model
    // ══════════════════════════════════════════════════════════════
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
    // SummaryCardAdapter  —  drives the RecyclerView carousel
    // ══════════════════════════════════════════════════════════════
    static class SummaryCardAdapter
            extends RecyclerView.Adapter<SummaryCardAdapter.CardViewHolder> {

        private final Context          context;
        private final List<SummaryCard> items;

        SummaryCardAdapter(Context context, List<SummaryCard> items) {
            this.context = context;
            this.items   = items;
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
            SummaryCard card = items.get(position);
            holder.tvCount.setText(card.count);
            holder.tvCount.setTextColor(Color.parseColor(card.colorHex));
            holder.tvLabel.setText(card.label);
        }

        @Override
        public int getItemCount() { return items.size(); }

        // Public helper so the Activity can update counts later
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
