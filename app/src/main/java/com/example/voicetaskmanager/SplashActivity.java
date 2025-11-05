package com.example.voicetaskmanager;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.airbnb.lottie.LottieAnimationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class SplashActivity extends AppCompatActivity {

    private static final long SPLASH_DELAY = 2000; // 2 seconds
    private LottieAnimationView lottieSplash;
    private PrefsManager prefs;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        // Initialize PrefsManager
        prefs = new PrefsManager(this);

        // Apply saved theme before calling super.onCreate
        if (prefs.isDarkMode()) {
            setTheme(R.style.Theme_VoiceTaskManager); // Your dark theme
        } else {
            setTheme(R.style.Theme_VoiceTaskManager); // Your light theme
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        lottieSplash = findViewById(R.id.lottieSplash);
        mAuth = FirebaseAuth.getInstance();

        // Optional: start Lottie animation
        lottieSplash.playAnimation();

        // Navigate after delay
        new Handler(Looper.getMainLooper()).postDelayed(this::checkUserAndNavigate, SPLASH_DELAY);
    }

    private void checkUserAndNavigate() {
        FirebaseUser currentUser = mAuth.getCurrentUser();

        Intent intent;
        if (currentUser != null) {
            // User already logged in → MainActivity
            intent = new Intent(SplashActivity.this, MainActivity.class);
        } else {
            // Not logged in → LoginActivity
            intent = new Intent(SplashActivity.this, LoginActivity.class);
        }

        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
