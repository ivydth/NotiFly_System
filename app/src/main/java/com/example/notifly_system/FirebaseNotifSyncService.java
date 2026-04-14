package com.example.notifly_system;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.RingtoneManager;
import android.net.Uri;
import android.media.Ringtone;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class FirebaseNotifSyncService {

    private static final String PREFS_NAME     = "notifly_prefs";
    private static final String PREFS_SEEN_IDS = "seen_notif_ids";

    private static FirebaseNotifSyncService instance;
    private DatabaseReference notificationsRef;
    private ValueEventListener listener;
    private boolean isListening = false;

    private final Set<String> seenIds = new HashSet<>();
    private Context appContext;

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
     * Must be called from NotiflyApplication.onCreate() before startListening().
     */
    public void init(Context context) {
        this.appContext = context.getApplicationContext();

        SharedPreferences prefs = appContext.getSharedPreferences(
                PREFS_SEEN_IDS, Context.MODE_PRIVATE);
        Set<String> saved = prefs.getStringSet("ids", new HashSet<>());
        seenIds.addAll(saved);
    }

    /**
     * Start listening to /notifications in Firebase.
     * Safe to call multiple times — only attaches once.
     */
    public void startListening() {
        if (isListening) return;
        isListening = true;

        listener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                List<NotificationItem> incoming = new ArrayList<>();
                List<NotificationItem> newItems = new ArrayList<>();

                for (DataSnapshot child : snapshot.getChildren()) {
                    String id          = child.getKey();
                    String title       = child.child("title").getValue(String.class);
                    String body        = child.child("body").getValue(String.class);
                    String target      = child.child("target").getValue(String.class);
                    String topicOrUser = child.child("topicOrUser").getValue(String.class);
                    Long   ts          = child.child("timestamp").getValue(Long.class);

                    if (title == null) title = "Notification";
                    if (body  == null) body  = "";

                    String category  = mapTargetToCategory(target, topicOrUser);
                    String dateLabel = formatTimestamp(ts);

                    NotificationItem item = new NotificationItem(
                            id, title, body, dateLabel, category, false,
                            R.drawable.avatar_teal
                    );
                    if (ts != null) item.timestamp = ts;
                    incoming.add(item);

                    // Track genuinely new notifications
                    if (id != null && !seenIds.contains(id)) {
                        newItems.add(item);
                    }
                }

                // Always sync full list to store for UI display
                NotificationStore.getInstance().syncFromFirebase(incoming);

                // Trigger alert only for new notifications, filtered by prefs
                if (!newItems.isEmpty() && appContext != null) {
                    triggerAlertForNewNotifications(newItems);
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                // Silent fail — store keeps whatever it had
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

    // ── Alert trigger ─────────────────────────────────────────────────────────

    private void triggerAlertForNewNotifications(List<NotificationItem> newItems) {
        SharedPreferences prefs = appContext.getSharedPreferences(
                PREFS_NAME, Context.MODE_PRIVATE);

        boolean masterOn        = prefs.getBoolean("master",        true);
        boolean soundOn         = prefs.getBoolean("sound",         true);
        boolean vibrationOn     = prefs.getBoolean("vibration",     false);
        boolean allowAnnounce   = prefs.getBoolean("announcements", true);
        boolean allowEvents     = prefs.getBoolean("events",        true);
        boolean allowAlerts     = prefs.getBoolean("alerts",        true);

        // Mark all new IDs as seen BEFORE triggering so rapid
        // Firebase updates don't double-fire
        for (NotificationItem item : newItems) {
            if (item.id != null) seenIds.add(item.id);
        }
        persistSeenIds();

        // Master switch — if OFF, stop here for sound/vibration
        // (UI still shows notifications via syncFromFirebase above)
        if (!masterOn) return;

        // Check if at least one new notification passes the type filter
        boolean shouldAlert = false;
        for (NotificationItem item : newItems) {
            if (passesTypeFilter(item.category, allowAnnounce, allowEvents, allowAlerts)) {
                shouldAlert = true;
                break;
            }
        }

        if (!shouldAlert) return;

        if (soundOn)     triggerSound();
        if (vibrationOn) triggerVibration();
    }

    /**
     * Returns true if the notification category is allowed by the user's settings.
     * category comes from mapTargetToCategory() — values are:
     *   "Announcements", "Events", "Alerts", "Unread"
     */
    private boolean passesTypeFilter(String category,
                                     boolean allowAnnounce,
                                     boolean allowEvents,
                                     boolean allowAlerts) {
        if (category == null) return true;
        switch (category.toLowerCase()) {
            case "announcements": return allowAnnounce;
            case "events":        return allowEvents;
            case "alerts":        return allowAlerts;
            default:              return true; // "Unread" / unknown — allow through
        }
    }

    // ── Sound ─────────────────────────────────────────────────────────────────

    private void triggerSound() {
        try {
            Uri soundUri = RingtoneManager.getDefaultUri(
                    RingtoneManager.TYPE_NOTIFICATION);
            Ringtone ringtone = RingtoneManager.getRingtone(appContext, soundUri);
            if (ringtone != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    ringtone.setLooping(false);
                    ringtone.setVolume(1.0f);
                }
                ringtone.play();
            }
        } catch (Exception e) {
            // Fail silently — sound is not critical
        }
    }

    // ── Vibration ─────────────────────────────────────────────────────────────

    private void triggerVibration() {
        long[] pattern = {0, 300, 150, 300};

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                VibratorManager vm = (VibratorManager)
                        appContext.getSystemService(Context.VIBRATOR_MANAGER_SERVICE);
                if (vm != null) {
                    vm.getDefaultVibrator().vibrate(
                            VibrationEffect.createWaveform(pattern, -1));
                }
            } else {
                Vibrator vibrator = (Vibrator)
                        appContext.getSystemService(Context.VIBRATOR_SERVICE);
                if (vibrator != null && vibrator.hasVibrator()) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1));
                    } else {
                        vibrator.vibrate(pattern, -1);
                    }
                }
            }
        } catch (Exception e) {
            // Fail silently
        }
    }

    // ── Persist seen IDs ──────────────────────────────────────────────────────

    private void persistSeenIds() {
        if (appContext == null) return;
        appContext.getSharedPreferences(PREFS_SEEN_IDS, Context.MODE_PRIVATE)
                .edit()
                .putStringSet("ids", new HashSet<>(seenIds))
                .apply();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Maps the admin panel's target/topicOrUser fields to a category string
     * that matches the user's settings checkboxes.
     *
     * Admin sends:
     *   target="all"    → treat as Announcements
     *   target="topic"  + topicOrUser="announcements" → Announcements
     *   target="topic"  + topicOrUser="events"        → Events
     *   target="single"                               → Alerts (direct/personal)
     */
    private String mapTargetToCategory(String target, String topicOrUser) {
        if (target == null) return "Unread";
        switch (target.toLowerCase()) {
            case "all":
                return "Announcements";
            case "topic":
                if (topicOrUser != null) {
                    switch (topicOrUser.toLowerCase()) {
                        case "announcements": return "Announcements";
                        case "events":        return "Events";
                        case "alerts":        return "Alerts";
                    }
                }
                return "Announcements";
            case "single":
                // Single-user targeted messages map to Alerts
                return "Alerts";
            default:
                return "Unread";
        }
    }

    private String formatTimestamp(Long ts) {
        if (ts == null || ts == 0) return "Now";
        try {
            return new SimpleDateFormat("MMM d", Locale.getDefault())
                    .format(new Date(ts));
        } catch (Exception e) {
            return "Now";
        }
    }
}
