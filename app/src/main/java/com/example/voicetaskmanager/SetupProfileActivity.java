package com.example.voicetaskmanager;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.firestore.FieldValue;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class SetupProfileActivity extends AppCompatActivity {

    private static final String TAG = "SetupProfileAct";

    EditText etFullName, etUsername;
    ImageView ivAvatar;
    Button btnSave;
    FirebaseAuth mAuth;
    FirebaseFirestore db;
    FirebaseStorage storage;
    Uri selectedImageUri;

    ActivityResultLauncher<String> pickImageLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup_profile);

        etFullName = findViewById(R.id.etFullName);
        etUsername = findViewById(R.id.etUsername);
        ivAvatar = findViewById(R.id.ivAvatar);
        btnSave = findViewById(R.id.btnSaveProfile);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();

        pickImageLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri != null) {
                selectedImageUri = uri;
                try {
                    Bitmap bmp = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
                    ivAvatar.setImageBitmap(bmp);
                } catch (IOException e) { Log.e(TAG, "image load err", e); }
            }
        });

        ivAvatar.setOnClickListener(v -> pickImageLauncher.launch("image/*"));

        btnSave.setOnClickListener(v -> {
            String fullName = etFullName.getText().toString().trim();
            String username = etUsername.getText().toString().trim();

            if (fullName.isEmpty()) { etFullName.setError("Enter name"); return; }
            if (username.isEmpty()) { etUsername.setError("Enter username"); return; }

            String uid = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : null;
            if (uid == null) { Toast.makeText(this, "User not found", Toast.LENGTH_SHORT).show(); return; }

            if (selectedImageUri != null) {
                // upload image to Firebase Storage then save profile
                StorageReference ref = storage.getReference().child("profile_photos/" + uid + ".jpg");
                ref.putFile(selectedImageUri).addOnSuccessListener(taskSnapshot ->
                        ref.getDownloadUrl().addOnSuccessListener(uri -> {
                            saveProfileToFirestore(uid, fullName, username, uri.toString());
                        }).addOnFailureListener(e -> {
                            saveProfileToFirestore(uid, fullName, username, null);
                        })
                ).addOnFailureListener(e -> {
                    saveProfileToFirestore(uid, fullName, username, null);
                });
            } else {
                saveProfileToFirestore(uid, fullName, username, null);
            }
        });
    }

    private void saveProfileToFirestore(String uid, String name, String username, String photoUrl) {
        Map<String, Object> update = new HashMap<>();
        update.put("fullName", name);
        update.put("username", username);
        if (photoUrl != null) update.put("photoUrl", photoUrl);
        update.put("lastLogin", FieldValue.serverTimestamp());

        db.collection("users").document(uid).update(update)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(SetupProfileActivity.this, "Profile saved", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(SetupProfileActivity.this, MainActivity.class));
                    finish();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "profile save fail", e);
                    Toast.makeText(SetupProfileActivity.this, "Failed to save profile", Toast.LENGTH_SHORT).show();
                });
    }
}
