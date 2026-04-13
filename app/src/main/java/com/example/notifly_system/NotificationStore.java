package com.example.notifly_system;

import java.util.ArrayList;
import java.util.Comparator;
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

    // ── Newest-first comparator ───────────────────────────────────────────────

    private static final Comparator<NotificationItem> NEWEST_FIRST =
            (a, b) -> Long.compare(b.timestamp, a.timestamp);

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
     * Called every time the Firebase realtime listener fires (onDataChange).
     *
     * KEY FIX: Instead of merging incrementally (which skips new items whose
     * IDs were never seen), we do a FULL REPLACE every time:
     *   1. Remove all sample placeholders.
     *   2. Remove all non-starred, non-archived real notifications —
     *      they will be fully replaced by the fresh Firebase snapshot.
     *   3. Keep starred/archived items so user actions are not lost.
     *   4. Add every item from the incoming snapshot. For items already
     *      preserved in step 3, carry over their star/archive/read flags.
     *   5. Re-sort newest-first and recalculate the bell badge.
     *
     * This guarantees that a notification sent from the admin panel appears
     * immediately the next time onDataChange fires — no manual refresh needed.
     */
    public synchronized void syncFromFirebase(List<NotificationItem> incoming) {

        // Step 1 — always strip samples
        items.removeIf(n -> n.id.startsWith("sample_"));

        // Step 2 — snapshot the current user-action flags before clearing
        // so we can carry them over to the freshly synced items
        List<NotificationItem> preserved = new ArrayList<>();
        for (NotificationItem n : items) {
            if (n.isStarred || n.isArchived || n.isRead) {
                preserved.add(n);
            }
        }

        // Step 3 — clear all current real items; we are doing a full replace
        items.clear();

        // Step 4 — add every item from Firebase
        for (NotificationItem incomingItem : incoming) {
            // Carry over user-set flags from preserved list
            for (NotificationItem p : preserved) {
                if (p.id.equals(incomingItem.id)) {
                    incomingItem.isStarred  = p.isStarred;
                    incomingItem.isArchived = p.isArchived;
                    incomingItem.isRead     = p.isRead;
                    break;
                }
            }
            items.add(incomingItem);
        }

        // Step 5 — sort newest-first
        items.sort(NEWEST_FIRST);

        // Step 6 — recalculate bell badge
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

    /**
     * Returns true if this notification has NOT been seen yet by the user.
     * Used to show the glowing teal dot on new notification rows.
     * Samples are always considered seen and will never show the dot.
     */
    public synchronized boolean isNew(String id) {
        if (id == null || id.startsWith("sample_")) return false;
        return !seenIds.contains(id);
    }

    // ── Queries ───────────────────────────────────────────────────────────────

    /** All non-archived notifications, newest first. */
    public synchronized List<NotificationItem> getAll() {
        List<NotificationItem> result = new ArrayList<>();
        for (NotificationItem n : items) {
            if (!n.isArchived) result.add(n);
        }
        result.sort(NEWEST_FIRST);
        return result;
    }

    /** Non-archived notifications matching a specific category, newest first. */
    public synchronized List<NotificationItem> getByCategory(String category) {
        List<NotificationItem> result = new ArrayList<>();
        for (NotificationItem n : items) {
            if (!n.isArchived && n.originalCategory.equalsIgnoreCase(category))
                result.add(n);
        }
        result.sort(NEWEST_FIRST);
        return result;
    }

    /**
     * ALL starred notifications — includes BOTH archived and non-archived,
     * newest first.
     */
    public synchronized List<NotificationItem> getStarred() {
        List<NotificationItem> result = new ArrayList<>();
        for (NotificationItem n : items) {
            if (n.isStarred) result.add(n);
        }
        result.sort(NEWEST_FIRST);
        return result;
    }

    /** Unread notifications — not archived, category Unread, not yet read, newest first. */
    public synchronized List<NotificationItem> getUnread() {
        List<NotificationItem> result = new ArrayList<>();
        for (NotificationItem n : items) {
            if (!n.isArchived
                    && n.originalCategory.equalsIgnoreCase("Unread")
                    && !n.isRead)
                result.add(n);
        }
        result.sort(NEWEST_FIRST);
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

    /** All archived notifications, newest first. */
    public synchronized List<NotificationItem> getArchived() {
        List<NotificationItem> result = new ArrayList<>();
        for (NotificationItem n : items) {
            if (n.isArchived) result.add(n);
        }
        result.sort(NEWEST_FIRST);
        return result;
    }

    // ── Mutations ─────────────────────────────────────────────────────────────

    public synchronized void add(NotificationItem item) {
        for (NotificationItem n : items) {
            if (n.id.equals(item.id)) return;
        }
        items.add(item);
        items.sort(NEWEST_FIRST);
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
