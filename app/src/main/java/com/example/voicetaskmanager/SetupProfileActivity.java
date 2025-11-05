package com.example.voicetaskmanager;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import java.util.HashMap;
import java.util.Map;

public class SetupProfileActivity extends AppCompatActivity {

    EditText etUsername;
    Button btnSaveProfile;
    FirebaseFirestore db;
    FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup_profile);

        etUsername = findViewById(R.id.etUsername);
        btnSaveProfile = findViewById(R.id.btnSaveProfile);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        btnSaveProfile.setOnClickListener(v -> saveProfileAndNavigate());
    }

    private void saveProfileAndNavigate() {
        String username = etUsername.getText().toString().trim();
        if (username.isEmpty()) {
            etUsername.setError("Username required");
            return;
        }
        String uid = mAuth.getCurrentUser().getUid();

        // check uniqueness
        db.collection("users").whereEqualTo("username", username).limit(1).get().addOnCompleteListener(t -> {
            if (t.isSuccessful()) {
                QuerySnapshot snap = t.getResult();
                if (snap != null && !snap.isEmpty()) {
                    // taken - suggest alternatives
                    String suggestion = username + (int)(Math.random()*90 + 10);
                    Toast.makeText(this, "Username taken; try: " + suggestion, Toast.LENGTH_LONG).show();
                } else {
                    // available - update user doc
                    Map<String, Object> updates = new HashMap<>();
                    updates.put("username", username);
                    db.collection("users").document(uid).update(updates).addOnSuccessListener(aVoid -> {
                        Toast.makeText(SetupProfileActivity.this, "Profile Saved!", Toast.LENGTH_SHORT).show();
                        Intent i = new Intent(SetupProfileActivity.this, MainActivity.class);
                        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(i);
                        finish();
                    }).addOnFailureListener(e -> {
                        Toast.makeText(SetupProfileActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
                }
            } else {
                Toast.makeText(this, "Check failed: " + t.getException().getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
}
