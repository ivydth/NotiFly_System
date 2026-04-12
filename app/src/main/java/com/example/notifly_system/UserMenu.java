package com.example.notifly_system;

import android.animation.ValueAnimator;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.RippleDrawable;
import android.os.Bundle;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.animation.DecelerateInterpolator;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class UserMenu extends AppCompatActivity
        implements NotificationStore.StoreListener {   // ← live updates

    private View drawerPanel;
    private View rootLayout;
    private boolean isDrawerOpen = false;

    private View navDashboard;
    private View navNotifications;
    private View navAllInboxes;
    private View navUnread;
    private View navAnnouncements;
    private View navEvents;
    private View navSettings;
    private View navArchive;

    // ── Badges ────────────────────────────────────────────────────────────────
    // One badge per menu item that has a count
    private TextView badgeNotifications;   // existing — total unread
    private TextView badgeUnread;          // @+id/badge_unread
    private TextView badgeAnnouncements;   // @+id/badge_announcements
    private TextView badgeEvents;          // @+id/badge_events

    // Header views
    private TextView tvDrawerUsername;
    private TextView tvDrawerEmail;
    private TextView tvAvatar;

    // Firebase
    private FirebaseAuth      mAuth;
    private DatabaseReference database;

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        overridePendingTransition(0, 0);
        setContentView(R.layout.drawer_menu);

        drawerPanel = findViewById(R.id.drawer_panel);
        rootLayout  = findViewById(R.id.root_layout);

        drawerPanel.setTranslationX(-9999f);

        mAuth    = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance(
                "https://notifly-94dba-default-rtdb.asia-southeast1.firebasedatabase.app/"
        ).getReference("users");

        initViews();
        setActiveItem(navDashboard);
        setClickListeners();

        drawerPanel.getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        drawerPanel.getViewTreeObserver()
                                .removeOnGlobalLayoutListener(this);
                        drawerPanel.setTranslationX(-drawerPanel.getWidth());
                        openDrawer();
                    }
                });

        rootLayout.setOnClickListener(v -> closeDrawer());
        drawerPanel.setOnClickListener(v -> {});

        getOnBackPressedDispatcher().addCallback(this,
                new OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {
                        if (isDrawerOpen) {
                            closeDrawer();
                        } else {
                            setEnabled(false);
                            getOnBackPressedDispatcher().onBackPressed();
                        }
                    }
                });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Register for live store updates
        NotificationStore.getInstance().addListener(this);
        loadUserData();
        refreshBadges();   // show counts immediately on open
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Unregister — no updates needed while offscreen
        NotificationStore.getInstance().removeListener(this);
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(0, 0);
    }

    // ── StoreListener ─────────────────────────────────────────────────────────

    /**
     * Called whenever a notification is starred, unstarred, read, or added.
     * Runs badge refresh on the UI thread.
     */
    @Override
    public void onStoreChanged() {
        runOnUiThread(this::refreshBadges);
    }

    // ── Badge refresh ─────────────────────────────────────────────────────────

    /**
     * Reads live counts from NotificationStore and pushes them
     * to each badge TextView in the drawer.
     */
    private void refreshBadges() {
        NotificationStore store = NotificationStore.getInstance();

        // Unread = items not yet read in the Unread category
        int unreadCount         = store.getUnreadCount();

        // Announcements and Events = total items in that category
        int announcementsCount  = store.getByCategory("Announcements").size();
        int eventsCount         = store.getByCategory("Events").size();

        // Total for the top-level Notifications badge =
        // unread + announcements + events (anything the user hasn't read)
        int totalCount = unreadCount + announcementsCount + eventsCount;

        setBadge(badgeNotifications, totalCount);
        setBadge(badgeUnread,        unreadCount);
        setBadge(badgeAnnouncements, announcementsCount);
        setBadge(badgeEvents,        eventsCount);
    }

    /** Shows or hides a badge and sets its text. */
    private void setBadge(TextView badge, int count) {
        if (badge == null) return;
        if (count <= 0) {
            badge.setVisibility(View.GONE);
        } else {
            badge.setVisibility(View.VISIBLE);
            badge.setText(count > 99 ? "99+" : String.valueOf(count));
        }
    }

    // ── Init ──────────────────────────────────────────────────────────────────

    private void initViews() {
        navDashboard     = findViewById(R.id.nav_dashboard);
        navNotifications = findViewById(R.id.nav_notifications);
        navAllInboxes    = findViewById(R.id.nav_all_inboxes);
        navUnread        = findViewById(R.id.nav_unread);
        navAnnouncements = findViewById(R.id.nav_announcements);
        navEvents        = findViewById(R.id.nav_events);
        navSettings      = findViewById(R.id.nav_settings);
        navArchive       = findViewById(R.id.nav_archive);

        // Badges
        badgeNotifications = findViewById(R.id.badge_notifications);
        badgeUnread        = findViewById(R.id.badge_unread);
        badgeAnnouncements = findViewById(R.id.badge_announcements);
        badgeEvents        = findViewById(R.id.badge_events);

        // Header
        tvDrawerUsername = findViewById(R.id.tv_drawer_username);
        tvDrawerEmail    = findViewById(R.id.tv_drawer_email);
        tvAvatar         = findViewById(R.id.tv_avatar);

        applyRipple(navDashboard);
        applyRipple(navNotifications);
        applyRipple(navAllInboxes);
        applyRipple(navUnread);
        applyRipple(navAnnouncements);
        applyRipple(navEvents);
        applyRipple(navSettings);
        applyRipple(navArchive);
    }

    // ── Firebase ──────────────────────────────────────────────────────────────

    private void loadUserData() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) return;

        database.child(currentUser.getUid())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        if (!snapshot.exists()) return;

                        String firstName = snapshot.child("firstName").getValue(String.class);
                        String username  = snapshot.child("username").getValue(String.class);
                        String email     = snapshot.child("email").getValue(String.class);

                        String displayName;
                        if (username != null && !username.isEmpty()) {
                            displayName = username;
                        } else if (firstName != null && !firstName.isEmpty()) {
                            displayName = firstName;
                        } else {
                            displayName = "User";
                        }

                        String displayEmail = (email != null && !email.isEmpty())
                                ? email
                                : (currentUser.getEmail() != null
                                        ? currentUser.getEmail() : "");

                        if (tvDrawerUsername != null) tvDrawerUsername.setText(displayName);
                        if (tvDrawerEmail    != null) tvDrawerEmail.setText(displayEmail);
                        if (tvAvatar        != null)
                            tvAvatar.setText(
                                    String.valueOf(displayName.charAt(0)).toUpperCase());
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        if (tvDrawerUsername != null) tvDrawerUsername.setText("User");
                        if (tvAvatar        != null) tvAvatar.setText("U");
                        if (tvDrawerEmail   != null) tvDrawerEmail.setText("");
                    }
                });
    }

    // ── Ripple ────────────────────────────────────────────────────────────────

    private void applyRipple(View view) {
        ColorStateList rippleColor =
                ColorStateList.valueOf(Color.parseColor("#521ABFB8"));
        Drawable existingBg = view.getBackground();
        RippleDrawable ripple = new RippleDrawable(rippleColor, existingBg, null);
        view.setBackground(ripple);
    }

    // ── Click listeners ───────────────────────────────────────────────────────

    private void setClickListeners() {
        navDashboard.setOnClickListener(v -> {
            setActiveItem(navDashboard);
            onNavDashboardClicked();
        });

        navNotifications.setOnClickListener(v -> {
            setActiveItem(navNotifications);
            onNavNotificationsClicked();
        });

        navAllInboxes.setOnClickListener(v -> onNavAllInboxesClicked());

        navUnread.setOnClickListener(v -> onNavUnreadClicked());

        navAnnouncements.setOnClickListener(v -> onNavAnnouncementsClicked());

        navEvents.setOnClickListener(v -> onNavEventsClicked());

        navSettings.setOnClickListener(v -> {
            setActiveItem(navSettings);
            onNavSettingsClicked();
        });

        navArchive.setOnClickListener(v -> {
            setActiveItem(navArchive);
            onNavArchiveClicked();
        });
    }

    // ── Navigation actions ────────────────────────────────────────────────────

    private void onNavDashboardClicked() {
        startActivity(new Intent(this, UserActivity.class));
        closeDrawer();
    }

    private void onNavNotificationsClicked() {
        startActivity(new Intent(this, NotifActivity.class));
        closeDrawer();
    }

    private void onNavAllInboxesClicked() {
        Toast.makeText(this, "All Inboxes", Toast.LENGTH_SHORT).show();
        closeDrawer();
    }

    private void onNavUnreadClicked() {
        startActivity(new Intent(this, UnrActivity.class));
        closeDrawer();
    }

    private void onNavAnnouncementsClicked() {
        startActivity(new Intent(this, AnnActivity.class));
        closeDrawer();
    }

    private void onNavEventsClicked() {
        startActivity(new Intent(this, EventsActivity.class));
        closeDrawer();
    }

    private void onNavSettingsClicked() {
        startActivity(new Intent(this, SettActivity.class));
        closeDrawer();
    }

    private void onNavArchiveClicked() {
        startActivity(new Intent(this, ArcActivity.class));
        closeDrawer();
    }

    // ── Drawer animation ──────────────────────────────────────────────────────

    public void openDrawer() {
        float from = drawerPanel.getTranslationX();
        ValueAnimator animator = ValueAnimator.ofFloat(from, 0f);
        animator.setDuration(350);
        animator.setInterpolator(new DecelerateInterpolator(2f));
        animator.addUpdateListener(
                a -> drawerPanel.setTranslationX((float) a.getAnimatedValue()));
        animator.start();
        isDrawerOpen = true;
    }

    public void closeDrawer() {
        float panelWidth = drawerPanel.getWidth();
        ValueAnimator animator = ValueAnimator.ofFloat(0f, -panelWidth);
        animator.setDuration(300);
        animator.setInterpolator(new DecelerateInterpolator(2f));
        animator.addUpdateListener(
                a -> drawerPanel.setTranslationX((float) a.getAnimatedValue()));
        animator.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
                finish();
            }
        });
        animator.start();
        isDrawerOpen = false;
    }

    // ── Active item highlight ─────────────────────────────────────────────────

    private void setActiveItem(View activeView) {
        View[] items = {navDashboard, navNotifications, navSettings, navArchive};
        for (View item : items) {
            if (item == null) continue;
            if (item == activeView) {
                Drawable activeBg = getDrawable(R.drawable.nav_item_active_bg);
                ColorStateList rippleColor =
                        ColorStateList.valueOf(Color.parseColor("#521ABFB8"));
                item.setBackground(new RippleDrawable(rippleColor, activeBg, null));
                TextView label = getFirstTextView(item);
                if (label != null) {
                    label.setTextColor(Color.parseColor("#1ABFB8"));
                    label.setTypeface(label.getTypeface(), Typeface.BOLD);
                }
            } else {
                applyRipple(item);
                TextView label = getFirstTextView(item);
                if (label != null) {
                    label.setTextColor(getColor(android.R.color.white));
                    label.setTypeface(Typeface.DEFAULT);
                }
            }
        }
    }

    private TextView getFirstTextView(View view) {
        if (view instanceof android.view.ViewGroup) {
            android.view.ViewGroup group = (android.view.ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                View child = group.getChildAt(i);
                if (child instanceof TextView) return (TextView) child;
            }
        }
        return null;
    }

    // ── Legacy helper (kept for compatibility) ────────────────────────────────

    public void setNotificationBadge(int count) {
        setBadge(badgeNotifications, count);
    }
}
