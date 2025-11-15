package com.example.voicetaskmanager;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.switchmaterial.SwitchMaterial;

public class SettingsActivity extends AppCompatActivity {

    private PrefsManager prefs;
    private SwitchMaterial switchTheme;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        prefs = new PrefsManager(this);
        AppCompatDelegate.setDefaultNightMode(
                prefs.isDarkMode() ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        MaterialToolbar toolbar = findViewById(R.id.settingsToolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        switchTheme = findViewById(R.id.switchTheme);
        switchTheme.setChecked(prefs.isDarkMode());

        switchTheme.setOnCheckedChangeListener((btn, isChecked) -> {
            prefs.setDarkMode(isChecked);
            AppCompatDelegate.setDefaultNightMode(
                    isChecked ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
            );
            recreate();
        });
    }
}
