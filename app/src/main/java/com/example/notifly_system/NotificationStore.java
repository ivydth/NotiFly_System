package com.example.notifly_system;

import java.util.ArrayList;
import java.util.List;

public class NotificationStore {

    public interface StoreListener {
        void onStoreChanged();
    }

    private static NotificationStore instance;
    private final List<NotificationItem> items       = new ArrayList<>();
    private final List<StoreListener>    listeners   = new ArrayList<>();
    private final List<String>           seenIds     = new ArrayList<>();

    private int     newCount      = 0;
    private boolean samplesLoaded = false;

    private NotificationStore() {
        loadSampleData();
    }

    public static synchronized NotificationStore getInstance() {
        if (instance == null) instance = new NotificationStore();
        return instance;
    }

    // ── Sample Data ───────────────────────────────────────────────────────────

    /**
     * One sample per category so every card and section has a placeholder
     * while Firebase loads. Samples are removed the moment real data arrives.
     *
     * Constructor: id, senderName, message, dateLabel, category, isStarred, avatarResId
     */
    private void loadSampleData() {
        if (samplesLoaded) return;
        samplesLoaded = true;

        // 1 sample for Unread
        items.add(new NotificationItem(
                "sample_unread",
                "System",
                "You have a new message waiting for you.",
                "Today",
                "Unread",
                false,
                R.drawable.avatar_teal
        ));

        // 1 sample for Announcements
        items.add(new NotificationItem(
                "sample_announcement",
                "Admin",
                "Welcome to NotiFly! Stay tuned for updates.",
                "Today",
                "Announcements",
                false,
                R.drawable.avatar_teal
        ));

        // 1 sample for Events
        items.add(new NotificationItem(
                "sample_event",
                "System",
                "Upcoming event: Foundation Day on April 20.",
                "Yesterday",
                "Events",
                false,
                R.drawable.avatar_teal
        ));

        // 1 sample for Starred (pre-starred so Starred card shows data)
        items.add(new NotificationItem(
                "sample_starred",
                "Registrar",
                "Enrollment for next semester is now open.",
                "Apr 10",
                "Announcements",
                true,
                R.drawable.avatar_teal
        ));

        // Mark all samples as already seen — they should NOT count as new
        for (NotificationItem n : items) {
            seenIds.add(n.id);
        }
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

    // ── Firebase sync ─────────────────────────────────────────────────────────

    /**
     * Called by FirebaseNotifSyncService whenever /notifications changes.
     * - Removes sample placeholders when real data arrives.
     * - Merges new Firebase items without duplicating.
     * - Bell badge = count of Firebase IDs not yet in seenIds.
     */
    public synchronized void syncFromFirebase(List<NotificationItem> incoming) {
        // Strip samples only when real data exists
        if (!incoming.isEmpty()) {
            items.removeIf(n -> n.id.startsWith("sample_"));
        }

        // Merge incoming — skip duplicates
        for (NotificationItem incomingItem : incoming) {
            boolean exists = false;
            for (NotificationItem existing : items) {
                if (existing.id.equals(incomingItem.id)) {
                    exists = true;
                    break;
                }
            }
            if (!exists) {
                items.add(incomingItem);
            }
        }

        // Recalculate new count:
        // any Firebase item whose ID is NOT in seenIds is "new"
        newCount = 0;
        for (NotificationItem n : items) {
            if (!n.id.startsWith("sample_") && !seenIds.contains(n.id)) {
                newCount++;
            }
        }

        notifyListeners();
    }

    /**
     * Call when the user opens the bell / notification screen.
     * Marks all current items as seen so badge resets to 0.
     */
    public synchronized void markAllSeen() {
        for (NotificationItem n : items) {
            if (!seenIds.contains(n.id)) {
                seenIds.add(n.id);
            }
        }
        newCount = 0;
        notifyListeners();
    }

    /** How many NEW notifications arrived since the user last opened bell. */
    public synchronized int getNewCount() {
        return newCount;
    }

    // ── Queries ───────────────────────────────────────────────────────────────

    public synchronized List<NotificationItem> getAll() {
        List<NotificationItem> result = new ArrayList<>();
        for (NotificationItem n : items) {
            if (!n.isArchived) result.add(n);
        }
        return result;
    }

    public synchronized List<NotificationItem> getByCategory(String category) {
        List<NotificationItem> result = new ArrayList<>();
        for (NotificationItem n : items) {
            if (!n.isArchived && n.originalCategory.equalsIgnoreCase(category))
                result.add(n);
        }
        return result;
    }

    public synchronized List<NotificationItem> getStarred() {
        List<NotificationItem> result = new ArrayList<>();
        for (NotificationItem n : items) {
            if (!n.isArchived && n.isStarred) result.add(n);
        }
        return result;
    }

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

    public synchronized int getUnreadCount() {
        int count = 0;
        for (NotificationItem n : items) {
            if (!n.isArchived
                    && n.originalCategory.equalsIgnoreCase("Unread")
                    && !n.isRead) count++;
        }
        return count;
    }

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
                notifyListeners();
                return;
            }
        }
    }

    public synchronized void unstar(String id) {
        for (NotificationItem n : items) {
            if (n.id.equals(id)) {
                n.isStarred = false;
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

    public synchronized void markUnread(String id) {
        for (NotificationItem n : items) {
            if (n.id.equals(id) && n.isRead) {
                n.isRead = false;
                notifyListeners();
                return;
            }
        }
    }

    public synchronized void archive(String id) {
        for (NotificationItem n : items) {
            if (n.id.equals(id)) {
                n.isArchived = true;
                notifyListeners();
                return;
            }
        }
    }

    public synchronized void unarchive(String id) {
        for (NotificationItem n : items) {
            if (n.id.equals(id)) {
                n.isArchived = false;
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
