package com.example.notifly_system;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.RingtoneManager;
import android.media.SoundPool;
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

    // Keep track of IDs we've already seen so we only trigger
    // sound/vibration for genuinely new notifications
    private final Set<String> seenIds = new HashSet<>();

    // Context needed for sound/vibration — set from Application
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

        // Restore previously seen IDs from prefs so we don't
        // re-trigger sound/vibration after an app restart
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
                List<String> newIds = new ArrayList<>();

                for (DataSnapshot child : snapshot.getChildren()) {
                    String id     = child.getKey();
                    String title  = child.child("title").getValue(String.class);
                    String body   = child.child("body").getValue(String.class);
                    String target = child.child("target").getValue(String.class);
                    String topicOrUser = child.child("topicOrUser").getValue(String.class);
                    Long   ts     = child.child("timestamp").getValue(Long.class);

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

                    // Track which IDs are new (not seen before)
                    if (id != null && !seenIds.contains(id)) {
                        newIds.add(id);
                    }
                }

                // Push to store — triggers UI refresh on all screens
                NotificationStore.getInstance().syncFromFirebase(incoming);

                // Trigger sound/vibration only for new notifications
                if (!newIds.isEmpty() && appContext != null) {
                    triggerAlertForNewNotifications(newIds);
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

    private void triggerAlertForNewNotifications(List<String> newIds) {
        // Read user preferences
        SharedPreferences prefs = appContext.getSharedPreferences(
                PREFS_NAME, Context.MODE_PRIVATE);

        boolean masterOn    = prefs.getBoolean("master",    true);
        boolean soundOn     = prefs.getBoolean("sound",     true);
        boolean vibrationOn = prefs.getBoolean("vibration", false);

        // Mark all new IDs as seen BEFORE triggering so rapid
        // Firebase updates don't double-fire
        seenIds.addAll(newIds);
        persistSeenIds();

        if (!masterOn) return;

        if (soundOn)     triggerSound();
        if (vibrationOn) triggerVibration();
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

    private String mapTargetToCategory(String target, String topicOrUser) {
        if (target == null) return "Unread";
        switch (target.toLowerCase()) {
            case "all":    return "Announcements";
            case "topic":
                if (topicOrUser != null) {
                    switch (topicOrUser.toLowerCase()) {
                        case "announcements": return "Announcements";
                        case "events":        return "Events";
                    }
                }
                return "Announcements";
            case "single": return "Unread";
            default:       return "Unread";
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
