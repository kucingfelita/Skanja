package com.example.presensi_qr;

import android.os.Bundle;
import android.util.Log;

import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.presensi_qr.api.ApiClient;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import java.io.IOException;

public class RegisterActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        TextInputEditText etFullName = findViewById(R.id.et_reg_full_name);
        TextInputEditText etId = findViewById(R.id.et_reg_id);
        TextInputEditText etPassword = findViewById(R.id.et_reg_password);
        MaterialButton btnRegister = findViewById(R.id.btn_register);
        MaterialButton btnBack = findViewById(R.id.btn_back_to_login);

        com.google.android.material.textfield.TextInputLayout tilId = findViewById(R.id.til_reg_id);
        com.google.android.material.textfield.TextInputLayout tilClass = findViewById(R.id.til_reg_class);
        com.google.android.material.textfield.TextInputLayout tilYear = findViewById(R.id.til_reg_year);
        android.widget.AutoCompleteTextView actvClass = findViewById(R.id.actv_reg_class);
        android.widget.AutoCompleteTextView actvYear = findViewById(R.id.actv_reg_year);

        android.widget.TextView tvTitle = findViewById(R.id.tv_register_title);
        android.widget.TextView tvDesc = findViewById(R.id.tv_register_desc);

        final String targetRole = getIntent().getStringExtra("TARGET_ROLE");
        final String role;
        if (targetRole != null && !targetRole.isEmpty()) {
            role = targetRole.toLowerCase();
            btnBack.setVisibility(android.view.View.GONE);
            tilClass.setVisibility(android.view.View.GONE);
            tilYear.setVisibility(android.view.View.GONE);
            if (role.equals("guru")) {
                tvTitle.setText("Tambah Guru 🧑‍🏫");
                tvDesc.setText("Daftarkan akun baru untuk guru pengajar.");
                tilId.setHint("Nama Pengenal / Inisial Guru");
                btnRegister.setText("TAMBAH GURU");
            } else if (role.equals("operator")) {
                tvTitle.setText("Tambah Operator 🛠️");
                tvDesc.setText("Daftarkan akun baru untuk admin/operator sistem.");
                tilId.setHint("Username Operator");
                btnRegister.setText("TAMBAH OPERATOR");
            }
        } else {
            role = "murid";
            tilId.setHint("NIS (Nomor Induk Siswa)");

            // Fetch dynamic classes from API
            ApiClient.getService().getClasses(null).enqueue(new Callback<java.util.List<com.example.presensi_qr.models.SchoolClass>>() {
                @Override
                public void onResponse(Call<java.util.List<com.example.presensi_qr.models.SchoolClass>> call, Response<java.util.List<com.example.presensi_qr.models.SchoolClass>> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        java.util.List<com.example.presensi_qr.models.SchoolClass> list = response.body();
                        android.widget.ArrayAdapter<com.example.presensi_qr.models.SchoolClass> classAdapter = new android.widget.ArrayAdapter<>(
                            RegisterActivity.this, android.R.layout.simple_dropdown_item_1line, list
                        );
                        actvClass.setAdapter(classAdapter);
                    }
                }

                @Override
                public void onFailure(Call<java.util.List<com.example.presensi_qr.models.SchoolClass>> call, Throwable t) {
                    Log.e("API_ERROR", "Failed to fetch classes: " + t.getMessage());
                }
            });

            // Fetch dynamic years from API
            ApiClient.getService().getYears(null).enqueue(new Callback<java.util.List<com.example.presensi_qr.models.SchoolYear>>() {
                @Override
                public void onResponse(Call<java.util.List<com.example.presensi_qr.models.SchoolYear>> call, Response<java.util.List<com.example.presensi_qr.models.SchoolYear>> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        java.util.List<com.example.presensi_qr.models.SchoolYear> list = response.body();
                        android.widget.ArrayAdapter<com.example.presensi_qr.models.SchoolYear> yearAdapter = new android.widget.ArrayAdapter<>(
                            RegisterActivity.this, android.R.layout.simple_dropdown_item_1line, list
                        );
                        actvYear.setAdapter(yearAdapter);
                    }
                }

                @Override
                public void onFailure(Call<java.util.List<com.example.presensi_qr.models.SchoolYear>> call, Throwable t) {
                    Log.e("API_ERROR", "Failed to fetch years: " + t.getMessage());
                }
            });
        }

        btnRegister.setOnClickListener(v -> {
            String fullName = etFullName.getText().toString().trim();
            String id = etId.getText().toString().trim(); 
            String password = etPassword.getText().toString().trim();
            String studentClass = actvClass.getText().toString().trim();
            String admissionYear = actvYear.getText().toString().trim();

            if (fullName.isEmpty() || id.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Harap isi semua bidang!", Toast.LENGTH_SHORT).show();
                return;
            }

            if (role.equals("murid")) {
                if (studentClass.isEmpty()) {
                    Toast.makeText(this, "Pilih kelas terlebih dahulu!", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (admissionYear.isEmpty()) {
                    Toast.makeText(this, "Pilih tahun masuk terlebih dahulu!", Toast.LENGTH_SHORT).show();
                    return;
                }
                // Gabungkan kelas dan tahun masuk menjadi satu kesatuan: "X-1 (2026)"
                studentClass = studentClass + " (" + admissionYear + ")";
            } else {
                studentClass = "";
                admissionYear = "";
            }

            ApiClient.getService().register(fullName, id, password, role, studentClass, admissionYear).enqueue(new Callback<ResponseBody>() {
                @Override
                public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                    if (response.isSuccessful()) {
                        if (targetRole != null && !targetRole.isEmpty()) {
                            Toast.makeText(RegisterActivity.this, "Akun " + role + " berhasil ditambahkan!", Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(RegisterActivity.this, "Registrasi Berhasil!", Toast.LENGTH_LONG).show();
                        }
                        finish();
                    } else {
                        // MENGAMBIL PESAN ERROR ASLI DARI LARAVEL
                        String errorDetail = "";
                        try {
                            if (response.errorBody() != null) {
                                errorDetail = response.errorBody().string();
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        
                        Log.e("API_ERROR", "Code: " + response.code() + " | Detail: " + errorDetail);
                        Toast.makeText(RegisterActivity.this, "Gagal (Error " + response.code() + ")", Toast.LENGTH_LONG).show();
                    }
                }

                @Override
                public void onFailure(Call<ResponseBody> call, Throwable t) {
                    Log.e("API_ERROR", "Failure: " + t.getMessage());
                    Toast.makeText(RegisterActivity.this, "Koneksi Gagal/Timeout", Toast.LENGTH_SHORT).show();
                }
            });
        });

        btnBack.setOnClickListener(v -> finish());
    }
}
