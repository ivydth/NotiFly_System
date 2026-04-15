package com.example.notifly_system;

import static android.content.Context.MODE_PRIVATE;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class NotiflyMessagingService extends FirebaseMessagingService {

    private static final String TAG               = "NotiflyFCM";
    private static final String PREFS_NAME        = "notifly_prefs";
    public  static final String CHANNEL_ID        = "notifly_channel_sound";
    public  static final String CHANNEL_ID_SILENT = "notifly_channel_silent";

    // ─────────────────────────────────────────────
    // Called once from NotiflyApplication.onCreate()
    // ─────────────────────────────────────────────
    public static void createChannels(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;

        NotificationManager manager =
                (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);

        // Sound channel
        NotificationChannel soundChannel = new NotificationChannel(
                CHANNEL_ID,
                "NotiFly (Sound)",
                NotificationManager.IMPORTANCE_HIGH
        );
        Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        AudioAttributes audioAttr = new AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .build();
        soundChannel.setSound(soundUri, audioAttr);
        soundChannel.setDescription("NotiFly notification channel with sound");
        soundChannel.enableVibration(true);
        soundChannel.setVibrationPattern(new long[]{0, 300, 150, 300});
        manager.createNotificationChannel(soundChannel);

        // Silent channel
        NotificationChannel silentChannel = new NotificationChannel(
                CHANNEL_ID_SILENT,
                "NotiFly (Silent)",
                NotificationManager.IMPORTANCE_DEFAULT
        );
        silentChannel.setSound(null, null);
        silentChannel.setDescription("NotiFly silent notification channel");
        silentChannel.enableVibration(false);
        manager.createNotificationChannel(silentChannel);
    }

    // ─────────────────────────────────────────────
    // NOTE: This service is only called when:
    //   (a) app is in the FOREGROUND, or
    //   (b) FCM payload is DATA-ONLY (no "notification" block)
    //
    // Your admin panel writes to Firebase Realtime DB only —
    // it does NOT send FCM pushes. So real-time alerts are
    // handled by FirebaseNotifSyncService, not here.
    //
    // This service handles any future FCM pushes you may add
    // (e.g. from Firebase Cloud Functions or a backend server).
    // ─────────────────────────────────────────────
    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        Log.d(TAG, "onMessageReceived fired");
        Log.d(TAG, "Data payload: "         + remoteMessage.getData());
        Log.d(TAG, "Notification payload: " + remoteMessage.getNotification());

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        // 1. Master switch
        if (!prefs.getBoolean("master", true)) {
            Log.d(TAG, "Master switch OFF — notification dropped");
            return;
        }

        // 2. Type filter — matches admin "target" / "topicOrUser" fields
        String type = remoteMessage.getData().get("type");
        if (type != null) {
            switch (type.toLowerCase()) {
                case "announcements":
                    if (!prefs.getBoolean("announcements", true)) return;
                    break;
                case "events":
                    if (!prefs.getBoolean("events", true)) return;
                    break;
                case "alerts":
                    if (!prefs.getBoolean("alerts", true)) return;
                    break;
            }
        }

        // 3. Sound & vibration prefs
        boolean soundOn     = prefs.getBoolean("sound",     true);
        boolean vibrationOn = prefs.getBoolean("vibration", false);

        Log.d(TAG, "soundOn=" + soundOn + " vibrationOn=" + vibrationOn);

        // 4. Title & body — prefer data payload over notification block
        String title = null;
        String body  = null;
        if (remoteMessage.getNotification() != null) {
            title = remoteMessage.getNotification().getTitle();
            body  = remoteMessage.getNotification().getBody();
        }
        if (title == null) title = remoteMessage.getData().get("title");
        if (body  == null) body  = remoteMessage.getData().get("body");
        if (title == null) title = "NotiFly";
        if (body  == null) body  = "You have a new notification.";

        // 5. Show the notification
        showNotification(title, body, soundOn, vibrationOn);

        // 6. Vibration (independent of channel)
        if (vibrationOn) triggerVibration();
    }

    // ─────────────────────────────────────────────
    // Build and post the system notification
    // ─────────────────────────────────────────────
    private void showNotification(String title, String body,
                                  boolean soundOn, boolean vibrationOn) {

        NotificationManager manager =
                (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        // Route to sound or silent channel based on pref
        String channelId = soundOn ? CHANNEL_ID : CHANNEL_ID_SILENT;

        Log.d(TAG, "Posting to channel: " + channelId);

        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, intent,
                PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.drawable.ic_notif_bell)
                .setContentTitle(title)
                .setContentText(body)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent);

        // Fallback for API < 26 (no channels)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            if (soundOn) {
                Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                builder.setSound(soundUri);
            } else {
                builder.setSound(null);
            }
            builder.setVibrate(vibrationOn ? new long[]{0, 300, 150, 300} : null);
        }

        manager.notify((int) System.currentTimeMillis(), builder.build());
    }

    // ─────────────────────────────────────────────
    // Direct vibration trigger
    // ─────────────────────────────────────────────
    private void triggerVibration() {
        long[] pattern = {0, 300, 150, 300};

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            VibratorManager vm = (VibratorManager) getSystemService(VIBRATOR_MANAGER_SERVICE);
            if (vm != null) {
                vm.getDefaultVibrator().vibrate(
                        VibrationEffect.createWaveform(pattern, -1));
            }
        } else {
            Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
            if (vibrator != null && vibrator.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1));
                } else {
                    vibrator.vibrate(pattern, -1);
                }
            }
        }
    }

    // ─────────────────────────────────────────────
    // Called when FCM assigns a new registration token
    // ─────────────────────────────────────────────
    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        Log.d(TAG, "New FCM token: " + token);
        // TODO: Send token to your server or save to Firebase users/{uid}/fcmToken
    }
}
