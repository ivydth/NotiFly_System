package com.example.notifly_system;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.SharedPreferences;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.widget.CheckBox;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.switchmaterial.SwitchMaterial;

public class SettActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "notifly_prefs";

    private CardView          btnBack;
    private SwitchMaterial    switchMaster;
    private SwitchMaterial    switchSound;
    private SwitchMaterial    switchVibration;
    private CheckBox          checkAnnouncements;
    private CheckBox          checkEvents;
    private CheckBox          checkAlerts;
    private ChipGroup         chipGroupFreq;
    private Chip              chipLow;
    private Chip              chipMed;
    private Chip              chipHigh;
    private MaterialButton    btnSave;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        initViews();
        loadSavedPreferences();
        setListeners();
    }

    private void initViews() {
        btnBack            = findViewById(R.id.btnBack);
        switchMaster       = findViewById(R.id.switchMaster);
        switchSound        = findViewById(R.id.switchSound);
        switchVibration    = findViewById(R.id.switchVibration);
        checkAnnouncements = findViewById(R.id.checkAnnouncements);
        checkEvents        = findViewById(R.id.checkEvents);
        checkAlerts        = findViewById(R.id.checkAlerts);
        chipGroupFreq      = findViewById(R.id.chipGroupFreq);
        chipLow            = findViewById(R.id.chipLow);
        chipMed            = findViewById(R.id.chipMed);
        chipHigh           = findViewById(R.id.chipHigh);
        btnSave            = findViewById(R.id.btnSave);
    }

    private void setListeners() {
        btnBack.setOnClickListener(v -> finish());

        // Master switch controls all child settings
        switchMaster.setOnCheckedChangeListener((buttonView, isChecked) ->
                setChildrenEnabled(isChecked));

        // Row taps toggle their checkboxes
        findViewById(R.id.rowAnnouncements).setOnClickListener(v ->
                checkAnnouncements.setChecked(!checkAnnouncements.isChecked()));
        findViewById(R.id.rowEvents).setOnClickListener(v ->
                checkEvents.setChecked(!checkEvents.isChecked()));
        findViewById(R.id.rowAlerts).setOnClickListener(v ->
                checkAlerts.setChecked(!checkAlerts.isChecked()));

        btnSave.setOnClickListener(v -> savePreferences());
    }

    private void loadSavedPreferences() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        boolean master = prefs.getBoolean("master", true);
        switchMaster.setChecked(master);
        switchSound.setChecked(prefs.getBoolean("sound",         true));
        switchVibration.setChecked(prefs.getBoolean("vibration", false));
        checkAnnouncements.setChecked(prefs.getBoolean("announcements", true));
        checkEvents.setChecked(prefs.getBoolean("events",        true));
        checkAlerts.setChecked(prefs.getBoolean("alerts",        true));

        String freq = prefs.getString("frequency", "med");
        if (freq.equals("low"))       chipLow.setChecked(true);
        else if (freq.equals("high")) chipHigh.setChecked(true);
        else                          chipMed.setChecked(true);

        setChildrenEnabled(master);
    }

    private void savePreferences() {
        boolean masterOn      = switchMaster.isChecked();
        boolean soundOn       = switchSound.isChecked();
        boolean vibrationOn   = switchVibration.isChecked();
        boolean announcements = checkAnnouncements.isChecked();
        boolean events        = checkEvents.isChecked();
        boolean alerts        = checkAlerts.isChecked();

        String frequency = "med";
        int checkedChipId = chipGroupFreq.getCheckedChipId();
        if      (checkedChipId == R.id.chipLow)  frequency = "low";
        else if (checkedChipId == R.id.chipHigh) frequency = "high";

        // Persist all prefs — FirebaseNotifSyncService reads these live on every
        // new notification, so no restart needed for changes to take effect.
        SharedPreferences.Editor editor =
                getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit();
        editor.putBoolean("master",        masterOn);
        editor.putBoolean("sound",         soundOn);
        editor.putBoolean("vibration",     vibrationOn);
        editor.putBoolean("announcements", announcements);
        editor.putBoolean("events",        events);
        editor.putBoolean("alerts",        alerts);
        editor.putString("frequency",      frequency);
        editor.apply();

        // Recreate notification channels so system respects updated sound/vibration
        recreateNotificationChannels(soundOn, vibrationOn);

        Toast.makeText(this, "Preferences saved!", Toast.LENGTH_SHORT).show();
    }

    // ── Enable / disable all child controls based on master switch ────────────

    private void setChildrenEnabled(boolean enabled) {
        switchSound.setEnabled(enabled);
        switchVibration.setEnabled(enabled);
        checkAnnouncements.setEnabled(enabled);
        checkEvents.setEnabled(enabled);
        checkAlerts.setEnabled(enabled);
        chipGroupFreq.setEnabled(enabled);
        chipLow.setEnabled(enabled);
        chipMed.setEnabled(enabled);
        chipHigh.setEnabled(enabled);
    }

    // ── Recreate channels so Android picks up updated sound/vibration ─────────
    //
    // Android caches channel settings after first creation.
    // Deleting and re-creating with a fresh config is the only reliable
    // way to change sound/vibration on API 26+.

    private void recreateNotificationChannels(boolean soundOn, boolean vibrationOn) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;

        NotificationManager manager =
                (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        // Delete old channels
        manager.deleteNotificationChannel(NotiflyMessagingService.CHANNEL_ID);
        manager.deleteNotificationChannel(NotiflyMessagingService.CHANNEL_ID_SILENT);

        // Recreate SOUND channel
        NotificationChannel soundChannel = new NotificationChannel(
                NotiflyMessagingService.CHANNEL_ID,
                "NotiFly (Sound)",
                NotificationManager.IMPORTANCE_HIGH
        );
        Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        AudioAttributes audioAttr = new AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .build();
        soundChannel.setSound(soundOn ? soundUri : null, soundOn ? audioAttr : null);
        soundChannel.setDescription("NotiFly notification channel with sound");
        soundChannel.enableVibration(vibrationOn);
        if (vibrationOn) soundChannel.setVibrationPattern(new long[]{0, 300, 150, 300});
        manager.createNotificationChannel(soundChannel);

        // Recreate SILENT channel
        NotificationChannel silentChannel = new NotificationChannel(
                NotiflyMessagingService.CHANNEL_ID_SILENT,
                "NotiFly (Silent)",
                NotificationManager.IMPORTANCE_DEFAULT
        );
        silentChannel.setSound(null, null);
        silentChannel.setDescription("NotiFly silent notification channel");
        silentChannel.enableVibration(false);
        manager.createNotificationChannel(silentChannel);
    }
}
