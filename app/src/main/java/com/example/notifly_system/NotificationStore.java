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
    private final List<String>           seenIds   = new ArrayList<>();

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
     * One sample per category so every card has a placeholder
     * while Firebase loads. Samples are removed when real data arrives.
     * Samples are pre-marked seen so they never trigger the bell badge.
     *
     * Constructor: id, senderName, message, dateLabel, category, isStarred, avatarResId
     */
    private void loadSampleData() {
        if (samplesLoaded) return;
        samplesLoaded = true;

        items.add(new NotificationItem(
                "sample_unread",
                "System",
                "You have a new message waiting for you.",
                "Today",
                "Unread",
                false,
                R.drawable.avatar_teal
        ));

        items.add(new NotificationItem(
                "sample_announcement",
                "Admin",
                "Welcome to NotiFly! Stay tuned for updates.",
                "Today",
                "Announcements",
                false,
                R.drawable.avatar_teal
        ));

        items.add(new NotificationItem(
                "sample_event",
                "System",
                "Upcoming event: Foundation Day on April 20.",
                "Yesterday",
                "Events",
                false,
                R.drawable.avatar_teal
        ));

        // Pre-starred so Starred card shows data on first load
        items.add(new NotificationItem(
                "sample_starred",
                "Registrar",
                "Enrollment for next semester is now open.",
                "Apr 10",
                "Announcements",
                true,
                R.drawable.avatar_teal
        ));

        // Pre-mark all samples as seen — must NOT trigger the bell badge
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
     * Called by FirebaseNotifSyncService / UserActivity realtime listener.
     * Strips samples when real data arrives, merges without duplicating,
     * recalculates bell badge count.
     */
    public synchronized void syncFromFirebase(List<NotificationItem> incoming) {
        if (!incoming.isEmpty()) {
            items.removeIf(n -> n.id.startsWith("sample_"));
        }

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

        // Bell badge = items the user hasn't seen yet
        newCount = 0;
        for (NotificationItem n : items) {
            if (!n.id.startsWith("sample_") && !seenIds.contains(n.id)) {
                newCount++;
            }
        }

        notifyListeners();
    }

    /**
     * Call when the user taps the bell / opens NotifActivity1.
     * Badge resets to 0. Notifications are NOT removed — they stay permanently.
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

    /** Bell badge number. Resets to 0 on markAllSeen(). */
    public synchronized int getNewCount() {
        return newCount;
    }

    // ── Queries ───────────────────────────────────────────────────────────────

    /** All non-archived notifications. */
    public synchronized List<NotificationItem> getAll() {
        List<NotificationItem> result = new ArrayList<>();
        for (NotificationItem n : items) {
            if (!n.isArchived) result.add(n);
        }
        return result;
    }

    /** Non-archived notifications matching a specific category. */
    public synchronized List<NotificationItem> getByCategory(String category) {
        List<NotificationItem> result = new ArrayList<>();
        for (NotificationItem n : items) {
            if (!n.isArchived && n.originalCategory.equalsIgnoreCase(category))
                result.add(n);
        }
        return result;
    }

    /**
     * ALL starred notifications — includes BOTH archived and non-archived.
     * ✅ This is the key fix: removing the !isArchived filter means that
     * starring a notification inside ArcActivity immediately updates the
     * Starred count on the UserActivity dashboard, and unstarring removes it.
     */
    public synchronized List<NotificationItem> getStarred() {
        List<NotificationItem> result = new ArrayList<>();
        for (NotificationItem n : items) {
            if (n.isStarred) result.add(n); // ✅ no isArchived filter
        }
        return result;
    }

    /** Unread notifications — not archived, category Unread, not yet read. */
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

    /** Count of unread notifications. */
    public synchronized int getUnreadCount() {
        int count = 0;
        for (NotificationItem n : items) {
            if (!n.isArchived
                    && n.originalCategory.equalsIgnoreCase("Unread")
                    && !n.isRead) count++;
        }
        return count;
    }

    /** All archived notifications. */
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

    /**
     * Stars a notification by ID.
     * Triggers notifyListeners() so ALL registered StoreListeners update —
     * including UserActivity (dashboard Starred card) and ArcActivity (star color).
     */
    public synchronized void star(String id) {
        for (NotificationItem n : items) {
            if (n.id.equals(id)) {
                n.isStarred = true;
                notifyListeners();
                return;
            }
        }
    }

    /**
     * Unstars a notification by ID.
     * Same as star() — triggers live update on dashboard and archive screen.
     */
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
