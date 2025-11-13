package com.example.voicetaskmanager;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class SetupProfileActivity extends AppCompatActivity {

    private TextInputEditText etUsername, etDOB;
    private Spinner spinnerGender;
    private ImageView ivAvatar, ivAddPhoto;
    private Button btnSaveProfile;
    private ProgressBar progressBar;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private StorageReference storageRef;

    private Uri avatarUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup_profile);

        // Initialize views
        etUsername = findViewById(R.id.etUsername);
        etDOB = findViewById(R.id.etDOB);
        spinnerGender = findViewById(R.id.spinnerGender);
        ivAvatar = findViewById(R.id.ivAvatar);
        ivAddPhoto = findViewById(R.id.ivAddPhoto);
        btnSaveProfile = findViewById(R.id.btnSaveProfile);
        progressBar = findViewById(R.id.progressBar);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        storageRef = FirebaseStorage.getInstance().getReference();

        // Gender spinner setup
        ArrayAdapter<CharSequence> genderAdapter = ArrayAdapter.createFromResource(
                this,
                R.array.gender_array,
                android.R.layout.simple_spinner_item
        );
        genderAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerGender.setAdapter(genderAdapter);

        // Date picker setup
        etDOB.setOnClickListener(v -> showDatePicker());

        // Android 13+ Photo Picker
        ActivityResultLauncher<PickVisualMediaRequest> photoPicker =
                registerForActivityResult(
                        new ActivityResultContracts.PickVisualMedia(),
                        uri -> {
                            if (uri != null) {
                                avatarUri = uri;
                                Glide.with(this).load(uri).into(ivAvatar);
                            }
                        });

        ivAvatar.setOnClickListener(v -> photoPicker.launch(
                new PickVisualMediaRequest.Builder()
                        .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                        .build()));

        ivAddPhoto.setOnClickListener(v -> photoPicker.launch(
                new PickVisualMediaRequest.Builder()
                        .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                        .build()));

        btnSaveProfile.setOnClickListener(v -> saveProfile());
    }

    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR) - 18;
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog dpd = new DatePickerDialog(this,
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    String dob = String.format(Locale.getDefault(),
                            "%02d/%02d/%04d", selectedDay, selectedMonth + 1, selectedYear);
                    etDOB.setText(dob);
                }, year, month, day);
        dpd.show();
    }

    private void saveProfile() {
        String username = etUsername.getText().toString().trim();
        String dob = etDOB.getText().toString().trim();
        String gender = spinnerGender.getSelectedItem().toString();
        String uid = Objects.requireNonNull(mAuth.getCurrentUser()).getUid();

        if (username.isEmpty()) {
            etUsername.setError("Username required");
            return;
        }
        if (dob.isEmpty()) {
            etDOB.setError("Enter your date of birth");
            return;
        }

        btnSaveProfile.setEnabled(false);
        progressBar.setVisibility(View.VISIBLE);

        uploadAvatarAndSave(uid, username, dob, gender);
    }

    private void uploadAvatarAndSave(String uid, String username, String dob, String gender) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("username", username);
        updates.put("dob", dob);
        updates.put("gender", gender);

        if (avatarUri != null) {
            try {
                // Copy the picked image to a local cache file
                InputStream inputStream = getContentResolver().openInputStream(avatarUri);
                File tempFile = new File(getCacheDir(), "temp_avatar_" + uid + ".jpg");
                FileOutputStream outputStream = new FileOutputStream(tempFile);
                byte[] buffer = new byte[1024];
                int length;
                while ((length = inputStream.read(buffer)) > 0) {
                    outputStream.write(buffer, 0, length);
                }
                inputStream.close();
                outputStream.close();

                Uri localUri = Uri.fromFile(tempFile);
                StorageReference avatarRef = storageRef.child("avatars/" + uid + ".jpg");

                UploadTask uploadTask = avatarRef.putFile(localUri);
                uploadTask
                        .addOnSuccessListener(taskSnapshot ->
                                avatarRef.getDownloadUrl().addOnSuccessListener(uri -> {
                                    updates.put("photoUrl", uri.toString());
                                    saveToFirestore(uid, updates);
                                }))
                        .addOnFailureListener(e -> {
                            progressBar.setVisibility(View.GONE);
                            btnSaveProfile.setEnabled(true);
                            Toast.makeText(this, "Failed to upload image: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        });

            } catch (Exception e) {
                progressBar.setVisibility(View.GONE);
                btnSaveProfile.setEnabled(true);
                Toast.makeText(this, "File access error: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        } else {
            saveToFirestore(uid, updates);
        }
    }

    private void saveToFirestore(String uid, Map<String, Object> updates) {
        db.collection("users").document(uid)
                .set(updates, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Profile saved!", Toast.LENGTH_SHORT).show();
                    Intent i = new Intent(this, MainActivity.class);
                    i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(i);
                    finish();
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    btnSaveProfile.setEnabled(true);
                    Toast.makeText(this, "Failed to save: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }
}
