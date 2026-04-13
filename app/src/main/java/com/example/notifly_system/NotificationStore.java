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

    // Tracks how many Firebase notifications have been seen
    // so the bell badge shows only NEW ones
    private int     lastSeenCount = 0;
    private int     newCount      = 0;

    // Flag so sample data is only injected once
    private boolean samplesLoaded = false;

    private NotificationStore() {
        loadSampleData();
    }

    public static synchronized NotificationStore getInstance() {
        if (instance == null) instance = new NotificationStore();
        return instance;
    }

    // ── Sample / Placeholder Data ─────────────────────────────────────────────

    /**
     * Pre-populates the store with placeholder notifications so the
     * home screen is never blank while Firebase loads.
     * These are replaced/merged once syncFromFirebase() is called
     * with real data.
     *
     * Constructor order:
     * id, senderName, message, dateLabel, category, isStarred, avatarResId
     */
    private void loadSampleData() {
        if (samplesLoaded) return;
        samplesLoaded = true;

        items.add(new NotificationItem(
                "sample_1",
                "Admin",
                "Welcome to NotiFly! Your notifications will appear here.",
                "Today",
                "Announcements",
                false,
                R.drawable.avatar_teal
        ));

        items.add(new NotificationItem(
                "sample_2",
                "System",
                "School event: Foundation Day celebration on April 20.",
                "Yesterday",
                "Events",
                false,
                R.drawable.avatar_teal
        ));

        items.add(new NotificationItem(
                "sample_3",
                "Class Adviser",
                "Reminder: Submit your project requirements by Friday.",
                "Apr 11",
                "Announcements",
                false,
                R.drawable.avatar_teal
        ));

        items.add(new NotificationItem(
                "sample_4",
                "Registrar",
                "Enrollment for next semester is now open.",
                "Apr 10",
                "Announcements",
                true,           // starred so Starred summary card also shows data
                R.drawable.avatar_teal
        ));

        items.add(new NotificationItem(
                "sample_5",
                "Guidance Office",
                "Career talk scheduled for April 18 at the auditorium.",
                "Apr 9",
                "Events",
                false,
                R.drawable.avatar_teal
        ));
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
     * Called by FirebaseNotifSyncService whenever the /notifications node
     * changes. On first real Firebase sync, removes sample placeholders so
     * real data takes over cleanly. If Firebase returns an empty list,
     * samples are kept so the screen is never blank.
     */
    public synchronized void syncFromFirebase(List<NotificationItem> incoming) {
        // Only strip samples when real data actually arrives
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

        // New count = total Firebase items minus how many the user has seen
        int total = incoming.size();
        newCount  = Math.max(0, total - lastSeenCount);

        notifyListeners();
    }

    /**
     * Call this when the user opens the bell / notification screen
     * so the badge resets to 0.
     */
    public synchronized void markAllSeen() {
        lastSeenCount = getTotalFirebaseCount();
        newCount      = 0;
        notifyListeners();
    }

    private int getTotalFirebaseCount() {
        return items.size();
    }

    /** How many NEW notifications have arrived since the user last opened bell. */
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
