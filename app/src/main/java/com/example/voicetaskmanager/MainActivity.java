package com.example.voicetaskmanager;

import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;

public class MainActivity extends AppCompatActivity {

    private DrawerLayout drawer;
    private NavigationView navView;
    private BottomNavigationView bottomNav;
    private MaterialToolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        NotificationHelper.createChannel(this);


        drawer = findViewById(R.id.drawerLayout);
        navView = findViewById(R.id.navView);
        bottomNav = findViewById(R.id.bottomNav);
        toolbar = findViewById(R.id.mainToolbar);

        setSupportActionBar(toolbar);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.drawer_open, R.string.drawer_close
        );
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        // Default fragment
        loadFragment(new HomeFragment());

        // Bottom navigation listener
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                loadFragment(new HomeFragment());
            } else if (id == R.id.nav_completed) {
                loadFragment(new CompletedTasksFragment());
            } else if (id == R.id.nav_failed) {
                loadFragment(new FailedTasksFragment());
            }
            return true;
        });

        // Drawer menu listener
        navView.setNavigationItemSelectedListener(item -> {
            drawer.closeDrawers();
            int id = item.getItemId();

            if (id == R.id.nav_profile) {
                startActivity(new Intent(this, ProfileActivity.class));
            } else if (id == R.id.nav_settings) {
                startActivity(new Intent(this, SettingsActivity.class));
            } else if (id == R.id.nav_logout) {
                FirebaseAuth.getInstance().signOut();
                startActivity(new Intent(this, LoginActivity.class));
                finish();
            }
            return true;
        });
    }

    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.container_frame, fragment)
                .commit();
    }

    public void openDrawer() {
        drawer.open();
    }

    @Override
    public void onBackPressed() {
        if (drawer.isOpen()) {
            drawer.close();
            return;
        }

        // If not HomeFragment, go back to Home
        Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.container_frame);
        if (!(currentFragment instanceof HomeFragment)) {
            loadFragment(new HomeFragment());
            bottomNav.setSelectedItemId(R.id.nav_home);
            return;
        }

        super.onBackPressed();
    }
}
