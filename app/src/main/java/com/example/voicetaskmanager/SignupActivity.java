package com.example.voicetaskmanager;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class SignupActivity extends AppCompatActivity {

    TextInputEditText etFullName, etEmail, etUsername, etPhone, etPassword, etConfirm;
    Button btnSignup;
    FirebaseAuth mAuth;
    FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        etFullName = findViewById(R.id.etFullName);
        etEmail = findViewById(R.id.etEmail);
        etUsername = findViewById(R.id.etUsername);
        etPhone = findViewById(R.id.etPhone);
        etPassword = findViewById(R.id.etPassword);
        etConfirm = findViewById(R.id.etConfirmPassword);
        btnSignup = findViewById(R.id.btnSignup);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        btnSignup.setOnClickListener(v -> {
            String name = etFullName.getText() == null ? "" : etFullName.getText().toString().trim();
            String email = etEmail.getText() == null ? "" : etEmail.getText().toString().trim();
            String username = etUsername.getText() == null ? "" : etUsername.getText().toString().trim();
            String phone = etPhone.getText() == null ? "" : etPhone.getText().toString().trim();
            String pass = etPassword.getText() == null ? "" : etPassword.getText().toString();
            String confirm = etConfirm.getText() == null ? "" : etConfirm.getText().toString();

            if (TextUtils.isEmpty(name)) { etFullName.setError("Enter full name"); return; }
            if (TextUtils.isEmpty(email)) { etEmail.setError("Enter email"); return; }
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) { etEmail.setError("Enter valid email"); return; }
            if (TextUtils.isEmpty(username)) { etUsername.setError("Enter username"); return; }
            if (TextUtils.isEmpty(phone)) { etPhone.setError("Enter phone"); return; }
            if (TextUtils.isEmpty(pass)) { etPassword.setError("Enter password"); return; }
            if (pass.length() < 6) { etPassword.setError("Password must be at least 6 characters"); return; }
            if (!pass.equals(confirm)) { etConfirm.setError("Passwords do not match"); return; }

            // create firebase auth user
            mAuth.createUserWithEmailAndPassword(email, pass).addOnCompleteListener(this, task -> {
                if (task.isSuccessful()) {
                    String uid = mAuth.getCurrentUser().getUid();
                    Map<String, Object> profile = new HashMap<>();
                    profile.put("fullName", name);
                    profile.put("email", email);
                    profile.put("phone", phone);
                    profile.put("username", username);
                    profile.put("provider", "email");
                    profile.put("photoUrl", "");
                    profile.put("createdAt", FieldValue.serverTimestamp());
                    profile.put("lastLogin", FieldValue.serverTimestamp());

                    db.collection("users").document(uid).set(profile).addOnSuccessListener(aVoid -> {
                        // go to setup profile to allow adding additional details
                        Intent i = new Intent(SignupActivity.this, SetupProfileActivity.class);
                        startActivity(i);
                        finish();
                    }).addOnFailureListener(e -> {
                        Toast.makeText(SignupActivity.this, "Failed to save profile: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        // still proceed to main to avoid blocking user (optional)
                        startActivity(new Intent(SignupActivity.this, MainActivity.class));
                        finish();
                    });
                } else {
                    Toast.makeText(SignupActivity.this, "Signup failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                }
            });
        });
    }
}
