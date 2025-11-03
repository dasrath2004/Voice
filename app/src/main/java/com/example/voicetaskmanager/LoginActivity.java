package com.example.voicetaskmanager;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.airbnb.lottie.LottieAnimationView;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";

    EditText etUser, etPassword;
    Button btnLogin;
    MaterialButton btnSignup, btnPhoneLogin, btnGoogleLogin;
    LottieAnimationView loginAnimation;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private GoogleSignInClient googleClient;
    private ActivityResultLauncher<android.content.Intent> googleSignLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        etUser = findViewById(R.id.etEmail); // reuse your existing id
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnSignup = findViewById(R.id.btnSignup);
        btnPhoneLogin = findViewById(R.id.btnPhoneLogin); // add in layout
        btnGoogleLogin = findViewById(R.id.btnGoogleLogin); // add in layout
        loginAnimation = findViewById(R.id.loginAnimation);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Google sign in config
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        googleClient = GoogleSignIn.getClient(this, gso);

        googleSignLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    Intent data = result.getData();
                    Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
                    try {
                        GoogleSignInAccount account = task.getResult(ApiException.class);
                        if (account != null) firebaseAuthWithGoogle(account.getIdToken());
                    } catch (ApiException e) {
                        Log.w(TAG, "Google sign in failed", e);
                        Toast.makeText(this, "Google sign-in failed", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        btnLogin.setOnClickListener(v -> {
            String userInput = etUser.getText().toString().trim();
            String pass = etPassword.getText().toString().trim();
            if (TextUtils.isEmpty(userInput)) {
                etUser.setError("Enter email or phone");
                return;
            }
            if (TextUtils.isEmpty(pass)) {
                etPassword.setError("Enter password");
                return;
            }

            if (userInput.contains("@")) {
                // email login
                signInWithEmail(userInput, pass);
            } else {
                // phone login: find email in Firestore
                db.collection("users").whereEqualTo("phone", userInput).limit(1)
                        .get()
                        .addOnSuccessListener(q -> {
                            if (q.isEmpty()) {
                                Toast.makeText(LoginActivity.this, "No account with this phone", Toast.LENGTH_SHORT).show();
                                return;
                            }
                            String email = q.getDocuments().get(0).getString("email");
                            if (email == null || email.isEmpty()) {
                                Toast.makeText(LoginActivity.this, "No email linked to this phone", Toast.LENGTH_SHORT).show();
                                return;
                            }
                            signInWithEmail(email, pass);
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "phone lookup failed", e);
                            Toast.makeText(LoginActivity.this, "Error verifying phone", Toast.LENGTH_SHORT).show();
                        });
            }
        });

        btnSignup.setOnClickListener(v -> {
            // explicit sign out so Signup flow is clean
            mAuth.signOut();
            startActivity(new Intent(LoginActivity.this, SignupOptionsActivity.class));
        });

        btnPhoneLogin.setOnClickListener(v -> {
            // open Phone OTP activity in login mode
            Intent i = new Intent(LoginActivity.this, PhoneOTPActivity.class);
            i.putExtra("mode", "login");
            startActivity(i);
        });

        btnGoogleLogin.setOnClickListener(v -> {
            // Google sign-in from Login (acts as login)
            Intent signInIntent = googleClient.getSignInIntent();
            googleSignLauncher.launch(signInIntent);
        });
    }

    private void firebaseAuthWithGoogle(String idToken) {
        if (idToken == null) {
            Toast.makeText(this, "Google token null", Toast.LENGTH_SHORT).show();
            return;
        }
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Ensure Firestore profile exists (MainActivity will also ensure)
                        startActivity(new Intent(LoginActivity.this, MainActivity.class));
                        finish();
                    } else {
                        Toast.makeText(LoginActivity.this, "Google auth failed", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void signInWithEmail(String email, String password) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        startActivity(new Intent(LoginActivity.this, MainActivity.class));
                        finish();
                    } else {
                        Toast.makeText(LoginActivity.this, "Login failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (loginAnimation != null) loginAnimation.playAnimation();
    }

    @Override
    protected void onPause() {
        if (loginAnimation != null) loginAnimation.pauseAnimation();
        super.onPause();
    }
}
