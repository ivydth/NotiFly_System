package com.example.notifly_system;

import android.animation.ValueAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.animation.DecelerateInterpolator;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

public class UserMenu extends AppCompatActivity {

    private View drawerPanel;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        overridePendingTransition(0, 0);
        setContentView(R.layout.drawer_menu);

        drawerPanel = findViewById(R.id.drawer_panel);
        drawerPanel.setTranslationX(-9999f);

        initViews();
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
        animator.start();
        isDrawerOpen = false;
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
}
