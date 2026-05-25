package com.example.presensi_qr;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.example.presensi_qr.fragments.HistoryFragment;
import com.example.presensi_qr.fragments.HomeFragment;
import com.example.presensi_qr.fragments.ProfileFragment;
import com.example.presensi_qr.fragments.DispenFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        BottomNavigationView bottomNav = findViewById(R.id.bottom_nav);

        android.content.SharedPreferences pref = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        String userRole = pref.getString("ROLE", "murid");

        if ("guru".equalsIgnoreCase(userRole)) {
            // Guru has: Beranda, Dispen/Riwayat, Profil
            bottomNav.getMenu().removeItem(R.id.nav_history);
        } else if ("murid".equalsIgnoreCase(userRole)) {
            // Murid has: Beranda, Riwayat, Profil
            bottomNav.getMenu().removeItem(R.id.nav_dispen);
        } else {
            // Operator has: Beranda, Profil
            bottomNav.getMenu().removeItem(R.id.nav_dispen);
            bottomNav.getMenu().removeItem(R.id.nav_history);
        }

        bottomNav.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            int itemId = item.getItemId();

            if (itemId == R.id.nav_home) {
                selectedFragment = new HomeFragment();
            } else if (itemId == R.id.nav_history) {
                selectedFragment = new HistoryFragment();
            } else if (itemId == R.id.nav_dispen) {
                selectedFragment = new DispenFragment();
            } else if (itemId == R.id.nav_profile) {
                selectedFragment = new ProfileFragment();
            }

            if (selectedFragment != null) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.container, selectedFragment)
                        .commit();
            }
            return true;
        });

        // Set default fragment
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.container, new HomeFragment())
                    .commit();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        android.content.SharedPreferences pref = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        String userRole = pref.getString("ROLE", "murid");
        String token = pref.getString("TOKEN", "");
        if ("guru".equalsIgnoreCase(userRole) && !token.isEmpty()) {
            checkPendingDispensations(token);
        }
    }

    private void checkPendingDispensations(String token) {
        BottomNavigationView bottomNav = findViewById(R.id.bottom_nav);
        com.example.presensi_qr.api.ApiClient.getService().getSchedules(token).enqueue(new retrofit2.Callback<java.util.List<com.example.presensi_qr.models.TeachingSchedule>>() {
            @Override
            public void onResponse(retrofit2.Call<java.util.List<com.example.presensi_qr.models.TeachingSchedule>> call, retrofit2.Response<java.util.List<com.example.presensi_qr.models.TeachingSchedule>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    java.util.HashSet<String> teacherClasses = new java.util.HashSet<>();
                    for (com.example.presensi_qr.models.TeachingSchedule ts : response.body()) {
                        if (ts.getClassName() != null) teacherClasses.add(ts.getClassName().toLowerCase());
                    }

                    com.example.presensi_qr.api.ApiClient.getService().getPendingDispensations(token).enqueue(new retrofit2.Callback<java.util.List<com.example.presensi_qr.models.Dispensation>>() {
                        @Override
                        public void onResponse(retrofit2.Call<java.util.List<com.example.presensi_qr.models.Dispensation>> call, retrofit2.Response<java.util.List<com.example.presensi_qr.models.Dispensation>> response) {
                            if (response.isSuccessful() && response.body() != null) {
                                int pendingCount = 0;
                                for (com.example.presensi_qr.models.Dispensation d : response.body()) {
                                    if (d.getClassName() != null && teacherClasses.contains(d.getClassName().toLowerCase())) {
                                        pendingCount++;
                                    }
                                }
                                if (pendingCount > 0) {
                                    com.google.android.material.badge.BadgeDrawable badge = bottomNav.getOrCreateBadge(R.id.nav_dispen);
                                    badge.setVisible(true);
                                    badge.setNumber(pendingCount);
                                } else {
                                    bottomNav.removeBadge(R.id.nav_dispen);
                                }
                            }
                        }
                        @Override
                        public void onFailure(retrofit2.Call<java.util.List<com.example.presensi_qr.models.Dispensation>> call, Throwable t) {}
                    });
                }
            }
            @Override
            public void onFailure(retrofit2.Call<java.util.List<com.example.presensi_qr.models.TeachingSchedule>> call, Throwable t) {}
        });
    }
}
