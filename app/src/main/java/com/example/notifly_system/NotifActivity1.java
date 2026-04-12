package com.example.notifly_system;

import android.content.Intent;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageView;

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

public class NotifActivity1 extends AppCompatActivity {

    // Top bar
    AppCompatImageView btnMenu;
    TextView           btnProfile; // ✅ TextView for live letter avatar

    // Bottom nav
    AppCompatImageView ivHome, ivSearch, ivBell;

    // Notification list container
    LinearLayout notifContainer;

    // Firebase
    FirebaseAuth      mAuth;
    DatabaseReference usersRef;
    DatabaseReference notificationsRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.userdbtotalnotif_activity;

        // ── INITIALIZE VIEWS ──────────────────────────────────────
        btnMenu    = findViewById(R.id.btnMenu);
        btnProfile = findViewById(R.id.btnProfile); // TextView now
        ivHome     = findViewById(R.id.ivHome);
        ivSearch   = findViewById(R.id.ivSearch);
        ivBell     = findViewById(R.id.ivBell);
        notifContainer = findViewById(R.id.notifContainer);

        // ── FIREBASE ──────────────────────────────────────────────
        mAuth = FirebaseAuth.getInstance();
        usersRef = FirebaseDatabase.getInstance(
                "https://notifly-94dba-default-rtdb.asia-southeast1.firebasedatabase.app/"
        ).getReference("users");
        notificationsRef = FirebaseDatabase.getInstance(
                "https://notifly-94dba-default-rtdb.asia-southeast1.firebasedatabase.app/"
        ).getReference("notifications");

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
            overridePendingTransition(0, 0);
        });

        ivSearch.setOnClickListener(v -> {
            // TODO: navigate to search
        });

        ivBell.setOnClickListener(v -> {
            // already here
        });

        // ── LOAD DATA ─────────────────────────────────────────────
        loadProfileAvatar();
        loadNotifications();
    }

    // ✅ Loads the user's first letter just like UserActivity does
    private void loadProfileAvatar() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) return;

        usersRef.child(currentUser.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                String username  = snapshot.child("username").getValue(String.class);
                String firstName = snapshot.child("firstName").getValue(String.class);

                String displayName;
                if (username != null && !username.isEmpty()) {
                    displayName = username;
                } else if (firstName != null && !firstName.isEmpty()) {
                    displayName = firstName;
                } else {
                    displayName = "U";
                }

                btnProfile.setText(String.valueOf(displayName.charAt(0)).toUpperCase());
            }

            @Override
            public void onCancelled(DatabaseError error) {
                btnProfile.setText("U");
            }
        });
    }

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
                List<DataSnapshot> notifList = new ArrayList<>();
                for (DataSnapshot child : snapshot.getChildren()) {
                    notifList.add(0, child); // newest first
                }

                notifContainer.removeAllViews();

                if (notifList.isEmpty()) {
                    showEmptyState();
                    return;
                }

                for (int i = 0; i < notifList.size(); i++) {
                    DataSnapshot notif = notifList.get(i);

                    String title     = notif.child("title").getValue(String.class);
                    String body      = notif.child("body").getValue(String.class);
                    Long   timestamp = notif.child("timestamp").getValue(Long.class);

                    if (title == null) title = "Notification";
                    if (body  == null) body  = "";

                    String dateStr = "—";
                    if (timestamp != null) {
                        SimpleDateFormat sdf = new SimpleDateFormat("MMM d", Locale.getDefault());
                        dateStr = sdf.format(new Date(timestamp));
                    }

                    addNotifRow(title, body, dateStr, i < notifList.size() - 1);
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                showErrorState(error.getMessage());
            }
        });
    }

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

        // Right column (date + dot)
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
        dotParams.gravity = android.view.Gravity.CENTER_HORIZONTAL;
        dot.setLayoutParams(dotParams);
        dot.setBackgroundResource(R.drawable.dot_blue);
        rightCol.addView(dot);

        row.addView(rightCol);
        notifContainer.addView(row);

        // Divider
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

    private void showEmptyState() {
        TextView empty = new TextView(this);
        empty.setText("No notifications yet.");
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

    // ── Online presence ────────────────────────────────────────────
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

    private void setOnlineStatus(boolean isOnline) {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) return;
        DatabaseReference presenceRef = usersRef.child(currentUser.getUid()).child("online");
        presenceRef.setValue(isOnline);
        if (isOnline) presenceRef.onDisconnect().setValue(false);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (mAuth.getCurrentUser() == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        }
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }
}
