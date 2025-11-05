package com.example.voicetaskmanager;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;

public class LoginActivity extends AppCompatActivity {

    private TextInputEditText etIdentifier, etPassword;
    private MaterialButton btnLogin, btnSignupOptions;
    private TextView tvForgotPassword;
    private FirebaseAuth mAuth;

    private static final String PHONE_DOMAIN = "@vtm.local"; // fake email domain for phone accounts

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();

        etIdentifier = findViewById(R.id.etLoginIdentifier);
        etPassword = findViewById(R.id.etLoginPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnSignupOptions = findViewById(R.id.btnSignupOptions);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);

        btnLogin.setOnClickListener(v -> attemptLogin());
        btnSignupOptions.setOnClickListener(v -> startActivity(new Intent(LoginActivity.this, SignupOptionsActivity.class)));
        tvForgotPassword.setOnClickListener(v -> {
            String identifier = etIdentifier.getText().toString().trim();
            if (identifier.contains("@")) {
                // email reset
                if (TextUtils.isEmpty(identifier)) {
                    Toast.makeText(this, "Enter email to reset password", Toast.LENGTH_SHORT).show();
                    return;
                }
                mAuth.sendPasswordResetEmail(identifier).addOnCompleteListener(t -> {
                    if (t.isSuccessful()) Toast.makeText(this, "Password reset email sent", Toast.LENGTH_SHORT).show();
                    else Toast.makeText(this, "Failed: " + t.getException().getMessage(), Toast.LENGTH_LONG).show();
                });
            } else {
                // phone reset - direct user to PhoneSignupActivity to verify phone and set password
                Intent i = new Intent(LoginActivity.this, PhoneSignupActivity.class);
                i.putExtra("mode", "reset"); // mode = reset will verify phone then let user set password
                startActivity(i);
            }
        });
    }

    private void attemptLogin() {
        String id = etIdentifier.getText().toString().trim();
        String pass = etPassword.getText().toString().trim();
        if (TextUtils.isEmpty(id)) { etIdentifier.setError("Enter email or phone"); return; }
        if (TextUtils.isEmpty(pass)) { etPassword.setError("Enter password"); return; }

        String loginEmail = id.contains("@") ? id : (id + PHONE_DOMAIN);

        mAuth.signInWithEmailAndPassword(loginEmail, pass).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                startActivity(new Intent(LoginActivity.this, MainActivity.class));
                finish();
            } else {
                Toast.makeText(LoginActivity.this, "Login failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (mAuth.getCurrentUser() != null) {
            startActivity(new Intent(LoginActivity.this, MainActivity.class));
            finish();
        }
    }
}
