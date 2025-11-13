package com.example.voicetaskmanager;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.airbnb.lottie.LottieAnimationView;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class SignupOptionsActivity extends AppCompatActivity {

    private CardView googleTile, emailTile;
    private LottieAnimationView optionsAnimation;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private GoogleSignInClient googleClient;
    private ActivityResultLauncher<Intent> googleLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup_options);

        googleTile = findViewById(R.id.googleTile);
        emailTile = findViewById(R.id.emailTile);
        optionsAnimation = findViewById(R.id.optionsAnimation);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Google Sign-In setup
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        googleClient = GoogleSignIn.getClient(this, gso);

        googleLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    Intent data = result.getData();
                    if (data == null) return;
                    try {
                        GoogleSignInAccount account = GoogleSignIn.getSignedInAccountFromIntent(data).getResult();
                        if (account != null) firebaseAuthWithGoogle(account.getIdToken());
                    } catch (Exception e) {
                        Toast.makeText(this, "Google sign-in failed", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        // Google Click
        googleTile.setOnClickListener(v -> {
            mAuth.signOut();
            googleLauncher.launch(googleClient.getSignInIntent());
        });

        // Email Click
        emailTile.setOnClickListener(v -> {
            Intent i = new Intent(SignupOptionsActivity.this, EmailSignupActivity.class);
            startActivity(i);
        });

        findViewById(R.id.tvGoToLogin).setOnClickListener(v -> {
            startActivity(new Intent(SignupOptionsActivity.this, LoginActivity.class));
            finish();
        });
    }

    private void firebaseAuthWithGoogle(String idToken) {
        if (idToken == null) return;

        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential).addOnCompleteListener(this, task -> {
            if (task.isSuccessful()) {
                String uid = mAuth.getCurrentUser().getUid();
                String name = mAuth.getCurrentUser().getDisplayName();
                String email = mAuth.getCurrentUser().getEmail();
                String photo = mAuth.getCurrentUser().getPhotoUrl() != null
                        ? mAuth.getCurrentUser().getPhotoUrl().toString() : null;

                Map<String, Object> profile = new HashMap<>();
                profile.put("fullName", name != null ? name : "");
                profile.put("email", email != null ? email : "");
                profile.put("provider", "google");
                profile.put("photoUrl", photo);
                profile.put("createdAt", FieldValue.serverTimestamp());
                profile.put("lastLogin", FieldValue.serverTimestamp());

                db.collection("users").document(uid).set(profile)
                        .addOnSuccessListener(aVoid -> {
                            startActivity(new Intent(SignupOptionsActivity.this, SetupProfileActivity.class));
                            finish();
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(this, "Failed to save profile", Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(SignupOptionsActivity.this, MainActivity.class));
                            finish();
                        });
            } else {
                Toast.makeText(this, "Google authentication failed", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
