package com.example.notifly_system; // TODO: Replace with your actual package name

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

    // Top bar
    private CardView btnBack;

    // Notification toggles
    private SwitchMaterial switchMaster;
    private SwitchMaterial switchSound;
    private SwitchMaterial switchVibration;

    // Notification type checkboxes
    private CheckBox checkAnnouncements;
    private CheckBox checkEvents;       // ✅ was checkPromotions
    private CheckBox checkAlerts;

    // Frequency chips
    private ChipGroup chipGroupFreq;
    private Chip chipLow;
    private Chip chipMed;
    private Chip chipHigh;

    // Save button
    private MaterialButton btnSave;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        initViews();
        setListeners();
        loadSavedPreferences();
    }

    private void initViews() {
        btnBack            = findViewById(R.id.btnBack);

        switchMaster       = findViewById(R.id.switchMaster);
        switchSound        = findViewById(R.id.switchSound);
        switchVibration    = findViewById(R.id.switchVibration);

        checkAnnouncements = findViewById(R.id.checkAnnouncements);
        checkEvents        = findViewById(R.id.checkEvents);     // ✅ was R.id.checkPromotions
        checkAlerts        = findViewById(R.id.checkAlerts);

        chipGroupFreq      = findViewById(R.id.chipGroupFreq);
        chipLow            = findViewById(R.id.chipLow);
        chipMed            = findViewById(R.id.chipMed);
        chipHigh           = findViewById(R.id.chipHigh);

        btnSave            = findViewById(R.id.btnSave);
    }

    private void setListeners() {

        btnBack.setOnClickListener(v -> finish());

        // Master switch — disables all sub-settings when turned off
        switchMaster.setOnCheckedChangeListener((buttonView, isChecked) -> {
            switchSound.setEnabled(isChecked);
            switchVibration.setEnabled(isChecked);
            checkAnnouncements.setEnabled(isChecked);
            checkEvents.setEnabled(isChecked);       // ✅ was checkPromotions
            checkAlerts.setEnabled(isChecked);
            chipGroupFreq.setEnabled(isChecked);
            chipLow.setEnabled(isChecked);
            chipMed.setEnabled(isChecked);
            chipHigh.setEnabled(isChecked);
        });

        // Row clicks toggle their checkbox
        findViewById(R.id.rowAnnouncements).setOnClickListener(v ->
                checkAnnouncements.setChecked(!checkAnnouncements.isChecked()));

        // ✅ was rowPromotions / checkPromotions
        findViewById(R.id.rowEvents).setOnClickListener(v ->
                checkEvents.setChecked(!checkEvents.isChecked()));

        findViewById(R.id.rowAlerts).setOnClickListener(v ->
                checkAlerts.setChecked(!checkAlerts.isChecked()));

        btnSave.setOnClickListener(v -> savePreferences());
    }

    private void loadSavedPreferences() {
        // TODO: Load from SharedPreferences or Firebase and apply to UI
        // Example:
        // SharedPreferences prefs = getSharedPreferences("notifly_prefs", MODE_PRIVATE);
        // switchMaster.setChecked(prefs.getBoolean("master", true));
        // switchSound.setChecked(prefs.getBoolean("sound", true));
        // switchVibration.setChecked(prefs.getBoolean("vibration", false));
        // checkAnnouncements.setChecked(prefs.getBoolean("announcements", true));
        // checkEvents.setChecked(prefs.getBoolean("events", true));
        // checkAlerts.setChecked(prefs.getBoolean("alerts", true));
        // String freq = prefs.getString("frequency", "med");
        // if (freq.equals("low")) chipLow.setChecked(true);
        // else if (freq.equals("high")) chipHigh.setChecked(true);
        // else chipMed.setChecked(true);
    }

    private void savePreferences() {
        boolean masterOn      = switchMaster.isChecked();
        boolean soundOn       = switchSound.isChecked();
        boolean vibrationOn   = switchVibration.isChecked();
        boolean announcements = checkAnnouncements.isChecked();
        boolean events        = checkEvents.isChecked();   // ✅ was promotions
        boolean alerts        = checkAlerts.isChecked();

        String frequency = "med";
        int checkedChipId = chipGroupFreq.getCheckedChipId();
        if (checkedChipId == R.id.chipLow)       frequency = "low";
        else if (checkedChipId == R.id.chipHigh) frequency = "high";

        // TODO: Save to SharedPreferences or Firebase
        // Example:
        // SharedPreferences.Editor editor = getSharedPreferences("notifly_prefs", MODE_PRIVATE).edit();
        // editor.putBoolean("master", masterOn);
        // editor.putBoolean("sound", soundOn);
        // editor.putBoolean("vibration", vibrationOn);
        // editor.putBoolean("announcements", announcements);
        // editor.putBoolean("events", events);     // ✅ was "promotions"
        // editor.putBoolean("alerts", alerts);
        // editor.putString("frequency", frequency);
        // editor.apply();

        Toast.makeText(this, "Preferences saved!", Toast.LENGTH_SHORT).show();
    }
}
