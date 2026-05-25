package com.example.presensi_qr;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.presensi_qr.api.ApiClient;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AddSubjectActivity extends AppCompatActivity {

    private TextInputEditText etSubjectName;
    private MaterialButton btnSave, btnManage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_subject);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        etSubjectName = findViewById(R.id.et_subject_name);
        btnSave = findViewById(R.id.btn_save);
        btnManage = findViewById(R.id.btn_manage);

        btnSave.setOnClickListener(v -> saveSubject());
        btnManage.setOnClickListener(v -> {
            android.content.Intent intent = new android.content.Intent(AddSubjectActivity.this, ManageSubjectsActivity.class);
            startActivity(intent);
        });
    }

    private void saveSubject() {
        String subjectName = etSubjectName.getText().toString().trim();

        if (subjectName.isEmpty()) {
            Toast.makeText(this, "Nama mata pelajaran tidak boleh kosong", Toast.LENGTH_SHORT).show();
            return;
        }

        SharedPreferences pref = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        String token = pref.getString("TOKEN", "");

        if (token.isEmpty()) {
            Toast.makeText(this, "Sesi berakhir, silakan login ulang", Toast.LENGTH_SHORT).show();
            return;
        }

        btnSave.setEnabled(false);
        btnSave.setText("Memeriksa...");

        // Cek dulu apakah nama mata pelajaran sudah ada
        ApiClient.getService().getSubjects(token).enqueue(new Callback<java.util.List<com.example.presensi_qr.models.Subject>>() {
            @Override
            public void onResponse(Call<java.util.List<com.example.presensi_qr.models.Subject>> call, Response<java.util.List<com.example.presensi_qr.models.Subject>> response) {
                if (isFinishing() || isDestroyed()) return;

                boolean isDuplicate = false;
                if (response.isSuccessful() && response.body() != null) {
                    for (com.example.presensi_qr.models.Subject s : response.body()) {
                        if (s.getName().equalsIgnoreCase(subjectName)) {
                            isDuplicate = true;
                            break;
                        }
                    }
                }

                if (isDuplicate) {
                    btnSave.setEnabled(true);
                    btnSave.setText("SIMPAN");
                    Toast.makeText(AddSubjectActivity.this, "Mata Pelajaran ini sudah ada!", Toast.LENGTH_LONG).show();
                } else {
                    // Jika belum ada, baru lakukan proses penyimpanan (POST)
                    btnSave.setText("Menyimpan...");
                    ApiClient.getService().addSubject(token, subjectName).enqueue(new Callback<ResponseBody>() {
                        @Override
                        public void onResponse(Call<ResponseBody> call, Response<ResponseBody> responseAdd) {
                            if (isFinishing() || isDestroyed()) return;
                            
                            btnSave.setEnabled(true);
                            btnSave.setText("SIMPAN");
                            
                            if (responseAdd.isSuccessful()) {
                                Toast.makeText(AddSubjectActivity.this, "Mata Pelajaran berhasil ditambahkan", Toast.LENGTH_SHORT).show();
                                finish();
                            } else {
                                Toast.makeText(AddSubjectActivity.this, "Gagal menyimpan (Error " + responseAdd.code() + ")", Toast.LENGTH_LONG).show();
                                Log.e("API_ERROR", "Response code: " + responseAdd.code());
                            }
                        }

                        @Override
                        public void onFailure(Call<ResponseBody> call, Throwable t) {
                            if (isFinishing() || isDestroyed()) return;
                            btnSave.setEnabled(true);
                            btnSave.setText("SIMPAN");
                            Toast.makeText(AddSubjectActivity.this, "Koneksi bermasalah", Toast.LENGTH_SHORT).show();
                            Log.e("API_ERROR", "Failure: " + t.getMessage());
                        }
                    });
                }
            }

            @Override
            public void onFailure(Call<java.util.List<com.example.presensi_qr.models.Subject>> call, Throwable t) {
                if (isFinishing() || isDestroyed()) return;
                btnSave.setEnabled(true);
                btnSave.setText("SIMPAN");
                Toast.makeText(AddSubjectActivity.this, "Gagal memeriksa data", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
