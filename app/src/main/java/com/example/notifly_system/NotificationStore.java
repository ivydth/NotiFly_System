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
    private int lastSeenCount = 0;
    private int newCount      = 0;

    private NotificationStore() {}

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

    // ── Firebase sync ─────────────────────────────────────────────────────────

    /**
     * Called by FirebaseNotifSyncService whenever the /notifications node
     * changes. Merges incoming Firebase items into the store without
     * duplicating existing ones, then recalculates the new-notification count.
     */
    public synchronized void syncFromFirebase(List<NotificationItem> incoming) {
        for (NotificationItem incoming_item : incoming) {
            boolean exists = false;
            for (NotificationItem existing : items) {
                if (existing.id.equals(incoming_item.id)) {
                    exists = true;
                    break;
                }
            }
            if (!exists) {
                items.add(incoming_item);
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
