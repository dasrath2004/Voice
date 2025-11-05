package com.example.voicetaskmanager;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Spinner;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class PhoneSignupActivity extends AppCompatActivity {

    private Spinner spinnerCountry;
    private TextInputEditText etPhone, etOtp, etPassword, etConfirm;
    private MaterialButton btnSendOtp, btnVerifyOtp, btnCreateAccount;
    private ProgressBar progress;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private String verificationId;
    private PhoneAuthProvider.ForceResendingToken resendToken;
    private PhoneAuthCredential phoneCredential;

    private static final String PHONE_DOMAIN = "@vtm.local";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_phone_signup);

        spinnerCountry = findViewById(R.id.spinnerCountry);
        etPhone = findViewById(R.id.etPhone);
        etOtp = findViewById(R.id.etOtp);
        etPassword = findViewById(R.id.etPassword);
        etConfirm = findViewById(R.id.etConfirmPassword);

        btnSendOtp = findViewById(R.id.btnSendOtp);
        btnVerifyOtp = findViewById(R.id.btnVerifyOtp);
        btnCreateAccount = findViewById(R.id.btnCreateAccount);
        progress = findViewById(R.id.progress);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        showState(1);

        btnSendOtp.setOnClickListener(v -> sendVerificationCode());
        btnVerifyOtp.setOnClickListener(v -> verifyCode());
        btnCreateAccount.setOnClickListener(v -> createAccount());
    }

    private void showState(int state) {
        findViewById(R.id.layoutPhone).setVisibility(state == 1 ? View.VISIBLE : View.GONE);
        btnSendOtp.setVisibility(state == 1 ? View.VISIBLE : View.GONE);

        findViewById(R.id.layoutOtp).setVisibility(state == 2 ? View.VISIBLE : View.GONE);
        btnVerifyOtp.setVisibility(state == 2 ? View.VISIBLE : View.GONE);

        findViewById(R.id.layoutPassword).setVisibility(state == 3 ? View.VISIBLE : View.GONE);
        btnCreateAccount.setVisibility(state == 3 ? View.VISIBLE : View.GONE);
    }

    private void sendVerificationCode() {
        String phoneRaw = etPhone.getText().toString().trim();
        if (phoneRaw.length() < 10) {
            etPhone.setError("Enter valid phone");
            return;
        }
        String phoneWithCode = "+91" + phoneRaw;
        progress.setVisibility(View.VISIBLE);
        btnSendOtp.setVisibility(View.GONE);

        PhoneAuthOptions options = PhoneAuthOptions.newBuilder(mAuth)
                .setPhoneNumber(phoneWithCode)
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(this)
                .setCallbacks(mCallbacks)
                .build();

        PhoneAuthProvider.verifyPhoneNumber(options);
    }

    private final PhoneAuthProvider.OnVerificationStateChangedCallbacks mCallbacks =
            new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                @Override
                public void onVerificationCompleted(@NonNull PhoneAuthCredential credential) {
                    phoneCredential = credential;
                    progress.setVisibility(View.GONE);
                    showState(3);
                }

                @Override
                public void onVerificationFailed(@NonNull FirebaseException e) {
                    progress.setVisibility(View.GONE);
                    btnSendOtp.setVisibility(View.VISIBLE);
                    Toast.makeText(PhoneSignupActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();
                }

                @Override
                public void onCodeSent(@NonNull String verId,
                                       @NonNull PhoneAuthProvider.ForceResendingToken token) {
                    super.onCodeSent(verId, token);
                    verificationId = verId;
                    resendToken = token;
                    progress.setVisibility(View.GONE);
                    showState(2);
                    Toast.makeText(PhoneSignupActivity.this, "OTP Sent", Toast.LENGTH_SHORT).show();
                }
            };

    private void verifyCode() {
        String code = etOtp.getText().toString().trim();
        if (code.length() < 4) {
            etOtp.setError("Enter OTP");
            return;
        }
        progress.setVisibility(View.VISIBLE);
        phoneCredential = PhoneAuthProvider.getCredential(verificationId, code);
        progress.setVisibility(View.GONE);
        showState(3);
    }

    private void createAccount() {
        String pass = etPassword.getText().toString();
        String confirm = etConfirm.getText().toString();
        if (pass.length() < 6) {
            etPassword.setError("At least 6 chars");
            return;
        }
        if (!pass.equals(confirm)) {
            etConfirm.setError("Passwords do not match");
            return;
        }

        progress.setVisibility(View.VISIBLE);
        String phoneRaw = etPhone.getText().toString().trim();
        String fakeEmail = phoneRaw + PHONE_DOMAIN;

        mAuth.createUserWithEmailAndPassword(fakeEmail, pass)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        if (phoneCredential != null) {
                            mAuth.getCurrentUser().linkWithCredential(phoneCredential);
                        }
                        String uid = mAuth.getCurrentUser().getUid();
                        Map<String, Object> profile = new HashMap<>();
                        profile.put("uid", uid);
                        profile.put("email", fakeEmail);
                        profile.put("phone", "+91" + phoneRaw);
                        profile.put("provider", "phone");
                        profile.put("username", "");
                        profile.put("createdAt", FieldValue.serverTimestamp());
                        profile.put("lastLogin", FieldValue.serverTimestamp());

                        db.collection("users").document(uid).set(profile)
                                .addOnSuccessListener(aVoid -> {
                                    progress.setVisibility(View.GONE);
                                    startActivity(new Intent(this, SetupProfileActivity.class)
                                            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
                                    finish();
                                })
                                .addOnFailureListener(e -> {
                                    progress.setVisibility(View.GONE);
                                    Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                });
                    } else {
                        progress.setVisibility(View.GONE);
                        Toast.makeText(this, "Signup failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }
}
