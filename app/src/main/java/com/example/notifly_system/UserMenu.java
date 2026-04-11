package com.example.notifly_system;

import android.animation.ValueAnimator;
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

public class UserMenu extends AppCompatActivity {

    private View drawerPanel;
    private View rootLayout;
    private boolean isDrawerOpen = false;

    private View navDashboard;
    private View navNotifications;
    private View navAllInboxes;
    private View navUnread;
    private View navAnnouncements;
    private View navPromotions;
    private View navSettings;
    private View navArchive;

    private TextView badgeNotifications;

    // Header views
    private TextView tvDrawerUsername;
    private TextView tvDrawerEmail;
    private TextView tvAvatar;

    // Firebase
    private FirebaseAuth mAuth;
    private DatabaseReference database;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        overridePendingTransition(0, 0);
        setContentView(R.layout.drawer_menu);

        drawerPanel = findViewById(R.id.drawer_panel);
        rootLayout  = findViewById(R.id.root_layout);

        drawerPanel.setTranslationX(-9999f);

        initViews();
        loadUserData();
        setActiveItem(navDashboard);
        setClickListeners();

        drawerPanel.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                drawerPanel.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                drawerPanel.setTranslationX(-drawerPanel.getWidth());
                openDrawer();
            }
        });

        rootLayout.setOnClickListener(v -> closeDrawer());
        drawerPanel.setOnClickListener(v -> {});

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
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
    public void finish() {
        super.finish();
        overridePendingTransition(0, 0);
    }

    private void initViews() {
        navDashboard     = findViewById(R.id.nav_dashboard);
        navNotifications = findViewById(R.id.nav_notifications);
        navAllInboxes    = findViewById(R.id.nav_all_inboxes);
        navUnread        = findViewById(R.id.nav_unread);
        navAnnouncements = findViewById(R.id.nav_announcements);
        navPromotions    = findViewById(R.id.nav_promotions);
        navSettings      = findViewById(R.id.nav_settings);
        navArchive       = findViewById(R.id.nav_archive);
        badgeNotifications = findViewById(R.id.badge_notifications);

        tvDrawerUsername = findViewById(R.id.tv_drawer_username);
        tvDrawerEmail    = findViewById(R.id.tv_drawer_email);
        tvAvatar         = findViewById(R.id.tv_avatar);

        applyRipple(navDashboard);
        applyRipple(navNotifications);
        applyRipple(navAllInboxes);
        applyRipple(navUnread);
        applyRipple(navAnnouncements);
        applyRipple(navPromotions);
        applyRipple(navSettings);
        applyRipple(navArchive);
    }

    private void loadUserData() {
        mAuth    = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance("https://notifly-94dba-default-rtdb.asia-southeast1.firebasedatabase.app/")
                .getReference("users");

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) return;

        String userId = currentUser.getUid();

        database.child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (!snapshot.exists()) return;

                String firstName = snapshot.child("firstName").getValue(String.class);
                String username  = snapshot.child("username").getValue(String.class);
                String email     = snapshot.child("email").getValue(String.class);

                // resolve display name
                String displayName;
                if (username != null && !username.isEmpty()) {
                    displayName = username;
                } else if (firstName != null && !firstName.isEmpty()) {
                    displayName = firstName;
                } else {
                    displayName = "User";
                }

                // fallback to Firebase Auth email
                String displayEmail = (email != null && !email.isEmpty())
                        ? email
                        : (currentUser.getEmail() != null ? currentUser.getEmail() : "");

                // set header views
                if (tvDrawerUsername != null)
                    tvDrawerUsername.setText(displayName);

                if (tvDrawerEmail != null)
                    tvDrawerEmail.setText(displayEmail);

                if (tvAvatar != null)
                    tvAvatar.setText(String.valueOf(displayName.charAt(0)).toUpperCase());
            }

            @Override
            public void onCancelled(DatabaseError error) {
                if (tvDrawerUsername != null) tvDrawerUsername.setText("User");
                if (tvAvatar != null) tvAvatar.setText("U");
            }
        });
    }

    private void applyRipple(View view) {
        ColorStateList rippleColor = ColorStateList.valueOf(Color.parseColor("#521ABFB8"));
        Drawable existingBg = view.getBackground();
        RippleDrawable ripple = new RippleDrawable(rippleColor, existingBg, null);
        view.setBackground(ripple);
    }

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
        navPromotions.setOnClickListener(v -> onNavPromotionsClicked());

        navSettings.setOnClickListener(v -> {
            setActiveItem(navSettings);
            onNavSettingsClicked();
        });

        navArchive.setOnClickListener(v -> {
            setActiveItem(navArchive);
            onNavArchiveClicked();
        });
    }

    public void openDrawer() {
        float from = drawerPanel.getTranslationX();
        ValueAnimator animator = ValueAnimator.ofFloat(from, 0f);
        animator.setDuration(350);
        animator.setInterpolator(new DecelerateInterpolator(2f));
        animator.addUpdateListener(a -> drawerPanel.setTranslationX((float) a.getAnimatedValue()));
        animator.start();
        isDrawerOpen = true;
    }

    public void closeDrawer() {
        float panelWidth = drawerPanel.getWidth();
        ValueAnimator animator = ValueAnimator.ofFloat(0f, -panelWidth);
        animator.setDuration(300);
        animator.setInterpolator(new DecelerateInterpolator(2f));
        animator.addUpdateListener(a -> drawerPanel.setTranslationX((float) a.getAnimatedValue()));
        animator.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
                finish();
            }
        });
        animator.start();
        isDrawerOpen = false;
    }

    private void onNavDashboardClicked() {
        Toast.makeText(this, "Dashboard", Toast.LENGTH_SHORT).show();
        closeDrawer();
    }

    private void onNavNotificationsClicked() {
        Toast.makeText(this, "Notifications", Toast.LENGTH_SHORT).show();
        closeDrawer();
    }

    private void onNavAllInboxesClicked() {
        Toast.makeText(this, "All Inboxes", Toast.LENGTH_SHORT).show();
        closeDrawer();
    }

    private void onNavUnreadClicked() {
        Toast.makeText(this, "Unread", Toast.LENGTH_SHORT).show();
        closeDrawer();
    }

    private void onNavAnnouncementsClicked() {
        Toast.makeText(this, "Announcements", Toast.LENGTH_SHORT).show();
        closeDrawer();
    }

    private void onNavPromotionsClicked() {
        Toast.makeText(this, "Promotions", Toast.LENGTH_SHORT).show();
        closeDrawer();
    }

    private void onNavSettingsClicked() {
        Toast.makeText(this, "Settings", Toast.LENGTH_SHORT).show();
        closeDrawer();
    }

    private void onNavArchiveClicked() {
        Toast.makeText(this, "Archive", Toast.LENGTH_SHORT).show();
        closeDrawer();
    }

    private void setActiveItem(View activeView) {
        View[] items = {navDashboard, navNotifications, navSettings, navArchive};
        for (View item : items) {
            if (item == null) continue;
            if (item == activeView) {
                Drawable activeBg = getDrawable(R.drawable.nav_item_active_bg);
                ColorStateList rippleColor = ColorStateList.valueOf(Color.parseColor("#521ABFB8"));
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

    public void setNotificationBadge(int count) {
        if (badgeNotifications == null) return;
        if (count <= 0) {
            badgeNotifications.setVisibility(View.GONE);
        } else {
            badgeNotifications.setVisibility(View.VISIBLE);
            badgeNotifications.setText(count > 99 ? "99+" : String.valueOf(count));
        }
    }
}
