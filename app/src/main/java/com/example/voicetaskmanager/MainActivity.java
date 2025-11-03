package com.example.voicetaskmanager;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private final String[] DEFAULT_CATEGORIES = {"Personal", "Work"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
    }

    @Override
    protected void onStart() {
        super.onStart();
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }
        String uid = user.getUid();
        String name = user.getDisplayName() != null ? user.getDisplayName() : "";
        String email = user.getEmail() != null ? user.getEmail() : "";
        String photo = user.getPhotoUrl() != null ? user.getPhotoUrl().toString() : null;

        db.collection("users").document(uid).get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                db.collection("users").document(uid).update("lastLogin", FieldValue.serverTimestamp());
            } else {
                Map<String, Object> profile = new HashMap<>();
                profile.put("fullName", name);
                profile.put("email", email);
                profile.put("phone", "");
                profile.put("username", "");
                profile.put("provider", "google");
                profile.put("photoUrl", photo);
                profile.put("createdAt", FieldValue.serverTimestamp());
                profile.put("lastLogin", FieldValue.serverTimestamp());
                db.collection("users").document(uid).set(profile).addOnSuccessListener(aVoid -> {
                    for (String category : DEFAULT_CATEGORIES) {
                        Map<String, Object> cat = new HashMap<>();
                        cat.put("name", category);
                        cat.put("createdAt", FieldValue.serverTimestamp());
                        db.collection("users").document(uid).collection("categories").document(category).set(cat);
                    }
                }).addOnFailureListener(e -> Log.e(TAG, "failed create user doc", e));
            }
        }).addOnFailureListener(e -> Log.e(TAG, "failed get user doc", e));
    }
}
