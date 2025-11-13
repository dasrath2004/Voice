package com.example.voicetaskmanager;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;

public class ForgotPasswordActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private TextInputEditText etEmail;
    private MaterialButton btnSendReset;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_forgot_password);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        mAuth = FirebaseAuth.getInstance();
        etEmail = findViewById(R.id.etEmailReset);
        btnSendReset = findViewById(R.id.btnSendReset);

        // Autofill from intent
        String prefilledEmail = getIntent().getStringExtra("email");
        if (prefilledEmail != null) {
            etEmail.setText(prefilledEmail);
        }

        btnSendReset.setOnClickListener(v -> sendResetEmail());
    }

    private void sendResetEmail() {
        String email = etEmail.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            etEmail.setError("Enter your email");
            return;
        }

        mAuth.fetchSignInMethodsForEmail(email).addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null &&
                    task.getResult().getSignInMethods() != null &&
                    !task.getResult().getSignInMethods().isEmpty()) {

                mAuth.sendPasswordResetEmail(email).addOnCompleteListener(resetTask -> {
                    if (resetTask.isSuccessful()) {
                        Toast.makeText(this, "Reset link sent to your email!", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(this, "Failed to send reset link", Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                Toast.makeText(this, "No account found with this email.", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
