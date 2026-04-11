package com.example.notifly_system;

import android.os.Bundle;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

public class UserMenu extends AppCompatActivity {

    private DrawerLayout drawerLayout;

    private View navDashboard;
    private View navNotifications;
    private View navAllInboxes;
    private View navUnread;
    private View navAnnouncements;
    private View navPromotions;
    private View navSettings;
    private View navArchive;

    private TextView badgeNotifications;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.drawer_menu);

        drawerLayout = findViewById(R.id.drawer_layout);

        // Disable the dark scrim so background stays fully visible
        drawerLayout.setScrimColor(android.graphics.Color.TRANSPARENT);

        initViews();
        setActiveItem(navDashboard);
        setClickListeners();

        drawerLayout.addDrawerListener(new DrawerLayout.DrawerListener() {
            @Override
            public void onDrawerSlide(@NonNull View drawerView, float slideOffset) {
                // Slide drawer in from left — negative start, 0 when fully open
                float interpolated = new DecelerateInterpolator(2f).getInterpolation(slideOffset);
                drawerView.setTranslationX(-drawerView.getWidth() * (1f - interpolated));
            }

            @Override
            public void onDrawerOpened(@NonNull View drawerView) {
                drawerView.setTranslationX(0f);
            }

            @Override
            public void onDrawerClosed(@NonNull View drawerView) {
                drawerView.setTranslationX(-drawerView.getWidth());
            }

            @Override
            public void onDrawerStateChanged(int newState) {}
        });

        drawerLayout.openDrawer(GravityCompat.START);

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (drawerLayout != null && drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START);
                } else {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });
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
    }

    private void setClickListeners() {
        navDashboard.setOnClickListener(v -> {
            setActiveItem(navDashboard);
            onNavDashboardClicked();
            closeDrawer();
        });

        navNotifications.setOnClickListener(v -> {
            setActiveItem(navNotifications);
            onNavNotificationsClicked();
            closeDrawer();
        });

        navAllInboxes.setOnClickListener(v -> {
            onNavAllInboxesClicked();
            closeDrawer();
        });

        navUnread.setOnClickListener(v -> {
            onNavUnreadClicked();
            closeDrawer();
        });

        navAnnouncements.setOnClickListener(v -> {
            onNavAnnouncementsClicked();
            closeDrawer();
        });

        navPromotions.setOnClickListener(v -> {
            onNavPromotionsClicked();
            closeDrawer();
        });

        navSettings.setOnClickListener(v -> {
            setActiveItem(navSettings);
            onNavSettingsClicked();
            closeDrawer();
        });

        navArchive.setOnClickListener(v -> {
            setActiveItem(navArchive);
            onNavArchiveClicked();
            closeDrawer();
        });
    }

    private void onNavDashboardClicked() {
        Toast.makeText(this, "Dashboard", Toast.LENGTH_SHORT).show();
    }

    private void onNavNotificationsClicked() {
        Toast.makeText(this, "Notifications", Toast.LENGTH_SHORT).show();
    }

    private void onNavAllInboxesClicked() {
        Toast.makeText(this, "All Inboxes", Toast.LENGTH_SHORT).show();
    }

    private void onNavUnreadClicked() {
        Toast.makeText(this, "Unread", Toast.LENGTH_SHORT).show();
    }

    private void onNavAnnouncementsClicked() {
        Toast.makeText(this, "Announcements", Toast.LENGTH_SHORT).show();
    }

    private void onNavPromotionsClicked() {
        Toast.makeText(this, "Promotions", Toast.LENGTH_SHORT).show();
    }

    private void onNavSettingsClicked() {
        Toast.makeText(this, "Settings", Toast.LENGTH_SHORT).show();
    }

    private void onNavArchiveClicked() {
        Toast.makeText(this, "Archive", Toast.LENGTH_SHORT).show();
    }

    private void setActiveItem(View activeView) {
        View[] items = {navDashboard, navNotifications, navSettings, navArchive};
        for (View item : items) {
            if (item == null) continue;
            if (item == activeView) {
                item.setBackgroundResource(R.drawable.nav_item_active_bg);
                TextView label = getFirstTextView(item);
                if (label != null) {
                    label.setTextColor(android.graphics.Color.parseColor("#1ABFB8"));
                    label.setTypeface(label.getTypeface(), android.graphics.Typeface.BOLD);
                }
            } else {
                item.setBackgroundResource(android.R.color.transparent);
                TextView label = getFirstTextView(item);
                if (label != null) {
                    label.setTextColor(getColor(android.R.color.white));
                    label.setTypeface(android.graphics.Typeface.DEFAULT);
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

    private void closeDrawer() {
        if (drawerLayout != null && drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        }
    }

    public void openDrawer() {
        if (drawerLayout != null) {
            drawerLayout.openDrawer(GravityCompat.START);
        }
    }
}
