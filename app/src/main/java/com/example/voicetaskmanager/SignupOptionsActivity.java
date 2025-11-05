package com.example.voicetaskmanager;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
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
import com.google.android.material.button.MaterialButton;
import androidx.cardview.widget.CardView;
import java.util.HashMap;
import java.util.Map;

public class SignupOptionsActivity extends AppCompatActivity {

    private static final String TAG = "SignupOptionsAct";
    private MaterialButton btnGoogle;
    private CardView phoneTile;
    private TextView tvGoToLogin;
    private LottieAnimationView optionsAnimation;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private GoogleSignInClient googleClient;
    private ActivityResultLauncher<Intent> googleLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup_options);

        btnGoogle = findViewById(R.id.btnGoogle);
        phoneTile = findViewById(R.id.phoneTile);
        tvGoToLogin = findViewById(R.id.tvGoToLogin);
        optionsAnimation = findViewById(R.id.optionsAnimation);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        googleClient = GoogleSignIn.getClient(this, gso);

        googleLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            Intent data = result.getData();
            try {
                GoogleSignInAccount account = GoogleSignIn.getSignedInAccountFromIntent(data).getResult();
                if (account != null) {
                    firebaseAuthWithGoogle(account.getIdToken());
                }
            } catch (Exception e) {
                Log.w(TAG, "Google sign failed", e);
                Toast.makeText(SignupOptionsActivity.this, "Google sign-in failed", Toast.LENGTH_SHORT).show();
            }
        });

        btnGoogle.setOnClickListener(v -> {
            mAuth.signOut();
            Intent intent = googleClient.getSignInIntent();
            googleLauncher.launch(intent);
        });

        phoneTile.setOnClickListener(v -> {
            mAuth.signOut();
            Intent i = new Intent(SignupOptionsActivity.this, PhoneSignupActivity.class);
            i.putExtra("mode", "signup");
            startActivity(i);
        });

        tvGoToLogin.setOnClickListener(v -> {
            startActivity(new Intent(SignupOptionsActivity.this, LoginActivity.class));
            finish();
        });
    }

    private void firebaseAuthWithGoogle(String idToken) {
        if (idToken == null) {
            Toast.makeText(this, "Google token is null", Toast.LENGTH_SHORT).show();
            return;
        }
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential).addOnCompleteListener(this, task -> {
            if (task.isSuccessful()) {
                String uid = mAuth.getCurrentUser().getUid();
                String name = mAuth.getCurrentUser().getDisplayName();
                String email = mAuth.getCurrentUser().getEmail();
                String photo = mAuth.getCurrentUser().getPhotoUrl() != null ? mAuth.getCurrentUser().getPhotoUrl().toString() : null;

                Map<String, Object> profile = new HashMap<>();
                profile.put("fullName", name != null ? name : "");
                profile.put("email", email != null ? email : "");
                profile.put("phone", "");
                profile.put("username", "");
                profile.put("provider", "google");
                profile.put("photoUrl", photo);
                profile.put("createdAt", FieldValue.serverTimestamp());
                profile.put("lastLogin", FieldValue.serverTimestamp());

                db.collection("users").document(uid).set(profile).addOnSuccessListener(aVoid -> {
                    startActivity(new Intent(SignupOptionsActivity.this, SetupProfileActivity.class));
                    finish();
                }).addOnFailureListener(e -> {
                    Toast.makeText(SignupOptionsActivity.this, "Failed to save profile", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(SignupOptionsActivity.this, MainActivity.class));
                    finish();
                });

            } else {
                Toast.makeText(SignupOptionsActivity.this, "Google authentication failed", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
