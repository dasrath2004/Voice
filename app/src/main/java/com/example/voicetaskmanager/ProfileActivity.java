package com.example.voicetaskmanager;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class ProfileActivity extends AppCompatActivity {

    MaterialToolbar topBar;
    ImageView profileImage;
    TextView usernameTv, emailTv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        topBar = findViewById(R.id.topBar);
        profileImage = findViewById(R.id.profileImage);
        usernameTv = findViewById(R.id.usernameTv);
        emailTv = findViewById(R.id.emailTv);

        topBar.setNavigationOnClickListener(v -> finish());

        loadUserData();
    }

    private void loadUserData() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        FirebaseFirestore.getInstance()
                .collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        showData(document);
                    }
                });
    }

    private void showData(@NonNull DocumentSnapshot doc) {
        String username = doc.getString("username");
        String email = doc.getString("email");
        String imgUrl = doc.getString("profileImage");

        usernameTv.setText(username);
        emailTv.setText(email);

        if (imgUrl != null && !imgUrl.isEmpty()) {
            Glide.with(this).load(imgUrl).into(profileImage);
        }
    }
}
