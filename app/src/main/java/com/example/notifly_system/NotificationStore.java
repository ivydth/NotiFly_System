package com.example.notifly_system;

import java.util.ArrayList;
import java.util.List;

public class NotificationStore {

    public interface StoreListener {
        void onStoreChanged();
    }

    private static NotificationStore instance;
    private final List<NotificationItem> items     = new ArrayList<>();
    private final List<StoreListener>    listeners = new ArrayList<>();

    private NotificationStore() { seedSampleData(); }

    public static synchronized NotificationStore getInstance() {
        if (instance == null) instance = new NotificationStore();
        return instance;
    }

    // ── Listener management ───────────────────────────────────────────────────

    public synchronized void addListener(StoreListener l) {
        if (!listeners.contains(l)) listeners.add(l);
    }

    public synchronized void removeListener(StoreListener l) {
        listeners.remove(l);
    }

    private void notifyListeners() {
        for (StoreListener l : new ArrayList<>(listeners)) l.onStoreChanged();
    }

    // ── Seed data ─────────────────────────────────────────────────────────────

    private void seedSampleData() {
        items.add(new NotificationItem(
                "1", "System",
                "You have 3 unread messages waiting.",
                "Now", "Unread", false, R.drawable.avatar_teal));

        items.add(new NotificationItem(
                "2", "Admin",
                "Scheduled maintenance on Sunday 10 PM.",
                "Mon", "Announcements", false, R.drawable.avatar_teal));

        items.add(new NotificationItem(
                "3", "Events Bot",
                "Team meeting tomorrow at 9 AM.",
                "Tue", "Events", false, R.drawable.avatar_teal));

        NotificationItem starred = new NotificationItem(
                "4", "You",
                "You starred this important reminder.",
                "Wed", "Starred", true, R.drawable.avatar_teal);
        items.add(starred);
    }

    // ── Queries ───────────────────────────────────────────────────────────────

    /** All non-archived items. */
    public synchronized List<NotificationItem> getAll() {
        List<NotificationItem> result = new ArrayList<>();
        for (NotificationItem n : items) {
            if (!n.isArchived) result.add(n);
        }
        return result;
    }

    /** Non-archived items matching originalCategory. */
    public synchronized List<NotificationItem> getByCategory(String category) {
        List<NotificationItem> result = new ArrayList<>();
        for (NotificationItem n : items) {
            if (!n.isArchived && n.originalCategory.equalsIgnoreCase(category))
                result.add(n);
        }
        return result;
    }

    /** Non-archived starred items. */
    public synchronized List<NotificationItem> getStarred() {
        List<NotificationItem> result = new ArrayList<>();
        for (NotificationItem n : items) {
            if (!n.isArchived && n.isStarred) result.add(n);
        }
        return result;
    }

    /** Non-archived unread items. */
    public synchronized List<NotificationItem> getUnread() {
        List<NotificationItem> result = new ArrayList<>();
        for (NotificationItem n : items) {
            if (!n.isArchived
                    && n.originalCategory.equalsIgnoreCase("Unread")
                    && !n.isRead)
                result.add(n);
        }
        return result;
    }

    /** Count of non-archived unread items. */
    public synchronized int getUnreadCount() {
        int count = 0;
        for (NotificationItem n : items) {
            if (!n.isArchived
                    && n.originalCategory.equalsIgnoreCase("Unread")
                    && !n.isRead) count++;
        }
        return count;
    }

    /** All archived items. */
    public synchronized List<NotificationItem> getArchived() {
        List<NotificationItem> result = new ArrayList<>();
        for (NotificationItem n : items) {
            if (n.isArchived) result.add(n);
        }
        return result;
    }

    // ── Mutations ─────────────────────────────────────────────────────────────

    public synchronized void add(NotificationItem item) {
        for (NotificationItem n : items) {
            if (n.id.equals(item.id)) return;
        }
        items.add(item);
        notifyListeners();
    }

    public synchronized void star(String id) {
        for (NotificationItem n : items) {
            if (n.id.equals(id)) {
                n.isStarred = true;
                n.category  = "Starred";
                notifyListeners();
                return;
            }
        }
    }

    public synchronized void unstar(String id) {
        for (NotificationItem n : items) {
            if (n.id.equals(id)) {
                n.isStarred = false;
                n.category  = n.originalCategory;
                notifyListeners();
                return;
            }
        }
    }

    public synchronized void markRead(String id) {
        for (NotificationItem n : items) {
            if (n.id.equals(id) && !n.isRead) {
                n.isRead = true;
                notifyListeners();
                return;
            }
        }
    }

    /**
     * Archives an item — disappears from all normal lists
     * and appears only in ArcActivity.
     */
    public synchronized void archive(String id) {
        for (NotificationItem n : items) {
            if (n.id.equals(id)) {
                n.isArchived = true;
                notifyListeners();
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
