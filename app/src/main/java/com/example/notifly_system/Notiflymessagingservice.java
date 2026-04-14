package com.example.notifly_system;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class NotiflyMessagingService extends FirebaseMessagingService {

    private static final String PREFS_NAME        = "notifly_prefs";
    public  static final String CHANNEL_ID        = "notifly_channel_sound";
    public  static final String CHANNEL_ID_SILENT = "notifly_channel_silent";

    // ─────────────────────────────────────────────
    // Called every time an FCM message arrives
    // ─────────────────────────────────────────────
    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        // 1. Master switch check — if off, drop the notification entirely
        if (!prefs.getBoolean("master", true)) return;

        // 2. Notification type filter
        //    Your FCM data payload should include: { "type": "announcements" | "events" | "alerts" }
        String type = remoteMessage.getData().get("type");
        if (type != null) {
            if (type.equals("announcements") && !prefs.getBoolean("announcements", true)) return;
            if (type.equals("events")        && !prefs.getBoolean("events",        true)) return;
            if (type.equals("alerts")        && !prefs.getBoolean("alerts",        true)) return;
        }

        // 3. Read sound & vibration prefs
        boolean soundOn     = prefs.getBoolean("sound",     true);
        boolean vibrationOn = prefs.getBoolean("vibration", false);

        // 4. Get title & body (supports both notification + data payloads)
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

        // 6. Trigger vibration independently (instant haptic feedback)
        if (vibrationOn) triggerVibration();
    }

    // ─────────────────────────────────────────────
    // Build channel + notification and post it
    // ─────────────────────────────────────────────
    private void showNotification(String title, String body,
                                  boolean soundOn, boolean vibrationOn) {

        NotificationManager manager =
                (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        String channelId = soundOn ? CHANNEL_ID : CHANNEL_ID_SILENT;

        // Create or update the notification channel (API 26+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel;

            if (soundOn) {
                channel = new NotificationChannel(
                        channelId,
                        "NotiFly (Sound)",
                        NotificationManager.IMPORTANCE_HIGH
                );
                Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                AudioAttributes audioAttr = new AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                        .build();
                channel.setSound(soundUri, audioAttr);
            } else {
                channel = new NotificationChannel(
                        channelId,
                        "NotiFly (Silent)",
                        NotificationManager.IMPORTANCE_DEFAULT
                );
                channel.setSound(null, null);
            }

            channel.setDescription("NotiFly notification channel");
            channel.enableVibration(vibrationOn);
            if (vibrationOn) {
                channel.setVibrationPattern(new long[]{0, 300, 150, 300});
            }

            manager.createNotificationChannel(channel);
        }

        // Tapping the notification opens MainActivity (change as needed)
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, intent,
                PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE
        );

        // Build the notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.drawable.ic_notif_bell)   // your existing bell icon
                .setContentTitle(title)
                .setContentText(body)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        if (soundOn) {
            Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            builder.setSound(soundUri);  // fallback for API < 26
        } else {
            builder.setSound(null);
        }

        if (vibrationOn) {
            builder.setVibrate(new long[]{0, 300, 150, 300});
        } else {
            builder.setVibrate(null);
        }

        manager.notify((int) System.currentTimeMillis(), builder.build());
    }

    // ─────────────────────────────────────────────
    // Direct vibration trigger (instant on receive)
    // ─────────────────────────────────────────────
    private void triggerVibration() {
        long[] pattern = {0, 300, 150, 300};

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // API 31+
            VibratorManager vm = (VibratorManager) getSystemService(VIBRATOR_MANAGER_SERVICE);
            if (vm != null) {
                vm.getDefaultVibrator().vibrate(
                        VibrationEffect.createWaveform(pattern, -1)
                );
            }
        } else {
            // API 24–30
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
    // Called when FCM assigns a new token
    // ─────────────────────────────────────────────
    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        // TODO: Send this token to your server/Firebase if needed
    }
}
