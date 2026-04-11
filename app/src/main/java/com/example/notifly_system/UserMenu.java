package com.example.notifly_system;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

public class UserMenu extends AppCompatActivity {

    private DrawerLayout drawerLayout;

    // Main section
    private View navDashboard;
    private View navNotifications;
    private View navAllInboxes;
    private View navUnread;
    private View navAnnouncements;
    private View navPromotions;

    // Options section
    private View navSettings;
    private View navArchive;

    // Badge
    private TextView badgeNotifications;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.drawer_menu); // Replace with your actual layout that includes the drawer

        drawerLayout = findViewById(R.id.drawer_layout); // Your DrawerLayout ID in the parent layout

        initViews();
        setActiveItem(navDashboard);
        setClickListeners();
    }

    private void initViews() {
        // Main section nav items
        navDashboard      = findViewById(R.id.nav_dashboard);
        navNotifications  = findViewById(R.id.nav_notifications);
        navAllInboxes     = findViewById(R.id.nav_all_inboxes);
        navUnread         = findViewById(R.id.nav_unread);
        navAnnouncements  = findViewById(R.id.nav_announcements);
        navPromotions     = findViewById(R.id.nav_promotions);

        // Options section nav items
        navSettings = findViewById(R.id.nav_settings);
        navArchive  = findViewById(R.id.nav_archive);

        // Badge
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

    // -------------------------------------------------------------------------
    // Navigation handlers — replace Toast stubs with your actual Fragment/Intent
    // -------------------------------------------------------------------------

    private void onNavDashboardClicked() {
        Toast.makeText(this, "Dashboard", Toast.LENGTH_SHORT).show();
        // e.g. getSupportFragmentManager().beginTransaction()
        //          .replace(R.id.fragment_container, new DashboardFragment())
        //          .commit();
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

    // -------------------------------------------------------------------------
    // Active state helper
    // Toggles the active background (@drawable/nav_item_active_bg) on the
    // selected top-level item and resets all others.
    // Sub-items (All Inboxes, Unread, Announcements, Promotions) use a
    // selectableItemBackground so they are excluded from active tracking.
    // -------------------------------------------------------------------------

    private final View[] topLevelItems = new View[0]; // populated lazily below

    private void setActiveItem(View activeView) {
        View[] items = {navDashboard, navNotifications, navSettings, navArchive};
        for (View item : items) {
            if (item == null) continue;
            if (item == activeView) {
                item.setBackgroundResource(R.drawable.nav_item_active_bg);
                // Tint label teal for the active item
                TextView label = getFirstTextView(item);
                if (label != null) {
                    label.setTextColor(getColor(R.color.teal_primary)); // #1ABFB8
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

    /** Walks the immediate children of a ViewGroup to find the first TextView. */
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

    // -------------------------------------------------------------------------
    // Badge helpers
    // -------------------------------------------------------------------------

    /** Update the notification badge count. Pass 0 to hide it. */
    public void setNotificationBadge(int count) {
        if (badgeNotifications == null) return;
        if (count <= 0) {
            badgeNotifications.setVisibility(View.GONE);
        } else {
            badgeNotifications.setVisibility(View.VISIBLE);
            badgeNotifications.setText(count > 99 ? "99+" : String.valueOf(count));
        }
    }

    // -------------------------------------------------------------------------
    // Drawer helpers
    // -------------------------------------------------------------------------

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

    @Override
    public void onBackPressed() {
        if (drawerLayout != null && drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }
}
