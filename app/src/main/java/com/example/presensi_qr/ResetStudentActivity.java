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

public class ResetStudentActivity extends AppCompatActivity {

    private TextInputEditText etNis, etNewPassword;
    private MaterialButton btnResetDevice, btnResetPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reset_student);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        etNis = findViewById(R.id.et_nis);
        etNewPassword = findViewById(R.id.et_new_password);
        btnResetDevice = findViewById(R.id.btn_reset_device);
        btnResetPassword = findViewById(R.id.btn_reset_password);

        btnResetDevice.setOnClickListener(v -> resetDevice());
        btnResetPassword.setOnClickListener(v -> resetPassword());
    }

    private void resetDevice() {
        String nis = etNis.getText().toString().trim();

        if (nis.isEmpty()) {
            Toast.makeText(this, "Masukkan NIS siswa terlebih dahulu", Toast.LENGTH_SHORT).show();
            return;
        }

        SharedPreferences pref = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        String token = pref.getString("TOKEN", "");

        if (token.isEmpty()) {
            Toast.makeText(this, "Sesi berakhir, silakan login ulang", Toast.LENGTH_SHORT).show();
            return;
        }

        btnResetDevice.setEnabled(false);
        btnResetDevice.setText("Memproses...");

        ApiClient.getService().resetDevice(token, nis).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (isFinishing() || isDestroyed()) return;
                
                btnResetDevice.setEnabled(true);
                btnResetDevice.setText("RESET DEVICE ID");

                if (response.isSuccessful()) {
                    Toast.makeText(ResetStudentActivity.this, "Berhasil! Device ID siswa " + nis + " disetel ulang ke 'none'.", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(ResetStudentActivity.this, "Gagal (Error " + response.code() + "). Cek apakah NIS terdaftar.", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                if (isFinishing() || isDestroyed()) return;
                
                btnResetDevice.setEnabled(true);
                btnResetDevice.setText("RESET DEVICE ID");
                Toast.makeText(ResetStudentActivity.this, "Koneksi bermasalah", Toast.LENGTH_SHORT).show();
                Log.e("API_ERROR", t.getMessage());
            }
        });
    }

    private void resetPassword() {
        String nis = etNis.getText().toString().trim();
        String newPassword = etNewPassword.getText().toString().trim();

        if (nis.isEmpty()) {
            Toast.makeText(this, "Masukkan NIS siswa terlebih dahulu", Toast.LENGTH_SHORT).show();
            return;
        }

        if (newPassword.isEmpty()) {
            Toast.makeText(this, "Masukkan password baru terlebih dahulu", Toast.LENGTH_SHORT).show();
            return;
        }

        if (newPassword.length() < 6) {
            Toast.makeText(this, "Password minimal terdiri dari 6 karakter", Toast.LENGTH_SHORT).show();
            return;
        }

        SharedPreferences pref = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        String token = pref.getString("TOKEN", "");

        if (token.isEmpty()) {
            Toast.makeText(this, "Sesi berakhir, silakan login ulang", Toast.LENGTH_SHORT).show();
            return;
        }

        btnResetPassword.setEnabled(false);
        btnResetPassword.setText("Memproses...");

        ApiClient.getService().resetPassword(token, nis, newPassword).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (isFinishing() || isDestroyed()) return;

                btnResetPassword.setEnabled(true);
                btnResetPassword.setText("RESET PASSWORD");

                if (response.isSuccessful()) {
                    Toast.makeText(ResetStudentActivity.this, "Berhasil! Password siswa " + nis + " berhasil diperbarui.", Toast.LENGTH_LONG).show();
                    etNewPassword.setText(""); // Kosongkan field password setelah sukses
                } else {
                    Toast.makeText(ResetStudentActivity.this, "Gagal (Error " + response.code() + "). Cek apakah NIS terdaftar.", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                if (isFinishing() || isDestroyed()) return;

                btnResetPassword.setEnabled(true);
                btnResetPassword.setText("RESET PASSWORD");
                Toast.makeText(ResetStudentActivity.this, "Koneksi bermasalah", Toast.LENGTH_SHORT).show();
                Log.e("API_ERROR", t.getMessage());
            }
        });
    }
}
