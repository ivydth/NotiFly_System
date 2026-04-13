package com.example.notifly_system;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class FirebaseNotifSyncService {

    private static FirebaseNotifSyncService instance;
    private DatabaseReference notificationsRef;
    private ValueEventListener listener;
    private boolean isListening = false;

    private FirebaseNotifSyncService() {
        notificationsRef = FirebaseDatabase.getInstance(
                "https://notifly-94dba-default-rtdb.asia-southeast1.firebasedatabase.app/"
        ).getReference("notifications");
    }

    public static synchronized FirebaseNotifSyncService getInstance() {
        if (instance == null) instance = new FirebaseNotifSyncService();
        return instance;
    }

    /**
     * Start listening to /notifications in Firebase.
     * Call this once from Application or MainActivity.
     * Safe to call multiple times — only attaches once.
     */
    public void startListening() {
        if (isListening) return;
        isListening = true;

        listener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                List<NotificationItem> incoming = new ArrayList<>();

                for (DataSnapshot child : snapshot.getChildren()) {
                    String id     = child.getKey();
                    String title  = child.child("title").getValue(String.class);
                    String body   = child.child("body").getValue(String.class);
                    String target = child.child("target").getValue(String.class);
                    Long   ts     = child.child("timestamp").getValue(Long.class);

                    if (title == null) title = "Notification";
                    if (body  == null) body  = "";

                    // Map Firebase "target" to a display category
                    String category = mapTargetToCategory(target);

                    // Format timestamp into a readable date label
                    String dateLabel = formatTimestamp(ts);

                    // Constructor: id, senderName, message, dateLabel,
                    //              category, isStarred, avatarResId
                    NotificationItem item = new NotificationItem(
                            id,
                            title,       // senderName = title from admin
                            body,        // message    = body from admin
                            dateLabel,
                            category,
                            false,       // isStarred  = false by default
                            R.drawable.avatar_teal
                    );
                    if (ts != null) item.timestamp = ts;

                    incoming.add(item);
                }

                // Push to store — triggers StoreListeners on all screens
                // and replaces sample data if real notifications exist
                NotificationStore.getInstance().syncFromFirebase(incoming);
            }

            @Override
            public void onCancelled(DatabaseError error) {
                // Silent fail — store keeps whatever it had (samples or real)
            }
        };

        notificationsRef.addValueEventListener(listener);
    }

    /** Stop listening — call in Application.onTerminate if needed. */
    public void stopListening() {
        if (listener != null) {
            notificationsRef.removeEventListener(listener);
            isListening = false;
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Maps the admin's "target" field to a user-facing category.
     * all    → "Announcements"
     * topic  → "Events"
     * single → "Unread"
     */
    private String mapTargetToCategory(String target) {
        if (target == null) return "Unread";
        switch (target.toLowerCase()) {
            case "all":    return "Announcements";
            case "topic":  return "Events";
            case "single": return "Unread";
            default:       return "Unread";
        }
    }

    private String formatTimestamp(Long ts) {
        if (ts == null || ts == 0) return "Now";
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM d", Locale.getDefault());
            return sdf.format(new Date(ts));
        } catch (Exception e) {
            return "Now";
        }
    }
}
