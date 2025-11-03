package com.example.voicetaskmanager;

import android.content.Intent;
import android.os.Bundle;
import android.telephony.PhoneNumberUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.TaskExecutors;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.concurrent.TimeUnit;
import java.util.HashMap;
import java.util.Map;

public class PhoneOTPActivity extends AppCompatActivity {

    private static final String TAG = "PhoneOTPActivity";

    EditText etPhone, etCode;
    Button btnSend, btnVerify;
    String storedVerificationId;
    PhoneAuthProvider.ForceResendingToken resendToken;
    FirebaseAuth mAuth;
    FirebaseFirestore db;
    String mode; // "signup" or "login"

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_phone_otp);

        etPhone = findViewById(R.id.etPhoneNumber);
        etCode = findViewById(R.id.etOtpCode);
        btnSend = findViewById(R.id.btnSendCode);
        btnVerify = findViewById(R.id.btnVerifyCode);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        mode = getIntent().getStringExtra("mode");
        if (mode == null) mode = "signup";

        btnSend.setOnClickListener(v -> {
            String phone = etPhone.getText().toString().trim();
            if (phone.isEmpty()) {
                etPhone.setError("Enter phone");
                return;
            }
            // optional: normalize phone to E.164 if you want
            startPhoneVerification(phone);
        });

        btnVerify.setOnClickListener(v -> {
            String code = etCode.getText().toString().trim();
            if (code.isEmpty()) {
                etCode.setError("Enter code");
                return;
            }
            verifyPhoneNumberWithCode(storedVerificationId, code);
        });
    }

    private void startPhoneVerification(String phoneNumber) {
        PhoneAuthOptions options =
                PhoneAuthOptions.newBuilder(mAuth)
                        .setPhoneNumber(phoneNumber)
                        .setTimeout(60L, TimeUnit.SECONDS)
                        .setActivity(this)
                        .setCallbacks(callbacks)
                        .build();
        PhoneAuthProvider.verifyPhoneNumber(options);
        Toast.makeText(this, "Sending code...", Toast.LENGTH_SHORT).show();
    }

    private final PhoneAuthProvider.OnVerificationStateChangedCallbacks callbacks =
            new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

                @Override
                public void onVerificationCompleted(@NonNull PhoneAuthCredential credential) {
                    // Auto-retrieval or instant verification
                    signInWithPhoneAuthCredential(credential);
                }

                @Override
                public void onVerificationFailed(@NonNull FirebaseException e) {
                    Log.e(TAG, "Verification failed", e);
                    Toast.makeText(PhoneOTPActivity.this, "Verification failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }

                @Override
                public void onCodeSent(@NonNull String verificationId,
                                       @NonNull PhoneAuthProvider.ForceResendingToken token) {
                    storedVerificationId = verificationId;
                    resendToken = token;
                    Toast.makeText(PhoneOTPActivity.this, "Code sent", Toast.LENGTH_SHORT).show();
                }
            };

    private void verifyPhoneNumberWithCode(String verificationId, String code) {
        PhoneAuthCredential credential = PhoneAuthProvider.getCredential(verificationId, code);
        signInWithPhoneAuthCredential(credential);
    }

    private void signInWithPhoneAuthCredential(PhoneAuthCredential credential) {
        mAuth.signInWithCredential(credential).addOnCompleteListener(this, task -> {
            if (task.isSuccessful()) {
                // Signed in (or account created) with phone
                FirebaseUser user = mAuth.getCurrentUser();
                if (user == null) {
                    Toast.makeText(this, "Auth succeeded but user is null", Toast.LENGTH_LONG).show();
                    return;
                }
                String uid = user.getUid();

                if ("signup".equals(mode)) {
                    // Save a minimal profile and go to SetupProfileActivity
                    Map<String, Object> profile = new HashMap<>();
                    profile.put("fullName", "");
                    profile.put("email", "");
                    profile.put("phone", user.getPhoneNumber() != null ? user.getPhoneNumber() : "");
                    profile.put("username", "");
                    profile.put("provider", "phone");
                    profile.put("photoUrl", "");
                    profile.put("createdAt", FieldValue.serverTimestamp());
                    profile.put("lastLogin", FieldValue.serverTimestamp());

                    db.collection("users").document(uid).set(profile)
                            .addOnSuccessListener(aVoid -> {
                                startActivity(new Intent(PhoneOTPActivity.this, SetupProfileActivity.class));
                                finish();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(PhoneOTPActivity.this, "Failed to save profile", Toast.LENGTH_SHORT).show();
                                startActivity(new Intent(PhoneOTPActivity.this, MainActivity.class));
                                finish();
                            });

                } else {
                    // login mode - simply go to main
                    startActivity(new Intent(PhoneOTPActivity.this, MainActivity.class));
                    finish();
                }

            } else {
                Toast.makeText(PhoneOTPActivity.this, "Verification failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
}
