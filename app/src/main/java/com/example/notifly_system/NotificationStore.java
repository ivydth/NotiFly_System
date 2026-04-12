package com.example.notifly_system;

import java.util.ArrayList;
import java.util.List;

public class NotificationStore {

    private static NotificationStore instance;
    private final List<NotificationItem> items = new ArrayList<>();

    private NotificationStore() {
        seedSampleData();
    }

    public static synchronized NotificationStore getInstance() {
        if (instance == null) instance = new NotificationStore();
        return instance;
    }

    // ── Sample data — one notification per category ───────────────────────────

    private void seedSampleData() {
        items.add(new NotificationItem(
                "1",
                "System",
                "You have 3 unread messages waiting.",
                "Now",
                "Unread",
                false,
                R.drawable.avatar_teal));

        items.add(new NotificationItem(
                "2",
                "Admin",
                "Scheduled maintenance on Sunday 10 PM.",
                "Mon",
                "Announcements",
                false,
                R.drawable.avatar_teal));

        items.add(new NotificationItem(
                "3",
                "Events Bot",
                "Team meeting tomorrow at 9 AM.",
                "Tue",
                "Events",
                false,
                R.drawable.avatar_teal));

        items.add(new NotificationItem(
                "4",
                "You",
                "You starred this important reminder.",
                "Wed",
                "Starred",
                true,
                R.drawable.avatar_teal));
    }

    // ── Queries ───────────────────────────────────────────────────────────────

    public synchronized List<NotificationItem> getAll() {
        return new ArrayList<>(items);
    }

    public synchronized List<NotificationItem> getByCategory(String category) {
        List<NotificationItem> result = new ArrayList<>();
        for (NotificationItem n : items) {
            if (n.category.equalsIgnoreCase(category)) result.add(n);
        }
        return result;
    }

    public synchronized List<NotificationItem> getStarred() {
        List<NotificationItem> result = new ArrayList<>();
        for (NotificationItem n : items) {
            if (n.isStarred) result.add(n);
        }
        return result;
    }

    // ── Mutations ─────────────────────────────────────────────────────────────

    public synchronized void add(NotificationItem item) {
        for (NotificationItem n : items) {
            if (n.id.equals(item.id)) return;
        }
        items.add(item);
    }

    public synchronized void star(String id) {
        for (NotificationItem n : items) {
            if (n.id.equals(id)) {
                n.isStarred = true;
                n.category  = "Starred";
                return;
            }
        }
    }

    public synchronized void unstar(String id) {
        for (NotificationItem n : items) {
            if (n.id.equals(id)) {
                n.isStarred = false;
                if (n.category.equalsIgnoreCase("Starred")) {
                    n.category = "Unread";
                }
                return;
            }
        }
    }

    public synchronized boolean isStarred(String id) {
        for (NotificationItem n : items) {
            if (n.id.equals(id)) return n.isStarred;
        }
        return false;
    }
}
