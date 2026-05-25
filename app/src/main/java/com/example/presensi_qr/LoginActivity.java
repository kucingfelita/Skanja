package com.example.presensi_qr;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.presensi_qr.api.ApiClient;
import com.example.presensi_qr.models.LoginResponse;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import java.io.IOException;

public class LoginActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        TextInputEditText etUsername = findViewById(R.id.et_username);
        TextInputEditText etPassword = findViewById(R.id.et_password);
        MaterialButton btnLogin = findViewById(R.id.btn_login);
        MaterialButton btnGoToRegister = findViewById(R.id.btn_go_to_register);

        btnLogin.setOnClickListener(v -> {
            String username = etUsername.getText().toString().trim();
            String password = etPassword.getText().toString().trim();
            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Isi username dan password", Toast.LENGTH_SHORT).show();
                return;
            }

            // Khusus Guru dan Operator (username bukan angka murni/NIS), tidak menggunakan binding device ID
            String deviceId;
            if (username.matches("\\d+")) {
                deviceId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
            } else {
                deviceId = "none";
            }

            ApiClient.getService().login(username, password, deviceId).enqueue(new Callback<LoginResponse>() {
                @Override
                public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        LoginResponse loginData = response.body();
                        
                        SharedPreferences pref = getSharedPreferences("UserPrefs", MODE_PRIVATE);
                        SharedPreferences.Editor editor = pref.edit();
                        editor.putString("TOKEN", "Bearer " + loginData.getToken());
                        editor.putString("ROLE", loginData.getUser().getRole());
                        editor.putString("NAME", loginData.getUser().getName());
                        editor.putString("NIS_NAMA", loginData.getUser().getNisNama());
                        editor.putString("KELAS", loginData.getUser().getKelas());
                        editor.putString("TAHUN_MASUK", loginData.getUser().getTahunMasuk());
                        editor.apply();

                        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                        intent.putExtra("ROLE", loginData.getUser().getRole());
                        intent.putExtra("NAME", loginData.getUser().getName());
                        startActivity(intent);
                        finish();
                    } else {
                        String errorMessage = "Login Gagal (Error " + response.code() + ")";
                        try {
                            if (response.errorBody() != null) {
                                String errorJson = response.errorBody().string();
                                Log.e("API_ERROR", "Error JSON: " + errorJson);
                                org.json.JSONObject obj = new org.json.JSONObject(errorJson);
                                if (obj.has("error")) {
                                    errorMessage = obj.getString("error");
                                } else if (obj.has("message")) {
                                    errorMessage = obj.getString("message");
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        
                        Log.e("API_ERROR", "HTTP Code: " + response.code());
                        Toast.makeText(LoginActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                    }
                }

                @Override
                public void onFailure(Call<LoginResponse> call, Throwable t) {
                    Log.e("API_ERROR", "Failure: " + t.getMessage());
                    Toast.makeText(LoginActivity.this, "Koneksi Gagal: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        });

        btnGoToRegister.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
        });
    }
}
